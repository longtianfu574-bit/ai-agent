package com.aiagent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);
    private final HttpClient httpClient;
    private final LlmConfig config;
    private final ObjectMapper objectMapper;

    public LlmClient(LlmConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }

    public CompletableFuture<String> complete(String prompt, String context) {
        return CompletableFuture.supplyAsync(() -> {
            String userMessage = context != null && !context.isEmpty()
                ? "Context:\n" + context + "\n\nQuestion: " + prompt
                : prompt;

            log.info("Calling LLM with context length: {}, prompt length: {}",
                context != null ? context.length() : 0, prompt.length());

            try {
                Map<String, Object> requestBody = Map.of(
                    "model", config.getModel(),
                    "messages", List.of(
                        Map.of("role", "user", "content", userMessage)
                    )
                );

                String jsonBody = objectMapper.writeValueAsString(requestBody);

                log.info("LLM Request URL: {}{}", config.getBaseUrl(), "/chat/completions");
                log.info("LLM Request Body: {}", jsonBody);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl() + "/chat/completions"))
                    .timeout(Duration.ofSeconds(600))  // 10 minutes for LLM responses
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

                HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

                log.info("LLM Response Status: {}", response.statusCode());
                log.info("LLM Response Body: {}", response.body());

                if (response.statusCode() != 200) {
                    log.error("LLM returned error status: {}, body: {}", response.statusCode(), response.body());
                    return "抱歉，LLM 服务返回错误：" + response.statusCode();
                }

                var json = objectMapper.readTree(response.body());

                // Validate response structure
                if (!json.has("choices") || json.get("choices").isEmpty()) {
                    log.error("LLM response missing 'choices' field: {}", response.body());
                    return "抱歉，LLM 响应格式异常。";
                }

                var choice = json.get("choices").get(0);
                if (!choice.has("message") || !choice.get("message").has("content")) {
                    log.error("LLM response missing 'message.content' field: {}", response.body());
                    return "抱歉，LLM 响应格式异常。";
                }

                return choice.get("message").get("content").asText();

            } catch (Exception e) {
                log.error("Failed to call LLM - exception type: {}, message: {}",
                    e.getClass().getSimpleName(), e.getMessage(), e);
                return "抱歉，生成响应时出现错误：" + e.getMessage();
            }
        });
    }

    /**
     * Complete with function calling support
     */
    public CompletableFuture<FunctionCallResult> completeWithFunctions(
            String prompt,
            String context,
            List<Map<String, Object>> functions) {

        return CompletableFuture.supplyAsync(() -> {
            String userMessage = context != null && !context.isEmpty()
                ? "Context:\n" + context + "\n\nQuestion: " + prompt
                : prompt;

            log.info("Calling LLM with function calling support, {} functions registered", functions.size());

            try {
                Map<String, Object> requestBody = Map.of(
                    "model", config.getModel(),
                    "messages", List.of(
                        Map.of("role", "user", "content", userMessage)
                    ),
                    "tools", functions.stream()
                        .map(fn -> Map.of("type", "function", "function", fn))
                        .toList(),
                    "tool_choice", "auto"
                );

                String jsonBody = objectMapper.writeValueAsString(requestBody);

                log.info("LLM Function Call Request: {}", jsonBody);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl() + "/chat/completions"))
                    .timeout(Duration.ofSeconds(600))
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

                HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

                log.info("LLM Response Status: {}", response.statusCode());
                log.info("LLM Response Body: {}", response.body());

                if (response.statusCode() != 200) {
                    log.error("LLM returned error status: {}", response.statusCode());
                    return new FunctionCallResult(false, response.body(), null, null);
                }

                var json = objectMapper.readTree(response.body());
                var choices = json.get("choices");

                if (choices == null || choices.isEmpty()) {
                    return new FunctionCallResult(false, "No choices in response", null, null);
                }

                var choice = choices.get(0);
                var message = choice.get("message");

                // Check if LLM wants to call a function
                if (message.has("tool_calls") && !message.get("tool_calls").isEmpty()) {
                    var toolCalls = message.get("tool_calls");
                    log.info("LLM requested tool call: {}", toolCalls);

                    var toolCall = toolCalls.get(0);
                    var function = toolCall.get("function");
                    String functionName = function.get("name").asText();
                    JsonNode arguments = function.get("arguments");

                    return new FunctionCallResult(true, null, functionName, arguments);
                }

                // No function call, return regular response
                String content = message.get("content").asText();
                return new FunctionCallResult(true, content, null, null);

            } catch (Exception e) {
                log.error("Failed to call LLM with functions", e);
                return new FunctionCallResult(false, e.getMessage(), null, null);
            }
        });
    }

    public record FunctionCallResult(
        boolean success,
        String content,
        String functionName,
        JsonNode arguments
    ) {}
}
