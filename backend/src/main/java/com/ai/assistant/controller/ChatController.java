package com.ai.assistant.controller;

import com.ai.assistant.model.ChatRequest;
import com.ai.assistant.model.ChatResponse;
import com.ai.assistant.model.Conversation;
import com.ai.assistant.model.Message;
import com.ai.assistant.service.AuthService;
import com.ai.assistant.service.ChatService;
import com.ai.assistant.service.ConversationService;
import com.ai.assistant.tool.SkillRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ChatController {

    private final ChatService chatService;
    private final SkillRegistry skillRegistry;
    private final AuthService authService;
    private final ConversationService conversationService;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatResponse>> streamChat(@RequestBody ChatRequest request) {
        var user = authService.getCurrentUser();
        if (user == null) {
            return Flux.error(new RuntimeException("User not authenticated"));
        }

        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = chatService.getConversationId();
        }

        log.info("Starting stream chat - User: {}, Conversation: {}, Message: {}, Tools: {}, Skill: {}",
                user.getUsername(), conversationId, request.getMessage(), request.isUseTools(), request.getFormat());

        final String finalConversationId = conversationId;
        final Long userId = user.getId();

        return chatService.chatStream(
                        userId,
                        finalConversationId,
                        request.getMessage())
                .map(chunk -> {
                    ChatResponse response = ChatResponse.builder()
                            .conversationId(finalConversationId)
                            .content(chunk)
                            .role("assistant")
                            .timestamp(Instant.now().toEpochMilli())
                            .isDone(false)
                            .build();
                    return ServerSentEvent.<ChatResponse>builder()
                            .id(finalConversationId)
                            .event("message")
                            .data(response)
                            .build();
                })
                .concatWith(Flux.defer(() -> {
                    ChatResponse doneResponse = ChatResponse.builder()
                            .conversationId(finalConversationId)
                            .content("")
                            .role("assistant")
                            .timestamp(Instant.now().toEpochMilli())
                            .isDone(true)
                            .build();
                    return Flux.just(ServerSentEvent.<ChatResponse>builder()
                            .id(finalConversationId)
                            .event("done")
                            .data(doneResponse)
                            .build());
                }))
                .onErrorResume(error -> {
                    log.error("Stream error: {}", error.getMessage(), error);
                    ChatResponse errorResponse = ChatResponse.builder()
                            .conversationId(finalConversationId)
                            .content("Error: " + error.getMessage())
                            .role("system")
                            .timestamp(Instant.now().toEpochMilli())
                            .isDone(true)
                            .build();
                    return Flux.just(ServerSentEvent.<ChatResponse>builder()
                            .event("error")
                            .data(errorResponse)
                            .build());
                });
    }

    @PostMapping(value = "/block")
    public ChatResponse blockChat(@RequestBody ChatRequest request) {
        var user = authService.getCurrentUser();
        if (user == null) {
            throw new RuntimeException("User not authenticated");
        }

        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = chatService.getConversationId();
        }

        log.info("Starting block chat - User: {}, Conversation: {}, Message: {}", user.getUsername(), conversationId, request.getMessage());

        String response = chatService.chatBlock(
                user.getId(),
                conversationId,
                request.getMessage(),
                request.isUseTools(),
                null,
                request.getFormat());

        return ChatResponse.builder()
                .conversationId(conversationId)
                .content(response)
                .role("assistant")
                .timestamp(Instant.now().toEpochMilli())
                .isDone(true)
                .build();
    }

    @DeleteMapping("/conversation/{conversationId}")
    public java.util.Map<String, String> clearConversation(@PathVariable String conversationId) {
        var user = authService.getCurrentUser();
        if (user == null) {
            throw new RuntimeException("User not authenticated");
        }

        chatService.clearConversation(user.getId(), conversationId);
        return java.util.Map.of("status", "success", "message", "Conversation cleared");
    }

    @GetMapping("/skills")
    public java.util.Map<String, Object> getSkills() {
        var skills = skillRegistry.getAllSkills();
        var skillList = skills.values().stream()
                .map(skill -> java.util.Map.of(
                        "id", skill.id(),
                        "name", skill.name(),
                        "description", skill.systemPrompt().substring(0, Math.min(100, skill.systemPrompt().length())) + "...",
                        "category", skill.category(),
                        "examplePrompt", skill.examplePrompt()
                ))
                .collect(Collectors.toList());
        
        return java.util.Map.of(
                "status", "success",
                "skills", skillList
        );
    }

    @GetMapping("/tools")
    public java.util.Map<String, Object> getTools() {
        var tools = chatService.getAvailableTools();
        var toolList = tools.stream()
                .map(tool -> java.util.Map.of(
                        "name", tool.name(),
                        "description", tool.description()
                ))
                .collect(Collectors.toList());
        
        return java.util.Map.of(
                "status", "success",
                "tools", toolList
        );
    }

    @GetMapping("/health")
    public java.util.Map<String, String> health() {
        return java.util.Map.of("status", "ok", "timestamp", Instant.now().toString());
    }

    @GetMapping("/conversations")
    public java.util.Map<String, Object> getConversations() {
        var user = authService.getCurrentUser();
        if (user == null) {
            throw new RuntimeException("User not authenticated");
        }

        List<Conversation> conversations = conversationService.getConversationsByUserId(user.getId());
        var conversationList = conversations.stream()
                .map(conv -> java.util.Map.of(
                        "id", conv.getConversationId(),
                        "title", conv.getTitle(),
                        "createdAt", conv.getCreatedAt(),
                        "updatedAt", conv.getUpdatedAt()
                ))
                .collect(Collectors.toList());

        return java.util.Map.of(
                "status", "success",
                "conversations", conversationList
        );
    }

    @GetMapping("/conversation/{conversationId}")
    public java.util.Map<String, Object> getConversation(@PathVariable String conversationId) {
        var user = authService.getCurrentUser();
        if (user == null) {
            throw new RuntimeException("User not authenticated");
        }

        var conversation = conversationService.getConversationByUserIdAndConversationId(user.getId(), conversationId);
        if (conversation.isEmpty()) {
            throw new RuntimeException("Conversation not found");
        }

        var conv = conversation.get();
        var messages = conversationService.getMessagesByConversationId(conv.getId());
        var messageList = messages.stream()
                .map(msg -> java.util.Map.of(
                        "id", msg.getId(),
                        "role", msg.getRole(),
                        "content", msg.getContent(),
                        "createdAt", msg.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return java.util.Map.of(
                "status", "success",
                "conversation", java.util.Map.of(
                        "id", conv.getConversationId(),
                        "title", conv.getTitle(),
                        "createdAt", conv.getCreatedAt(),
                        "updatedAt", conv.getUpdatedAt(),
                        "messages", messageList
                )
        );
    }

    @PutMapping("/conversation/{conversationId}/title")
    public java.util.Map<String, Object> updateConversationTitle(@PathVariable String conversationId, @RequestBody java.util.Map<String, String> request) {
        var user = authService.getCurrentUser();
        if (user == null) {
            throw new RuntimeException("User not authenticated");
        }

        String title = request.get("title");
        if (title == null || title.isBlank()) {
            throw new RuntimeException("Title is required");
        }

        var updatedConversation = conversationService.updateConversationTitle(user.getId(), conversationId, title);
        if (updatedConversation == null) {
            throw new RuntimeException("Conversation not found");
        }

        return java.util.Map.of(
                "status", "success",
                "conversation", java.util.Map.of(
                        "id", updatedConversation.getConversationId(),
                        "title", updatedConversation.getTitle()
                )
        );
    }
}
