package com.aiagent.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);
    private final WebClient webClient;
    private final LlmConfig config;
    private final ObjectMapper objectMapper;

    public LlmClient(LlmConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
            .baseUrl(config.getBaseUrl())
            .defaultHeader("Authorization", "Bearer " + config.getApiKey())
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    public CompletableFuture<String> complete(String prompt, String context) {
        String userMessage = context != null && !context.isEmpty()
            ? "Context:\n" + context + "\n\nQuestion: " + prompt
            : prompt;

        Map<String, Object> requestBody = Map.of(
            "model", config.getModel(),
            "messages", List.of(
                Map.of("role", "user", "content", userMessage)
            )
        );

        return webClient.post()
            .uri("/chat/completions")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> {
                try {
                    var json = objectMapper.readTree(response);
                    return json.get("choices").get(0)
                        .get("message").get("content").asText();
                } catch (Exception e) {
                    log.error("Failed to parse LLM response", e);
                    return "抱歉，生成响应时出现错误。";
                }
            })
            .toFuture();
    }
}
