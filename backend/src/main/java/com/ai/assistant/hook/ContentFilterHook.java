package com.ai.assistant.hook;

import com.ai.assistant.config.ContentFilterConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 输入内容过滤 Hook
 * 在 Agent 执行前拦截用户输入，进行：
 * 1. 敏感词过滤
 * 2. 闲聊处理
 * 3. 无效输入检测
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentFilterHook extends AgentHook {

    private final ContentFilterConfig config;
    private List<Pattern> chitChatPatternsCompiled;

    @PostConstruct
    public void init() {
        chitChatPatternsCompiled = config.getChitChatPatterns().stream()
                .map(pattern -> Pattern.compile(pattern, Pattern.CASE_INSENSITIVE))
                .collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
        try {
            Optional<Object> messagesOpt = state.value("messages");
            if (messagesOpt.isEmpty()) {
                return CompletableFuture.completedFuture(Map.of());
            }

            List<Message> messages = (List<Message>) messagesOpt.get();
            String userMessage = extractLastUserMessage(messages);

            if (userMessage == null || userMessage.trim().isEmpty()) {
                return CompletableFuture.completedFuture(Map.of());
            }

            String userId = extractUserId(config);

            // 1. 敏感词检测
            if (this.config.isSensitiveWordEnabled()) {
                String sensitiveWordCheck = checkSensitiveWords(userMessage);
                if (sensitiveWordCheck != null) {
                    log.warn("🚫 敏感词拦截: userId={}, message={}", userId, maskMessage(userMessage));
                    return createInterceptResponse(sensitiveWordCheck);
                }
            }

            // 2. 闲聊检测
            if (this.config.isChitChatEnabled() && isChitChat(userMessage)) {
                log.info("💬 闲聊处理: userId={}, message={}", userId, userMessage);
                return createInterceptResponse(handleChitChat(userMessage));
            }

            // 3. 无效输入检测
            if (this.config.isInvalidInputEnabled()) {
                String invalidCheck = checkInvalidInput(userMessage);
                if (invalidCheck != null) {
                    log.info("⚠️ 无效输入: userId={}, message={}", userId, userMessage);
                    return createInterceptResponse(invalidCheck);
                }
            }

            log.debug("✅ 内容检查通过: userId={}", userId);
            return CompletableFuture.completedFuture(Map.of());

        } catch (Exception e) {
            log.error("ContentFilterHook error", e);
            return CompletableFuture.completedFuture(Map.of());
        }
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
        return CompletableFuture.completedFuture(Map.of());
    }

    /**
     * 提取最后一条用户消息
     */
    private String extractLastUserMessage(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof UserMessage) {
                return messages.get(i).getText();
            }
        }
        return null;
    }

    /**
     * 检查敏感词
     */
    private String checkSensitiveWords(String message) {
        String lowerMessage = message.toLowerCase();
        for (String word : config.getSensitiveWords()) {
            if (lowerMessage.contains(word.toLowerCase())) {
                return "您的输入包含不当内容，请文明提问。";
            }
        }
        return null;
    }

    /**
     * 检测是否为闲聊
     */
    private boolean isChitChat(String message) {
        String trimmed = message.trim();
        for (Pattern pattern : chitChatPatternsCompiled) {
            if (pattern.matcher(trimmed).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 处理闲聊回复
     */
    private String handleChitChat(String message) {
        String lower = message.toLowerCase();

        if (lower.contains("你好") || lower.contains("hi") || lower.contains("hello")) {
            return config.getChitChat().getGreeting();
        }
        if (lower.contains("在吗")) {
            return config.getChitChat().getAvailability();
        }
        if (lower.contains("名字") || lower.contains("是谁")) {
            return config.getChitChat().getIdentity();
        }
        if (lower.contains("能做什么") || lower.contains("会什么")) {
            return config.getChitChat().getCapabilities();
        }

        return config.getChitChat().getGreeting();
    }

    /**
     * 检查无效输入
     */
    private String checkInvalidInput(String message) {
        String trimmed = message.trim();

        if (trimmed.length() < config.getMinInputLength()) {
            return "您的输入太短了，请详细描述您的问题。";
        }

        if (trimmed.length() > config.getMaxInputLength()) {
            return "您的输入过长，请精简您的问题。";
        }

        if (trimmed.matches("^\\d+$")) {
            return "请输入完整的问题描述，而不是仅仅数字。";
        }

        if (trimmed.matches("^[^\\u4e00-\\u9fa5a-zA-Z0-9]+$")) {
            return "请输入有效的问题内容。";
        }

        return null;
    }

    /**
     * 创建拦截响应 - 提前结束 Agent 执行
     */
    private CompletableFuture<Map<String, Object>> createInterceptResponse(String message) {
        AssistantMessage response = new AssistantMessage(message);

        Map<String, Object> result = new HashMap<>();
        result.put("messages", List.of(response));
        result.put("__terminate", true);

        return CompletableFuture.completedFuture(result);
    }

    /**
     * 从配置中提取用户ID
     */
    private String extractUserId(RunnableConfig config) {
        try {
            Map<String, Object> metadata = config.metadata().get();
            if ( metadata.containsKey("user_id")) {
                return String.valueOf(metadata.get("user_id"));
            }
        } catch (Exception e) {
            log.debug("Failed to extract user_id", e);
        }
        return "unknown";
    }

    /**
     * 遮蔽消息用于日志
     */
    private String maskMessage(String message) {
        if (message.length() <= 10) {
            return message;
        }
        return message.substring(0, 10) + "...";
    }

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public String getName() {
        return ContentFilterHook.class.getName();
    }
}