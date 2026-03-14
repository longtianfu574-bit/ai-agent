package com.aiagent.rag.query;

import org.springframework.stereotype.Component;

@Component
public class QueryRewriter {
    public String rewrite(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }
        return query.trim().replaceAll("\\s+", " ");
    }
}
