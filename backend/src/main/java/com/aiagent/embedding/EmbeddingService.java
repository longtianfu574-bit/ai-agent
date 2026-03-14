package com.aiagent.embedding;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface EmbeddingService {
    CompletableFuture<List<List<Float>>> embed(List<String> texts);
    CompletableFuture<List<Float>> embedSingle(String text);
}
