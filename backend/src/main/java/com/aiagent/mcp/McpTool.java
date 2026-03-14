package com.aiagent.mcp;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;

/**
 * MCP Tool definition
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpTool {
    private String name;
    private String description;
    private Map<String, Object> parameters;
}
