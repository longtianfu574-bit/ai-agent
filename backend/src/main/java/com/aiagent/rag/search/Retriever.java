package com.aiagent.rag.search;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Retriever {
    CompletableFuture<List<SearchResult>> retrieve(SearchRequest request);
}
