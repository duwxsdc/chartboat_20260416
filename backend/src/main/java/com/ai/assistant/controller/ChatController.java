package com.ai.assistant.controller;

import com.ai.assistant.model.ChatRequest;
import com.ai.assistant.model.ChatResponse;
import com.ai.assistant.service.ChatService;
import com.ai.assistant.tool.SkillRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ChatController {

    private final ChatService chatService;
    private final SkillRegistry skillRegistry;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatResponse>> streamChat(@RequestBody ChatRequest request) {
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = chatService.getConversationId();
        }

        log.info("Starting stream chat - Conversation: {}, Message: {}, Tools: {}, Skill: {}",
                conversationId, request.getMessage(), request.isUseTools(), request.getFormat());

        final String finalConversationId = conversationId;
        
        return chatService.chatStream(
                        finalConversationId,
                        request.getMessage(),
                        request.isUseTools(),
                        null,
                        request.getFormat())
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
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = chatService.getConversationId();
        }

        log.info("Starting block chat - Conversation: {}, Message: {}", conversationId, request.getMessage());

        String response = chatService.chatBlock(
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
    public Map<String, String> clearConversation(@PathVariable String conversationId) {
        chatService.clearConversation(conversationId);
        return Map.of("status", "success", "message", "Conversation cleared");
    }

    @GetMapping("/skills")
    public Map<String, SkillRegistry.Skill> getSkills() {
        return skillRegistry.getAllSkills();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "timestamp", Instant.now().toString());
    }
}
