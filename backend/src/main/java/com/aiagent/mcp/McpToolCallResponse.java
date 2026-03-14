package com.aiagent.mcp;

import lombok.Data;
import java.util.List;

/**
 * Response from MCP tool call
 */
@Data
public class McpToolCallResponse {
    private boolean success;
    private Object output;
    private String error;
    private List<McpTool> tools;
    private List<String> servers;
}
