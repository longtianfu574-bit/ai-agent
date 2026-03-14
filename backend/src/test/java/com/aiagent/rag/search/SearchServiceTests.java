package com.aiagent.rag.search;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class SearchServiceTests {

    @Autowired
    private SearchService searchService;

    @MockBean
    private Retriever retriever;

    @Test
    void search_returnsResults() throws Exception {
        SearchResult mockResult = SearchResult.builder()
            .id("test-id")
            .content("test content")
            .score(0.85)
            .build();

        when(retriever.retrieve(any(SearchRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(List.of(mockResult)));

        SearchRequest request = SearchRequest.builder()
            .query("test query")
            .topK(5)
            .build();

        List<SearchResult> results = searchService.search(request).join();

        assertEquals(1, results.size());
        assertEquals("test-id", results.get(0).getId());
        assertEquals(0.85, results.get(0).getScore());
    }
}
