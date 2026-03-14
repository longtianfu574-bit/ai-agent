package com.aiagent.rag.search;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class SearchRequest {
    private String query;
    private int topK;
    private Map<String, Object> filter;
    private double scoreThreshold;
}
