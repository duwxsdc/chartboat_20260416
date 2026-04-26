package com.ai.assistant.hook;

import com.ai.assistant.service.CapabilityBoundaryLogger;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 能力边界检测 Hook
 * 在 Agent 执行完成后自动检测响应是否包含能力边界标记
 * 如果检测到未满足的能力，记录详细日志用于后续分析
 */
@Slf4j
@Component
public class CapabilityBoundaryHook extends AgentHook {

    private final CapabilityBoundaryLogger capabilityBoundaryLogger;

    public CapabilityBoundaryHook(CapabilityBoundaryLogger capabilityBoundaryLogger) {
        this.capabilityBoundaryLogger = capabilityBoundaryLogger;
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
        log.debug("CapabilityBoundaryHook beforeAgent: threadId={}", config.threadId());
        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
        try {
            // 从执行状态中获取最终响应
            Optional<Object> messagesOpt = state.value("messages");
            if (messagesOpt.isEmpty()) {
                log.debug("CapabilityBoundaryHook: no messages in state");
                return CompletableFuture.completedFuture(Map.of());
            }

            List<Message> messages = (List<Message>) messagesOpt.get();
            if (messages == null || messages.isEmpty()) {
                return CompletableFuture.completedFuture(Map.of());
            }

            // 获取最后一条助手消息
            AssistantMessage response = null;
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message msg = messages.get(i);
                if (msg instanceof AssistantMessage) {
                    response = (AssistantMessage) msg;
                    break;
                }
            }

            if (response == null) {
                log.debug("CapabilityBoundaryHook: no assistant message found");
                return CompletableFuture.completedFuture(Map.of());
            }

            String content = response.getText();
            if (content == null || content.isEmpty()) {
                return CompletableFuture.completedFuture(Map.of());
            }

            // 检测是否包含能力边界标记
            if (containsUnmetCapabilityMarker(content)) {
                logUnmetCapability(state, config, content);
            }

        } catch (Exception e) {
            log.error("CapabilityBoundaryHook afterAgent error", e);
        }

        return CompletableFuture.completedFuture(Map.of());
    }

    /**
     * 检测响应是否包含未满足能力标记
     */
    private boolean containsUnmetCapabilityMarker(String content) {
        return content.contains("\"unmet\":\"true\"") ||
                content.contains("\"unmet\": \"true\"") ||
                content.contains("\"unmet\":true");
    }

    /**
     * 记录未满足能力的详细信息
     */
    private void logUnmetCapability(OverAllState state, RunnableConfig config, String content) {
        try {
            // 从配置中获取会话和用户信息
            String sessionId = config.threadId().get();
            String userId = extractUserId(config);
            String userMessage = extractUserMessage(state);

            // 从响应中提取 JSON 信息
            String intent = extractJsonField(content, "intent");
            String slots = extractJsonField(content, "slots");
            String skillName = extractSkillName(content);

            // 调用日志服务记录
            capabilityBoundaryLogger.logUnmetCapability(
                    userId,
                    sessionId,
                    userMessage,
                    intent,
                    slots,
                    skillName
            );

            log.info("Capability boundary detected: session={}, user={}, intent={}",
                    sessionId, userId, intent);

        } catch (Exception e) {
            log.error("Failed to log unmet capability", e);
        }
    }

    /**
     * 从配置中提取用户ID
     */
    private String extractUserId(RunnableConfig config) {
        try {
            Map<String, Object> metadata = config.metadata().get();
            if (metadata.containsKey("user_id")) {
                return String.valueOf(metadata.get("user_id"));
            }
        } catch (Exception e) {
            log.debug("Failed to extract user_id from config", e);
        }
        return "unknown";
    }

    /**
     * 从状态中提取用户消息
     */
    private String extractUserMessage(OverAllState state) {
        try {
            Optional<Object> messagesOpt = state.value("messages");
            if (messagesOpt.isPresent()) {
                List<Message> messages = (List<Message>) messagesOpt.get();
                if (messages != null) {
                    // 获取最后一条用户消息
                    for (int i = messages.size() - 1; i >= 0; i--) {
                        Message msg = messages.get(i);
                        if (msg instanceof org.springframework.ai.chat.messages.UserMessage) {
                            return msg.getText();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract user message from state", e);
        }
        return "unknown";
    }

    /**
     * 从 JSON 字符串中提取字段值
     */
    private String extractJsonField(String content, String fieldName) {
        try {
            String pattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]*)\"";
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(content);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            log.debug("Failed to extract JSON field: {}", fieldName, e);
        }
        return "";
    }

    /**
     * 从响应中提取技能名称
     */
    private String extractSkillName(String content) {
        if (content.contains("ITBA") || content.contains("itba")) {
            return "itba-skill";
        } else if (content.contains("业务") || content.contains("business")) {
            return "business-skill";
        }
        return "unknown-skill";
    }

    @Override
    public String getName() {
        return CapabilityBoundaryHook.class.getName();
    }
}