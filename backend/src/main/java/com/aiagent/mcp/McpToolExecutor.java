package com.aiagent.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * MCP Tool Executor - Executes MCP tools based on LLM function calling results
 */
@Service
public class McpToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(McpToolExecutor.class);

    private final McpService mcpService;
    private final ObjectMapper objectMapper;

    public McpToolExecutor(McpService mcpService, ObjectMapper objectMapper) {
        this.mcpService = mcpService;
        this.objectMapper = objectMapper;
    }

    /**
     * Get all MCP tool definitions for LLM function calling
     */
    public List<McpFunctionDefinition> getFunctionDefinitions() {
        List<McpFunctionDefinition> functions = new ArrayList<>();

        // Filesystem tools
        functions.add(createFunction("read_file",
            "Read contents of a file from the filesystem",
            Map.of("path", createProperty("string", "File path relative to root")),
            java.util.List.of("path")));

        functions.add(createFunction("write_file",
            "Write content to a file in the filesystem",
            Map.of(
                "path", createProperty("string", "File path"),
                "content", createProperty("string", "Content to write")
            ),
            java.util.List.of("path", "content")));

        functions.add(createFunction("list_directory",
            "List contents of a directory",
            Map.of("path", createProperty("string", "Directory path")),
            java.util.List.of("path")));

        functions.add(createFunction("delete_file",
            "Delete a file from the filesystem",
            Map.of("path", createProperty("string", "File path")),
            java.util.List.of("path")));

        // Shell tools
        functions.add(createFunction("execute_shell",
            "Execute a shell command and return the output",
            Map.of(
                "command", createProperty("string", "Shell command to execute"),
                "timeout", createProperty("integer", "Timeout in seconds (default 30)")
            ),
            java.util.List.of("command")));

        // Database tools
        functions.add(createFunction("execute_sql",
            "Execute a SQL query against the database",
            Map.of(
                "sql", createProperty("string", "SQL query to execute"),
                "params", createProperty("object", "Query parameters (optional)")
            ),
            java.util.List.of("sql")));

        // HTTP tools
        functions.add(createFunction("http_get",
            "Make an HTTP GET request to fetch data from a URL",
            Map.of("url", createProperty("string", "URL to request")),
            java.util.List.of("url")));

        functions.add(createFunction("http_post",
            "Make an HTTP POST request with a body",
            Map.of(
                "url", createProperty("string", "URL to request"),
                "body", createProperty("string", "Request body (JSON)")
            ),
            java.util.List.of("url", "body")));

        // Git tools
        functions.add(createFunction("git_status",
            "Get the git status of a repository",
            Map.of("path", createProperty("string", "Repository path")),
            java.util.List.of("path")));

        functions.add(createFunction("git_commit",
            "Commit changes to git repository",
            Map.of(
                "path", createProperty("string", "Repository path"),
                "message", createProperty("string", "Commit message")
            ),
            java.util.List.of("path", "message")));

        functions.add(createFunction("git_init",
            "Initialize a new git repository",
            Map.of("path", createProperty("string", "Directory path")),
            java.util.List.of("path")));

        return functions;
    }

    private McpFunctionDefinition createFunction(String name, String description,
                                                   Map<String, McpFunctionDefinition.ParameterProperty> properties,
                                                   java.util.List<String> required) {
        McpFunctionDefinition def = new McpFunctionDefinition();
        def.setName(name);
        def.setDescription(description);

        McpFunctionDefinition.Parameters params = new McpFunctionDefinition.Parameters();
        params.setProperties(properties);
        params.setRequired(required);

        def.setParameters(params);
        return def;
    }

    private McpFunctionDefinition.ParameterProperty createProperty(String type, String description) {
        McpFunctionDefinition.ParameterProperty prop = new McpFunctionDefinition.ParameterProperty();
        prop.setType(type);
        prop.setDescription(description);
        return prop;
    }

    /**
     * Execute a tool based on LLM function call response
     */
    public CompletableFuture<McpClient.ToolCallResult> executeTool(
            String functionName,
            JsonNode arguments) {

        log.info("Executing MCP tool: {} with args: {}", functionName, arguments);

        Map<String, Object> args = objectMapper.convertValue(arguments, Map.class);

        // Map function names to server/tool
        String server;
        String tool;

        switch (functionName) {
            case "read_file":
            case "write_file":
            case "list_directory":
            case "delete_file":
                server = "filesystem";
                tool = functionName;
                break;
            case "execute_shell":
                server = "shell";
                tool = "execute";
                break;
            case "execute_sql":
                server = "database";
                tool = "query";
                break;
            case "http_get":
                server = "http";
                tool = "get";
                break;
            case "http_post":
                server = "http";
                tool = "post";
                break;
            case "git_status":
            case "git_commit":
            case "git_init":
                server = "git";
                tool = functionName.replace("git_", "");
                break;
            default:
                log.error("Unknown function: {}", functionName);
                CompletableFuture<McpClient.ToolCallResult> failed = new CompletableFuture<>();
                failed.completeExceptionally(new IllegalArgumentException("Unknown function: " + functionName));
                return failed;
        }

        return mcpService.callTool(server, tool, args);
    }

    /**
     * Format tool execution result for LLM response
     */
    public String formatResult(McpClient.ToolCallResult result, String toolName) {
        if (result.success()) {
            return String.format("【工具调用成功】%s 执行结果:\n%s",
                toolName, formatOutput(result.output()));
        } else {
            return String.format("【工具调用失败】%s 执行出错：%s",
                toolName, result.error());
        }
    }

    private String formatOutput(Object output) {
        if (output == null) {
            return "null";
        }
        if (output instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) output;
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sb.append(String.format("%s: %s\n", entry.getKey(), entry.getValue()));
            }
            return sb.toString();
        }
        if (output instanceof List) {
            List<?> list = (List<?>) output;
            StringBuilder sb = new StringBuilder();
            for (Object item : list) {
                sb.append(formatOutput(item)).append("\n");
            }
            return sb.toString();
        }
        return output.toString();
    }
}
