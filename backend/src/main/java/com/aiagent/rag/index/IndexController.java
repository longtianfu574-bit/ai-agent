package com.aiagent.rag.index;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/rag")
public class IndexController {

    private final IndexService indexService;

    public IndexController(IndexService indexService) {
        this.indexService = indexService;
    }

    @PostMapping("/index")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> indexDocument(
            @RequestBody Map<String, String> request) {

        String content = request.get("content");
        String source = request.getOrDefault("source", "unknown");

        if (content == null || content.isEmpty()) {
            CompletableFuture<ResponseEntity<Map<String, Object>>> failed = new CompletableFuture<>();
            failed.complete(ResponseEntity.badRequest()
                .body(Map.of("error", "Content is required")));
            return failed;
        }

        Document document = Document.builder()
            .id(UUID.randomUUID().toString())
            .content(content)
            .source(source)
            .build();

        return indexService.indexDocument(document)
            .thenApply(chunkCount -> {
                Map<String, Object> response = new HashMap<>();
                response.put("documentId", document.getId());
                response.put("chunksIndexed", chunkCount);
                response.put("status", "success");
                return ResponseEntity.ok(response);
            });
    }
}
