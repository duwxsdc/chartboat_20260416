package com.ai.assistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 内容过滤配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "content.filter")
public class ContentFilterConfig {

    /**
     * 是否启用敏感词过滤
     */
    private boolean sensitiveWordEnabled = true;

    /**
     * 是否启用闲聊拦截
     */
    private boolean chitChatEnabled = true;

    /**
     * 是否启用无效输入检测
     */
    private boolean invalidInputEnabled = true;

    /**
     * 敏感词列表
     */
    private List<String> sensitiveWords = new ArrayList<>();

    /**
     * 闲聊模式列表
     */
    private List<String> chitChatPatterns = new ArrayList<>();

    /**
     * 输入最小长度
     */
    private int minInputLength = 2;

    /**
     * 输入最大长度
     */
    private int maxInputLength = 1000;

    /**
     * 闲聊回复模板
     */
    private ChitChatResponses chitChat = new ChitChatResponses();

    @Data
    public static class ChitChatResponses {
        private String greeting = "你好！我是AI助手，很高兴为您服务。请问有什么我可以帮您的吗？";
        private String availability = "我在的！请问有什么可以帮助您的？";
        private String identity = "我是AI智能助手，专门为您提供智能问答服务。请问有什么我可以帮您的？";
        private String capabilities = "我可以为您提供以下服务：\n" +
                "1. 补录平台相关问题解答（ITBA职责、操作流程、权限咨询等）\n" +
                "2. 业务知识查询（补录业务流程、规范等）\n" +
                "3. 常见问题FAQ解答\n\n" +
                "请直接描述您的问题，我会尽力为您解答！";
    }
}
