package com.ai.assistant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String conversationId;
    private String content;
    private String role;
    private long timestamp;
    private boolean isDone;
    private String format;
}
