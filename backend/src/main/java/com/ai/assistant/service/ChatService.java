package com.ai.assistant.service;

import com.ai.assistant.tool.SkillRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ChatService {

    private final ChatModel chatModel;
    private final ChatMemory chatMemory;
    private final SkillRegistry skillRegistry;
    private final ConversationService conversationService;
    private ToolCallback[] toolCallbacks;

    @Autowired
    public ChatService(ChatModel chatModel, ChatMemory chatMemory, 
                      SkillRegistry skillRegistry, ConversationService conversationService) {
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
        this.skillRegistry = skillRegistry;
        this.conversationService = conversationService;
    }

    @Autowired(required = false)
    public void setToolCallbacks(ToolCallback[] toolCallbacks) {
        this.toolCallbacks = toolCallbacks;
    }

    private static final String SYSTEM_PROMPT = "你是一个helpful的中文AI助手。请用中文回复用户的问题。" +
            "当需要展示数学计算时，请使用简单易读的格式，不要使用LaTeX或数学公式符号（如\\times、\\frac等）。" +
            "对于算术计算，请直接使用标准运算符（*、/、+、-）和清晰的步骤说明，让所有人都能轻松理解。";

    public Flux<String> chatStream(Long userId, String conversationId, String message, boolean useTools, String skillId, String format) {
        String convId = conversationId != null ? conversationId : UUID.randomUUID().toString();
        String userConversationId = userId + ":" + convId;

        saveUserMessage(userId, convId, message);

        ChatClient.Builder builder = ChatClient.builder(chatModel);

        String systemPrompt = SYSTEM_PROMPT;

        if (skillId != null && skillRegistry.hasSkill(skillId)) {
            SkillRegistry.Skill skill = skillRegistry.getSkill(skillId);
            systemPrompt = skill.systemPrompt();
            log.info("Using skill: {}", skillId);
        }

        if (format != null && !format.isBlank()) {
            systemPrompt += " " + buildFormatPrompt(format);
        }

        builder.defaultSystem(systemPrompt);

        ChatClient chatClient = builder.build();

        StringBuilder fullResponse = new StringBuilder();

        ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt()
                .user(message)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId(userConversationId).build());

        if (useTools && toolCallbacks != null && toolCallbacks.length > 0) {
            log.info("Enabling {} tools for this request", toolCallbacks.length);
            requestSpec.tools(toolCallbacks);
        }

        return requestSpec.stream()
                .content()
                .doOnNext(content -> {
                    log.debug("Stream chunk: {}", content);
                    fullResponse.append(content);
                })
                .doOnComplete(() -> {
                    log.info("Stream completed for user: {}, conversation: {}", userId, convId);
                    saveAssistantMessage(userId, convId, fullResponse.toString());
                })
                .doOnError(error -> {
                    log.error("Stream error: {}", error.getMessage());
                    saveAssistantMessage(userId, convId, "Error: " + error.getMessage());
                });
    }

    public String chatBlock(Long userId, String conversationId, String message, boolean useTools, String skillId, String format) {
        String convId = conversationId != null ? conversationId : UUID.randomUUID().toString();
        String userConversationId = userId + ":" + convId;

        saveUserMessage(userId, convId, message);

        ChatClient.Builder builder = ChatClient.builder(chatModel);

        String systemPrompt = SYSTEM_PROMPT;

        if (skillId != null && skillRegistry.hasSkill(skillId)) {
            SkillRegistry.Skill skill = skillRegistry.getSkill(skillId);
            systemPrompt = skill.systemPrompt();
            log.info("Using skill: {}", skillId);
        }

        if (format != null && !format.isBlank()) {
            systemPrompt += " " + buildFormatPrompt(format);
        }

        ChatClient chatClient = builder.defaultSystem(systemPrompt).build();

        ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt()
                .user(message)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId(userConversationId).build());

        if (useTools && toolCallbacks != null && toolCallbacks.length > 0) {
            log.info("Enabling {} tools for this request", toolCallbacks.length);
            requestSpec.tools(toolCallbacks);
        }

        String response = requestSpec.call().content();

        saveAssistantMessage(userId, convId, response);

        return response;
    }

    private void saveUserMessage(Long userId, String conversationId, String message) {
        try {
            var existingConversation = conversationService.getConversationByUserIdAndConversationId(userId, conversationId);
            if (existingConversation.isEmpty()) {
                String title = message.length() > 50 ? message.substring(0, 50) + "..." : message;
                conversationService.createConversation(userId, conversationId, title);
            }
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

    public List<ToolInfo> getAvailableTools() {
        if (toolCallbacks == null || toolCallbacks.length == 0) {
            return List.of();
        }
        // 简化返回工具信息，不调用可能不存在的API
        return List.of(new ToolInfo("tools", toolCallbacks.length + " tools available"));
    }

    public record ToolInfo(String name, String description) {}

    public SkillRegistry.Skill getSkill(String skillId) {
        return skillRegistry.getSkill(skillId);
    }

    public Map<String, SkillRegistry.Skill> getAllSkills() {
        return skillRegistry.getAllSkills();
    }
}
