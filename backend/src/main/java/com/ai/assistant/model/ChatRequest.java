package com.ai.assistant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String conversationId;
    private String message;
    private boolean useTools;
    private boolean useMcp;
    private String format;
}
