package com.aiagent.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * MCP Client for communicating with MCP servers
 */
@Component
public class McpClient {

    private static final Logger log = LoggerFactory.getLogger(McpClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiToken;

    public McpClient(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${mcp.api-token:default-token}") String apiToken) {
        this.objectMapper = objectMapper;
        this.apiToken = apiToken;
        this.webClient = webClientBuilder
            .defaultHeader("Authorization", "Bearer " + apiToken)
            .build();
    }

    /**
     * List all tools from an MCP server
     */
    public CompletableFuture<List<McpTool>> listTools(String serverUrl) {
        log.debug("Listing tools from MCP server: {}", serverUrl);
        return webClient.get()
            .uri(serverUrl + "/tools")
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(node -> {
                List<McpTool> tools = new ArrayList<>();
                if (node.isArray()) {
                    for (JsonNode toolNode : node) {
                        McpTool tool = objectMapper.convertValue(toolNode, McpTool.class);
                        tools.add(tool);
                    }
                }
                log.debug("Found {} tools from server {}", tools.size(), serverUrl);
                return tools;
            })
            .toFuture();
    }

    /**
     * Call a tool on an MCP server
     */
    public CompletableFuture<ToolCallResult> callTool(
            String serverUrl,
            String toolName,
            Map<String, Object> args) {

        log.info("Calling tool {} on server {}", toolName, serverUrl);

        Map<String, Object> request = Map.of(
            "tool_name", toolName,
            "args", args
        );

        return webClient.post()
            .uri(serverUrl + "/tools/call")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ToolCallResult.class)
            .toFuture();
    }

    /**
     * Result from a tool call
     */
    public record ToolCallResult(boolean success, Object output, String error) {}
}
