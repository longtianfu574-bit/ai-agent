package com.aiagent.rag;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "qdrant")
public class QdrantConfig {
    private String host = "localhost";
    private int port = 6334;
    private String collection = "ai-agent-rag";
    private int vectorSize = 1024; // Qwen3-Embedding output dimension
}
