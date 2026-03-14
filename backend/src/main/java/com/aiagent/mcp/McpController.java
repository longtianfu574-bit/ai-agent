package com.aiagent.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for MCP tool execution
 */
@RestController
@RequestMapping("/api/mcp")
public class McpController {

    private static final Logger log = LoggerFactory.getLogger(McpController.class);

    private final McpService mcpService;

    public McpController(McpService mcpService) {
        this.mcpService = mcpService;
    }

    /**
     * List all available MCP servers
     */
    @GetMapping("/servers")
    public Set<String> listServers() {
        log.debug("Listing MCP servers");
        return mcpService.getServerNames();
    }

    /**
     * List all tools from all MCP servers
     */
    @GetMapping("/tools")
    public CompletableFuture<List<McpTool>> listAllTools() {
        log.debug("Listing all MCP tools");
        return mcpService.listAllTools();
    }

    /**
     * List tools from a specific MCP server
     */
    @GetMapping("/servers/{server}/tools")
    public CompletableFuture<List<McpTool>> listServerTools(
            @PathVariable String server) {
        log.debug("Listing tools for MCP server: {}", server);
        String url = mcpService.getServerUrl(server);
        if (url == null) {
            throw new IllegalArgumentException("Unknown server: " + server);
        }
        return mcpService.listAllTools()
            .thenApply(tools -> tools.stream()
                .filter(t -> t.getName().startsWith(server + "/"))
                .toList());
    }

    /**
     * Call an MCP tool
     */
    @PostMapping("/tools/call")
    public CompletableFuture<McpToolCallResponse> callTool(
            @RequestBody McpToolCallRequest request) {
        log.info("Calling MCP tool: {}/{} with args: {}",
            request.getServer(), request.getTool(), request.getArgs());

        return mcpService.callTool(request.getServer(), request.getTool(), request.getArgs())
            .thenApply(result -> {
                McpToolCallResponse response = new McpToolCallResponse();
                response.setSuccess(result.success());
                response.setOutput(result.output());
                response.setError(result.error());
                return response;
            })
            .exceptionally(ex -> {
                log.error("MCP tool call failed", ex);
                McpToolCallResponse response = new McpToolCallResponse();
                response.setSuccess(false);
                response.setError(ex.getMessage());
                return response;
            });
    }

    /**
     * Execute a filesystem read operation
     */
    @GetMapping("/filesystem/read")
    public CompletableFuture<McpToolCallResponse> readFile(
            @RequestParam String path) {
        log.debug("Reading file via MCP: {}", path);
        return mcpService.callTool("filesystem", "read_file", Map.of("path", path))
            .thenApply(result -> {
                McpToolCallResponse response = new McpToolCallResponse();
                response.setSuccess(result.success());
                response.setOutput(result.output());
                response.setError(result.error());
                return response;
            });
    }

    /**
     * Execute a filesystem write operation
     */
    @PostMapping("/filesystem/write")
    public CompletableFuture<McpToolCallResponse> writeFile(
            @RequestParam String path,
            @RequestBody String content) {
        log.debug("Writing file via MCP: {}", path);
        return mcpService.callTool("filesystem", "write_file",
                Map.of("path", path, "content", content))
            .thenApply(result -> {
                McpToolCallResponse response = new McpToolCallResponse();
                response.setSuccess(result.success());
                response.setOutput(result.output());
                response.setError(result.error());
                return response;
            });
    }

    /**
     * List directory contents
     */
    @GetMapping("/filesystem/list")
    public CompletableFuture<McpToolCallResponse> listDirectory(
            @RequestParam String path) {
        log.debug("Listing directory via MCP: {}", path);
        return mcpService.callTool("filesystem", "list_directory", Map.of("path", path))
            .thenApply(result -> {
                McpToolCallResponse response = new McpToolCallResponse();
                response.setSuccess(result.success());
                response.setOutput(result.output());
                response.setError(result.error());
                return response;
            });
    }

    /**
     * Execute a shell command
     */
    @PostMapping("/shell/execute")
    public CompletableFuture<McpToolCallResponse> executeCommand(
            @RequestParam String command,
            @RequestParam(defaultValue = "30") int timeout) {
        log.debug("Executing shell command via MCP: {}", command);
        return mcpService.callTool("shell", "execute",
                Map.of("command", command, "timeout", timeout))
            .thenApply(result -> {
                McpToolCallResponse response = new McpToolCallResponse();
                response.setSuccess(result.success());
                response.setOutput(result.output());
                response.setError(result.error());
                return response;
            });
    }

    /**
     * Execute a database query
     */
    @PostMapping("/database/query")
    public CompletableFuture<McpToolCallResponse> executeQuery(
            @RequestParam String sql,
            @RequestBody(required = false) Map<String, Object> params) {
        log.debug("Executing SQL query via MCP: {}", sql);
        Map<String, Object> args = new HashMap<>();
        args.put("sql", sql);
        if (params != null) {
            args.put("params", params);
        }
        return mcpService.callTool("database", "query", args)
            .thenApply(result -> {
                McpToolCallResponse response = new McpToolCallResponse();
                response.setSuccess(result.success());
                response.setOutput(result.output());
                response.setError(result.error());
                return response;
            });
    }

    /**
     * Make an HTTP GET request
     */
    @GetMapping("/http/get")
    public CompletableFuture<McpToolCallResponse> httpGet(
            @RequestParam String url) {
        log.debug("Making HTTP GET request via MCP: {}", url);
        return mcpService.callTool("http", "get", Map.of("url", url))
            .thenApply(result -> {
                McpToolCallResponse response = new McpToolCallResponse();
                response.setSuccess(result.success());
                response.setOutput(result.output());
                response.setError(result.error());
                return response;
            });
    }

    /**
     * Make an HTTP POST request
     */
    @PostMapping("/http/post")
    public CompletableFuture<McpToolCallResponse> httpPost(
            @RequestParam String url,
            @RequestBody String body) {
        log.debug("Making HTTP POST request via MCP: {}", url);
        return mcpService.callTool("http", "post",
                Map.of("url", url, "body", body))
            .thenApply(result -> {
                McpToolCallResponse response = new McpToolCallResponse();
                response.setSuccess(result.success());
                response.setOutput(result.output());
                response.setError(result.error());
                return response;
            });
    }

    /**
     * Get git status
     */
    @GetMapping("/git/status")
    public CompletableFuture<McpToolCallResponse> gitStatus(
            @RequestParam String path) {
        log.debug("Getting git status via MCP: {}", path);
        return mcpService.callTool("git", "status", Map.of("path", path))
            .thenApply(result -> {
                McpToolCallResponse response = new McpToolCallResponse();
                response.setSuccess(result.success());
                response.setOutput(result.output());
                response.setError(result.error());
                return response;
            });
    }

    /**
     * Initialize a git repository
     */
    @PostMapping("/git/init")
    public CompletableFuture<McpToolCallResponse> gitInit(
            @RequestParam String path) {
        log.debug("Initializing git repo via MCP: {}", path);
        return mcpService.callTool("git", "init", Map.of("path", path))
            .thenApply(result -> {
                McpToolCallResponse response = new McpToolCallResponse();
                response.setSuccess(result.success());
                response.setOutput(result.output());
                response.setError(result.error());
                return response;
            });
    }
}
