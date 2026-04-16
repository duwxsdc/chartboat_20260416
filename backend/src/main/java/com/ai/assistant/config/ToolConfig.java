package com.ai.assistant.config;

import com.ai.assistant.tool.AssistantTools;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolConfig {

    @Bean
    public ToolCallback[] toolCallbacks(AssistantTools assistantTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(assistantTools)
                .build()
                .getToolCallbacks();
    }
}
