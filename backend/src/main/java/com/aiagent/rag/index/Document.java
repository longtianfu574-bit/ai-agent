package com.aiagent.rag.index;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class Document {
    private String id;
    private String content;
    private String source;
    private Map<String, Object> metadata;
}
