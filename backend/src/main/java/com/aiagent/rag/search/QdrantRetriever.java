package com.aiagent.rag.search;

import com.aiagent.embedding.EmbeddingService;
import com.aiagent.rag.QdrantClientFactory;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.WithPayloadSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class QdrantRetriever implements Retriever {

    private static final Logger log = LoggerFactory.getLogger(QdrantRetriever.class);

    private final QdrantClientFactory clientFactory;
    private final EmbeddingService embeddingService;
    private final String collectionName;

    public QdrantRetriever(
            QdrantClientFactory clientFactory,
            EmbeddingService embeddingService,
            @org.springframework.beans.factory.annotation.Value("${qdrant.collection:ai-agent-rag}") String collectionName) {
        this.clientFactory = clientFactory;
        this.embeddingService = embeddingService;
        this.collectionName = collectionName;
    }

    @Override
    public CompletableFuture<List<SearchResult>> retrieve(SearchRequest request) {
        log.info("Retrieving for query: {}", request.getQuery());

        return embeddingService.embedSingle(request.getQuery())
            .thenCompose(embedding -> {
                try (QdrantClient client = clientFactory.createClient()) {
                    SearchPoints.Builder searchBuilder = SearchPoints.newBuilder()
                        .setCollectionName(collectionName)
                        .addAllVector(embedding)
                        .setLimit(request.getTopK())
                        .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true).build())
                        .setWithVectors(io.qdrant.client.grpc.Points.WithVectorsSelector.newBuilder()
                            .setEnable(true).build());

                    if (request.getFilter() != null) {
                        // Add filter logic here
                    }

                    ListenableFuture<List<ScoredPoint>> future = client.searchAsync(searchBuilder.build());
                    CompletableFuture<List<SearchResult>> completableFuture = new CompletableFuture<>();
                    Futures.addCallback(future, new FutureCallback<List<ScoredPoint>>() {
                        @Override
                        public void onSuccess(List<ScoredPoint> response) {
                            List<SearchResult> results = response.stream()
                                .filter(point -> point.getScore() >= request.getScoreThreshold())
                                .map(QdrantRetriever.this::toSearchResult)
                                .collect(Collectors.toList());

                            log.info("Found {} results", results.size());
                            completableFuture.complete(results);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            completableFuture.completeExceptionally(t);
                        }
                    }, Runnable::run);

                    return completableFuture;
                } catch (Exception e) {
                    log.error("Search failed", e);
                    CompletableFuture<List<SearchResult>> failed = new CompletableFuture<>();
                    failed.completeExceptionally(e);
                    return failed;
                }
            });
    }

    private SearchResult toSearchResult(ScoredPoint point) {
        String content = extractStringValue(point.getPayload().get("content"));
        String source = extractStringValue(point.getPayload().get("source"));

        return SearchResult.builder()
            .id(point.getId().getUuid())
            .score(point.getScore())
            .content(content)
            .source(source)
            .build();
    }

    private String extractStringValue(io.qdrant.client.grpc.JsonWithInt.Value value) {
        if (value == null) {
            return "";
        }
        if (value.hasStringValue()) {
            return value.getStringValue();
        }
        return value.getKindCase().toString();
    }
}
