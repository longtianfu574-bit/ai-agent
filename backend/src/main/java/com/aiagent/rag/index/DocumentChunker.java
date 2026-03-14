package com.aiagent.rag.index;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentChunker {

    private final int chunkSize;
    private final int chunkOverlap;

    public DocumentChunker(
            @Value("${rag.chunk-size:512}") int chunkSize,
            @Value("${rag.chunk-overlap:50}") int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public List<String> chunk(String content) {
        List<String> chunks = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            return chunks;
        }

        // Simple fixed-size chunking with overlap
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            String chunk = content.substring(start, end);

            // Try to break at sentence boundary
            if (end < content.length()) {
                int lastPeriod = chunk.lastIndexOf(".");
                if (lastPeriod > chunkSize / 2) {
                    end = start + lastPeriod + 1;
                    chunk = content.substring(start, end);
                }
            }

            chunks.add(chunk.trim());
            start = end - chunkOverlap;
        }

        return chunks;
    }

    public List<Document> chunkDocument(Document document) {
        List<String> chunks = chunk(document.getContent());
        List<Document> chunkedDocs = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            chunkedDocs.add(Document.builder()
                .id(document.getId() + "_chunk_" + i)
                .content(chunks.get(i))
                .source(document.getSource())
                .metadata(document.getMetadata())
                .build());
        }

        return chunkedDocs;
    }
}
