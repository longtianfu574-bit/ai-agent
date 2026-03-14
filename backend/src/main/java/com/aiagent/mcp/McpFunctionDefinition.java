package com.aiagent.mcp;

import lombok.Data;
import java.util.Map;

/**
 * MCP Tool definition for LLM function calling
 */
@Data
public class McpFunctionDefinition {
    private String name;
    private String description;
    private Parameters parameters;

    @Data
    public static class Parameters {
        private String type = "object";
        private Map<String, ParameterProperty> properties;
        private java.util.List<String> required;
    }

    @Data
    public static class ParameterProperty {
        private String type;
        private String description;
    }
}
