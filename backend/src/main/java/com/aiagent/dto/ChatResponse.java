package com.aiagent.dto;

import lombok.Data;

@Data
public class ChatResponse {
    private String message;
    private String sessionId;
    private String model;
}
