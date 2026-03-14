package com.aiagent.rag.index;

import com.aiagent.embedding.EmbeddingService;
import com.aiagent.rag.QdrantClientFactory;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.UpdateResult;
import io.qdrant.client.grpc.Points.Vector;
import io.qdrant.client.grpc.Points.Vectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class IndexService {

    private static final Logger log = LoggerFactory.getLogger(IndexService.class);

    private final QdrantClientFactory clientFactory;
    private final EmbeddingService embeddingService;
    private final DocumentChunker chunker;
    private final String collectionName;

    public IndexService(
            QdrantClientFactory clientFactory,
            EmbeddingService embeddingService,
            DocumentChunker chunker,
            @org.springframework.beans.factory.annotation.Value("${qdrant.collection:ai-agent-rag}") String collectionName) {
        this.clientFactory = clientFactory;
        this.embeddingService = embeddingService;
        this.chunker = chunker;
        this.collectionName = collectionName;
    }

    public CompletableFuture<Integer> indexDocument(Document document) {
        log.info("Indexing document: {}", document.getId());

        List<Document> chunks = chunker.chunkDocument(document);
        log.info("Document split into {} chunks", chunks.size());

        return indexChunks(chunks);
    }

    private CompletableFuture<Integer> indexChunks(List<Document> chunks) {
        // Get embeddings for all chunks
        List<String> texts = chunks.stream()
            .map(Document::getContent)
            .collect(Collectors.toList());

        return embeddingService.embed(texts)
            .thenApply(embeddings -> {
                try (QdrantClient client = clientFactory.createClient()) {
                    List<PointStruct> points = new ArrayList<>();

                    for (int i = 0; i < chunks.size(); i++) {
                        Document chunk = chunks.get(i);
                        List<Float> embedding = embeddings.get(i);

                        Map<String, Value> payload = new HashMap<>();
                        payload.put("content", Value.newBuilder()
                            .setStringValue(chunk.getContent()).build());
                        payload.put("source", Value.newBuilder()
                            .setStringValue(chunk.getSource()).build());
                        if (chunk.getMetadata() != null) {
                            chunk.getMetadata().forEach((k, v) -> {
                                payload.put(k, Value.newBuilder()
                                    .setStringValue(v.toString()).build());
                            });
                        }

                        Vector vector = Vector.newBuilder()
                            .addAllData(embedding).build();

                        // Use integer ID for chunks (Qdrant supports both UUID and integer IDs)
                        long chunkId = i; // Simple sequential ID for chunks

                        PointStruct point = PointStruct.newBuilder()
                            .setId(io.qdrant.client.grpc.Points.PointId.newBuilder()
                                .setNum(chunkId).build())
                            .setVectors(Vectors.newBuilder()
                                .setVector(vector).build())
                            .putAllPayload(payload)
                            .build();

                        points.add(point);
                    }

                    ListenableFuture<UpdateResult> future = client.upsertAsync(collectionName, points);
                    CompletableFuture<UpdateResult> completableFuture = new CompletableFuture<>();
                    Futures.addCallback(future, new FutureCallback<UpdateResult>() {
                        @Override
                        public void onSuccess(UpdateResult result) {
                            completableFuture.complete(result);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            completableFuture.completeExceptionally(t);
                        }
                    }, Runnable::run);
                    completableFuture.join();

                    log.info("Indexed {} chunks", chunks.size());
                    return chunks.size();
                }
            });
    }
}
