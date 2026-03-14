package com.aiagent.controller;

import com.aiagent.dto.ChatRequest;
import com.aiagent.dto.ChatResponse;
import com.aiagent.rag.query.QueryService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final QueryService queryService;

    public ChatController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping("/chat")
    public CompletableFuture<ChatResponse> chat(@RequestBody ChatRequest request) {
        String sessionId = request.getSessionId() != null
            ? request.getSessionId()
            : UUID.randomUUID().toString();

        return queryService.ragComplete(request.getMessage(), 5)
            .thenApply(message -> {
                ChatResponse response = new ChatResponse();
                response.setSessionId(sessionId);
                response.setModel("qwen-3.5-rag");
                response.setMessage(message);
                return response;
            });
    }
}
