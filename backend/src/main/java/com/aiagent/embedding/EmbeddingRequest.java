package com.aiagent.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmbeddingRequest {
    private String input;

    @JsonProperty("model")
    private String model = "Qwen3-Embedding";
}
