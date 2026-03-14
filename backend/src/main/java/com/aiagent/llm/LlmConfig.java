package com.aiagent.llm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmConfig {
    private String baseUrl = "http://123.57.224.128:58080/v1";
    private String apiKey = "sk-a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0";
    private String model = "Qwen3.5-4B-Q4_K_M.gguf";
}
