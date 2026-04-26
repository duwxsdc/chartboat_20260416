package com.ai.assistant.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 能力边界日志记录服务
 * 专门记录系统无法处理的请求，用于后续分析和能力提升
 */
@Slf4j
@Service
public class CapabilityBoundaryLogger {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 记录超出能力范围的请求
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param userMessage 用户原始问题
     * @param intent 识别的意图（可为空）
     * @param slots 提取的槽位信息（可为空）
     * @param skillName 技能名称
     */
    public void logUnmetCapability(String userId, String sessionId, String userMessage,
                                   String intent, String slots, String skillName) {
        String timestamp = LocalDateTime.now().format(FORMATTER);

        // 详细日志记录 - 人类可读格式
        log.warn("========================================");
        log.warn("🚨 能力边界外请求检测");
        log.warn("========================================");
        log.warn("⏰ 时间: {}", timestamp);
        log.warn("👤 用户ID: {}", userId);
        log.warn("💬 会话ID: {}", sessionId);
        log.warn("🔧 技能: {}", skillName);
        log.warn("❓ 用户问题: {}", userMessage);
        log.warn("🎯 识别意图: {}", intent != null && !intent.isEmpty() ? intent : "未识别");
        log.warn("📦 槽位信息: {}", slots != null && !slots.isEmpty() ? slots : "无");
        log.warn("📝 响应话术: 抱歉，该功能暂未开放，已为您记录需求或者联系杜文旭。");
        log.warn("📊 JSON标记: {{\"unmet\":\"true\",\"intent\":\"{}\",\"slots\":\"{}\"}}",
                intent != null ? intent : "", slots != null ? slots : "");
        log.warn("========================================");

        // 结构化日志 - 便于程序化分析
        log.info("CAPABILITY_GAP|{}|{}|{}|{}|{}|{}|{}",
                timestamp,
                userId,
                sessionId,
                skillName,
                sanitizeForLog(userMessage),
                sanitizeForLog(intent),
                sanitizeForLog(slots));
    }

    /**
     * 清理日志内容，防止注入攻击
     */
    private String sanitizeForLog(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return input.replaceAll("[\r\n|]", " ")
                .replaceAll("\\s+", " ")
                .substring(0, Math.min(input.length(), 200));
    }
}