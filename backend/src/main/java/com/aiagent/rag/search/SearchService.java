package com.aiagent.rag.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final Retriever retriever;

    public SearchService(Retriever retriever) {
        this.retriever = retriever;
    }

    public CompletableFuture<List<SearchResult>> search(SearchRequest request) {
        log.info("Search request: query={}, topK={}",
            request.getQuery(), request.getTopK());

        // Set defaults
        if (request.getTopK() == 0) {
            request.setTopK(5);
        }
        if (request.getScoreThreshold() == 0) {
            request.setScoreThreshold(0.5);
        }

        return retriever.retrieve(request);
    }
}
