package com.aiagent.rag.query;

import com.aiagent.llm.LlmClient;
import com.aiagent.rag.search.SearchRequest;
import com.aiagent.rag.search.SearchResult;
import com.aiagent.rag.search.SearchService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class QueryService {

    private final SearchService searchService;
    private final QueryRewriter rewriter;
    private final LlmClient llmClient;

    public QueryService(SearchService searchService, QueryRewriter rewriter, LlmClient llmClient) {
        this.searchService = searchService;
        this.rewriter = rewriter;
        this.llmClient = llmClient;
    }

    public CompletableFuture<List<SearchResult>> query(String userQuery, int topK) {
        String rewrittenQuery = rewriter.rewrite(userQuery);
        return searchService.search(SearchRequest.builder()
            .query(rewrittenQuery)
            .topK(topK)
            .scoreThreshold(0.5)
            .build());
    }

    public CompletableFuture<String> buildContext(String userQuery, int topK) {
        return query(userQuery, topK)
            .thenApply(results -> {
                StringBuilder context = new StringBuilder();
                for (int i = 0; i < results.size(); i++) {
                    var result = results.get(i);
                    context.append(String.format("[%d] %s (score: %.2f)\n",
                        i + 1, result.getContent(), result.getScore()));
                }
                return context.toString();
            });
    }

    /**
     * 完整的 RAG 流程：检索 + LLM 生成
     */
    public CompletableFuture<String> ragComplete(String userQuery, int topK) {
        return buildContext(userQuery, topK)
            .thenCompose(context -> {
                if (context.isBlank()) {
                    return CompletableFuture.completedFuture(
                        "抱歉，没有找到相关的上下文信息。");
                }
                return llmClient.complete(userQuery, context);
            });
    }
}
