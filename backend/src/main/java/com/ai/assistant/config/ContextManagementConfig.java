package com.ai.assistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "context.management")
public class ContextManagementConfig {

    private int hotTierSize = 50;
    private int warmTierSize = 150;
    private AutoCleanupConfig autoCleanup = new AutoCleanupConfig();
    private CompressionConfig compression = new CompressionConfig();
    private SummaryConfig summary = new SummaryConfig();
    private MonitorConfig monitor = new MonitorConfig();

    @Data
    public static class AutoCleanupConfig {
        private boolean enabled = true;
        private int maxConversationSize = 200;
        private int maxIdleHours = 24;
        private int maxTotalSizeMb = 500;
    }

    @Data
    public static class CompressionConfig {
        private boolean enabled = true;
        private int thresholdBytes = 1024;
    }

    @Data
    public static class SummaryConfig {
        private boolean enabled = true;
        private int maxMessagesBeforeSummary = 100;
        private String summaryModel = "qwen-max";
    }

    @Data
    public static class MonitorConfig {
        private boolean enabled = true;
        private int logIntervalSeconds = 60;
    }
}