package com.aiagent.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * MCP Service for managing MCP server connections and tool execution
 */
@Service
public class McpService {

    private static final Logger log = LoggerFactory.getLogger(McpService.class);

    private final McpClient mcpClient;
    private final Map<String, String> serverUrls;

    public McpService(
            McpClient mcpClient,
            @Value("${mcp.servers.filesystem:http://localhost:5001}") String filesystemUrl,
            @Value("${mcp.servers.shell:http://localhost:5002}") String shellUrl,
            @Value("${mcp.servers.database:http://localhost:5003}") String databaseUrl,
            @Value("${mcp.servers.http:http://localhost:5004}") String httpUrl,
            @Value("${mcp.servers.git:http://localhost:5005}") String gitUrl) {

        this.mcpClient = mcpClient;
        this.serverUrls = Map.of(
            "filesystem", filesystemUrl,
            "shell", shellUrl,
            "database", databaseUrl,
            "http", httpUrl,
            "git", gitUrl
        );
        log.info("MCP Service initialized with servers: {}", serverUrls.keySet());
    }

    /**
     * List all tools from all MCP servers
     */
    public CompletableFuture<List<McpTool>> listAllTools() {
        log.debug("Listing all tools from all MCP servers");

        List<CompletableFuture<List<McpTool>>> futures = serverUrls.entrySet().stream()
            .map(entry -> mcpClient.listTools(entry.getValue())
                .thenApply(tools -> {
                    tools.forEach(tool ->
                        tool.setName(entry.getKey() + "/" + tool.getName()));
                    return tools;
                }))
            .collect(Collectors.toList());

        return CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .flatMap(f -> f.join().stream())
                .collect(Collectors.toList()));
    }

    /**
     * Call a tool on a specific MCP server
     */
    public CompletableFuture<McpClient.ToolCallResult> callTool(
            String server,
            String toolName,
            Map<String, Object> args) {

        String url = serverUrls.get(server);
        if (url == null) {
            log.error("Unknown MCP server: {}", server);
            CompletableFuture<McpClient.ToolCallResult> failed = new CompletableFuture<>();
            failed.completeExceptionally(
                new IllegalArgumentException("Unknown server: " + server));
            return failed;
        }

        log.info("Calling tool {} on server {} ({})", toolName, server, url);
        return mcpClient.callTool(url, toolName, args);
    }

    /**
     * Get the URL for a specific MCP server
     */
    public String getServerUrl(String server) {
        return serverUrls.get(server);
    }

    /**
     * Get all configured server names
     */
    public Set<String> getServerNames() {
        return serverUrls.keySet();
    }
}
