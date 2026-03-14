package com.aiagent.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class EmbeddingClient implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public EmbeddingClient(
            @Value("${embedding.service-url:http://localhost:58080/embedding}") String serviceUrl,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
            .baseUrl(serviceUrl)
            .build();
    }

    @Override
    public CompletableFuture<List<List<Float>>> embed(List<String> texts) {
        log.debug("Requesting embeddings for {} texts", texts.size());

        // Process texts one by one or in batches depending on your embedding service
        List<CompletableFuture<List<Float>>> futures = texts.stream()
            .map(this::embedSingle)
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());
    }

    @Override
    public CompletableFuture<List<Float>> embedSingle(String text) {
        log.debug("Requesting embedding for text: {}", text.substring(0, Math.min(50, text.length())));

        EmbeddingRequest request = new EmbeddingRequest();
        request.setInput(text);

        return webClient.post()
            .uri("/embeddings")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(EmbeddingResponse.class)
            .map(EmbeddingResponse::getEmbedding)
            .toFuture();
    }
}
