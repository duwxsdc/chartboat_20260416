package com.ai.assistant.service;

import com.ai.assistant.model.Conversation;
import com.ai.assistant.service.ConversationService;
// import com.ai.assistant.service.RagService;
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
    private final ConversationService conversationService;
    // private final RagService ragService;

    @Autowired(required = false)
    private ToolCallback[] toolCallbacks;

    private static final String SYSTEM_PROMPT = "你是一个helpful的中文AI助手。请用中文回复用户的问题。" +
            "当需要展示数学计算时，请使用简单易读的格式，不要使用LaTeX或数学公式符号（如\\times、\\frac等）。" +
            "对于算术计算，请直接使用标准运算符（*、/、+、-）和清晰的步骤说明，让所有人都能轻松理解。";

    public Flux<String> chatStream(Long userId, String conversationId, String message, boolean useTools, String skillId, String format) {
        String convId = conversationId != null ? conversationId : UUID.randomUUID().toString();
        String userConversationId = userId + ":" + convId;

        // 保存用户消息
        saveUserMessage(userId, convId, message);

        ChatClient.Builder builder = ChatClient.builder(chatModel);

        String systemPrompt = SYSTEM_PROMPT;

        if (skillId != null && skillRegistry.hasSkill(skillId)) {
            SkillRegistry.Skill skill = skillRegistry.getSkill(skillId);
            systemPrompt = skill.systemPrompt();
        }

        if (format != null && !format.isBlank()) {
            systemPrompt += " " + buildFormatPrompt(format);
        }

        // 集成 RAG 功能
        // String ragContext = ragService.getRelevantContext(message, 3);
        // if (!ragContext.isEmpty()) {
        //     systemPrompt += "\n\n基于以下相关信息回答问题：\n" + ragContext;
        // }

        builder.defaultSystem(systemPrompt);

        ChatClient chatClient = builder.build();

        StringBuilder fullResponse = new StringBuilder();

        return chatClient.prompt()
                .user(message)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId(userConversationId).build())
                .stream()
                .content()
                .doOnNext(content -> {
                    log.debug("Stream chunk: {}", content);
                    fullResponse.append(content);
                })
                .doOnComplete(() -> {
                    log.info("Stream completed for user: {}, conversation: {}", userId, convId);
                    // 保存助手回复
                    saveAssistantMessage(userId, convId, fullResponse.toString());
                })
                .doOnError(error -> {
                    log.error("Stream error: {}", error.getMessage());
                    // 保存错误消息
                    saveAssistantMessage(userId, convId, "Error: " + error.getMessage());
                });
    }

    private void saveUserMessage(Long userId, String conversationId, String message) {
        try {
            // 检查会话是否存在，不存在则创建
            var existingConversation = conversationService.getConversationByUserIdAndConversationId(userId, conversationId);
            if (existingConversation.isEmpty()) {
                String title = message.length() > 50 ? message.substring(0, 50) + "..." : message;
                conversationService.createConversation(userId, conversationId, title);
            }
            // 保存用户消息
            var conversation = conversationService.getConversationByUserIdAndConversationId(userId, conversationId).orElse(null);
            if (conversation != null) {
                conversationService.saveMessage(conversation.getId(), "user", message);
            }
        } catch (Exception e) {
            log.error("Failed to save user message: {}", e.getMessage());
        }
    }

    private void saveAssistantMessage(Long userId, String conversationId, String message) {
        try {
            var conversation = conversationService.getConversationByUserIdAndConversationId(userId, conversationId).orElse(null);
            if (conversation != null) {
                conversationService.saveMessage(conversation.getId(), "assistant", message);
            }
        } catch (Exception e) {
            log.error("Failed to save assistant message: {}", e.getMessage());
        }
    }

    public String chatBlock(Long userId, String conversationId, String message, boolean useTools, String skillId, String format) {
        String convId = conversationId != null ? conversationId : UUID.randomUUID().toString();
        String userConversationId = userId + ":" + convId;

        // 保存用户消息
        saveUserMessage(userId, convId, message);

        ChatClient.Builder builder = ChatClient.builder(chatModel);

        String systemPrompt = SYSTEM_PROMPT;

        if (skillId != null && skillRegistry.hasSkill(skillId)) {
            SkillRegistry.Skill skill = skillRegistry.getSkill(skillId);
            systemPrompt = skill.systemPrompt();
        }

        if (format != null && !format.isBlank()) {
            systemPrompt += " " + buildFormatPrompt(format);
        }

        // 集成 RAG 功能
        // String ragContext = ragService.getRelevantContext(message, 3);
        // if (!ragContext.isEmpty()) {
        //     systemPrompt += "\n\n基于以下相关信息回答问题：\n" + ragContext;
        // }

        ChatClient chatClient = builder.defaultSystem(systemPrompt).build();

        String response = chatClient.prompt()
                .user(message)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId(userConversationId).build())
                .call()
                .content();

        // 保存助手回复
        saveAssistantMessage(userId, convId, response);

        return response;
    }

    public String getConversationId() {
        return UUID.randomUUID().toString();
    }

    public void clearConversation(Long userId, String conversationId) {
        String userConversationId = userId + ":" + conversationId;
        chatMemory.clear(userConversationId);
        log.info("Cleared conversation for user: {}, conversation: {}", userId, conversationId);
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
