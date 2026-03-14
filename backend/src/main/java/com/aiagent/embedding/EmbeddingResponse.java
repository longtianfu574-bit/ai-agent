package com.aiagent.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class EmbeddingResponse {
    private String model;
    private String object;
    private Usage usage;
    private List<EmbeddingData> data;

    @JsonProperty("embedding")
    private List<Float> embedding;

    @Data
    public static class EmbeddingData {
        private List<Float> embedding;
        private int index;
        private String object;
    }

    @Data
    public static class Usage {
        private int promptTokens;
        private int totalTokens;
    }

    public List<Float> getEmbedding() {
        if (data != null && !data.isEmpty()) {
            return data.get(0).getEmbedding();
        }
        return embedding;
    }
}
