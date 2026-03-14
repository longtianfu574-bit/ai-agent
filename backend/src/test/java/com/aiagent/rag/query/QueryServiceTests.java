package com.aiagent.rag.query;

import com.aiagent.llm.LlmClient;
import com.aiagent.rag.search.Retriever;
import com.aiagent.rag.search.SearchRequest;
import com.aiagent.rag.search.SearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class QueryServiceTests {

    @Autowired
    private QueryService queryService;

    @MockBean
    private Retriever retriever;

    @MockBean
    private LlmClient llmClient;

    @Test
    void query_returnsResults() throws Exception {
        SearchResult mockResult = SearchResult.builder()
            .id("test-id")
            .content("test content")
            .score(0.85)
            .build();

        when(retriever.retrieve(any(SearchRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(List.of(mockResult)));

        List<SearchResult> results = queryService.query("test query", 5).join();

        assertEquals(1, results.size());
        assertEquals("test-id", results.get(0).getId());
        assertEquals(0.85, results.get(0).getScore());
    }

    @Test
    void buildContext_returnsFormattedContext() throws Exception {
        SearchResult mockResult = SearchResult.builder()
            .id("test-id")
            .content("test content")
            .score(0.85)
            .build();

        when(retriever.retrieve(any(SearchRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(List.of(mockResult)));

        String context = queryService.buildContext("test query", 5).join();

        assertTrue(context.contains("[1] test content"));
        assertTrue(context.contains("score: 0.85"));
    }

    @Test
    void ragComplete_returnsLlmResponse() throws Exception {
        SearchResult mockResult = SearchResult.builder()
            .id("test-id")
            .content("test content")
            .score(0.85)
            .build();

        when(retriever.retrieve(any(SearchRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(List.of(mockResult)));

        when(llmClient.complete(anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture("This is the LLM response."));

        String response = queryService.ragComplete("test query", 5).join();

        assertEquals("This is the LLM response.", response);
    }

    @Test
    void ragComplete_returnsFallbackWhenNoContext() throws Exception {
        when(retriever.retrieve(any(SearchRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(List.of()));

        String response = queryService.ragComplete("test query", 5).join();

        assertEquals("抱歉，没有找到相关的上下文信息。", response);
    }
}
