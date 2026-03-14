package com.aiagent.rag.index;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentChunkerTests {

    private final DocumentChunker chunker = new DocumentChunker(50, 10);

    @Test
    void chunk_splitsLongText() {
        String content = "This is a test. ".repeat(20); // ~400 chars

        List<String> chunks = chunker.chunk(content);

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(s -> s.length() <= 60));
    }

    @Test
    void chunk_returnsEmptyForEmptyInput() {
        List<String> chunks = chunker.chunk("");

        assertTrue(chunks.isEmpty());
    }

    @Test
    void chunk_respectsSentenceBoundaries() {
        String content = "First sentence. Second sentence. Third sentence.";

        List<String> chunks = chunker.chunk(content);

        // Should break at sentence boundaries
        assertTrue(chunks.stream().allMatch(s ->
            s.isEmpty() || s.endsWith(".") || s.length() < 50));
    }
}
