package com.ai.assistant.service;

import com.ai.assistant.tool.SkillRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatModel chatModel;
    private final ChatMemory chatMemory;
    private final SkillRegistry skillRegistry;

    @Autowired(required = false)
    private ToolCallback[] toolCallbacks;

    public Flux<String> chatStream(String conversationId, String message, boolean useTools, String skillId, String format) {
        String convId = conversationId != null ? conversationId : UUID.randomUUID().toString();

        ChatClient.Builder builder = ChatClient.builder(chatModel);
        
        String systemPrompt = "You are a helpful AI assistant.";
        
        if (skillId != null && skillRegistry.hasSkill(skillId)) {
            SkillRegistry.Skill skill = skillRegistry.getSkill(skillId);
            systemPrompt = skill.systemPrompt();
        }

        if (format != null && !format.isBlank()) {
            systemPrompt += " " + buildFormatPrompt(format);
        }
        
        builder.defaultSystem(systemPrompt);
        
        ChatClient chatClient = builder.build();

        return chatClient.prompt()
                .user(message)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId(convId).build())
                .stream()
                .content()
                .doOnNext(content -> log.debug("Stream chunk: {}", content))
                .doOnComplete(() -> log.info("Stream completed for conversation: {}", convId))
                .doOnError(error -> log.error("Stream error: {}", error.getMessage()));
    }

    public String chatBlock(String conversationId, String message, boolean useTools, String skillId, String format) {
        String convId = conversationId != null ? conversationId : UUID.randomUUID().toString();

        ChatClient.Builder builder = ChatClient.builder(chatModel);
        
        String systemPrompt = "You are a helpful AI assistant.";
        
        if (skillId != null && skillRegistry.hasSkill(skillId)) {
            SkillRegistry.Skill skill = skillRegistry.getSkill(skillId);
            systemPrompt = skill.systemPrompt();
        }

        ChatClient chatClient = builder.defaultSystem(systemPrompt).build();

        return chatClient.prompt()
                .user(message)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId(convId).build())
                .call()
                .content();
    }

    public String getConversationId() {
        return UUID.randomUUID().toString();
    }

    public void clearConversation(String conversationId) {
        chatMemory.clear(conversationId);
        log.info("Cleared conversation: {}", conversationId);
    }

    private String buildFormatPrompt(String format) {
        return switch (format.toLowerCase()) {
            case "json" -> "Always respond in valid JSON format. Do not include any text outside the JSON object.";
            case "markdown" -> "Format your response using Markdown. Use headings, lists, code blocks, and other Markdown features appropriately.";
            case "html" -> "Format your response using HTML. Use appropriate HTML tags for structure and formatting.";
            case "table" -> "Present information in a well-formatted table structure when applicable.";
            case "bullet" -> "Present your response as a bulleted list with clear, concise points.";
            default -> format;
        };
    }
}
