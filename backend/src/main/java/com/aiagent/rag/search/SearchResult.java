package com.aiagent.rag.search;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class SearchResult {
    private String id;
    private String content;
    private double score;
    private Map<String, Object> metadata;
    private String source;
}
