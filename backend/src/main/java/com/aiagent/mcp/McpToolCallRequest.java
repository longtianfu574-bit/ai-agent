package com.aiagent.mcp;

import lombok.Data;
import java.util.Map;

/**
 * Request to call an MCP tool
 */
@Data
public class McpToolCallRequest {
    private String server;
    private String tool;
    private Map<String, Object> args;
}
