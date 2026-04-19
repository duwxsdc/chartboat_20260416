package com.ai.assistant.config;

import com.ai.assistant.memory.TieredChatMemory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatMemory chatMemory(TieredChatMemory tieredChatMemory) {
        return tieredChatMemory;
    }
}
