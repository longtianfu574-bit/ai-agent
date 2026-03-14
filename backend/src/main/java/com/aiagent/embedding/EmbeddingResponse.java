package com.aiagent.embedding;

import lombok.Data;
import java.util.List;

@Data
public class EmbeddingResponse {
    private List<Float> embedding;
}
