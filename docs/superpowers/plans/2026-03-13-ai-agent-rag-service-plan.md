# AI Agent RAG 服务实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 RAG（检索增强生成）服务，包括 Qdrant 向量数据库集成、检索服务、索引服务和 Python Sidecar 嵌入服务。

**Architecture:** RAG 服务分为三个独立服务：索引服务（文档分块、嵌入、存储）、检索服务（相似度搜索、过滤）、查询服务（查询改写、重排序、上下文构建）。嵌入生成通过 Python Sidecar HTTP 调用。

**Tech Stack:** Java (Spring AI), Qdrant, Python (FastAPI + sentence-transformers), bge-m3 模型

---

## 前置条件

**依赖计划:**
- `2026-03-13-ai-agent-foundation-plan.md` - 基础架构计划（必须已完成）

**环境准备:**
- Qdrant 容器运行中
- Python Sidecar 容器运行中
- 网络连通性验证通过

---

## Chunk 1: Qdrant 向量数据库配置

### Task 1: 创建 Qdrant 配置和客户端

**Files:**
- Create: `backend/src/main/java/com/aiagent/rag/QdrantConfig.java`
- Create: `backend/src/main/java/com/aiagent/rag/QdrantClientFactory.java`
- Create: `backend/src/main/resources/application-rag.yml`
- Test: `backend/src/test/java/com/aiagent/rag/QdrantConfigTests.java`

- [ ] **Step 1: 创建 Qdrant 配置类**

```java
package com.aiagent.rag;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "qdrant")
public class QdrantConfig {
    private String host = "localhost";
    private int port = 6334;
    private String collection = "ai-agent-rag";
    private int vectorSize = 1024; // bge-m3 output dimension
}
```

- [ ] **Step 2: 创建 QdrantClientFactory**

```java
package com.aiagent.rag;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.stereotype.Component;

@Component
public class QdrantClientFactory {

    private final QdrantConfig config;

    public QdrantClientFactory(QdrantConfig config) {
        this.config = config;
    }

    public QdrantClient createClient() {
        QdrantGrpcClient grpcClient = QdrantGrpcClient.newBuilder(
                config.getHost(),
                config.getPort(),
                false // usePlaintext
            ).build();
        return new QdrantClient(grpcClient);
    }
}
```

- [ ] **Step 3: 创建 RAG 配置覆写文件**

```yaml
# application-rag.yml
spring:
  config:
    activate:
      on-profile: rag

qdrant:
  host: ${QDRANT_HOST:localhost}
  port: ${QDRANT_PORT:6334}
  collection: ${QDRANT_COLLECTION:ai-agent-rag}
  vector-size: 1024

embedding:
  service-url: ${EMBEDDING_SERVICE_URL:http://localhost:5000}
  model: bge-m3
```

- [ ] **Step 4: 创建 Qdrant 配置测试**

```java
package com.aiagent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "qdrant.host=test-host",
    "qdrant.port=9999",
    "qdrant.collection=test-collection"
})
class QdrantConfigTests {

    @Autowired
    private QdrantConfig config;

    @Test
    void configLoadsPropertiesCorrectly() {
        assertEquals("test-host", config.getHost());
        assertEquals(9999, config.getPort());
        assertEquals("test-collection", config.getCollection());
    }
}
```

- [ ] **Step 5: 运行测试**

```bash
cd backend
mvn test -Dtest=QdrantConfigTests
```
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/aiagent/rag/Qdrant*.java
git add backend/src/main/resources/application-rag.yml
git add backend/src/test/java/com/aiagent/rag/QdrantConfigTests.java
git commit -m "feat: add Qdrant configuration and client factory"
```

---

### Task 2: 创建 Qdrant 集合初始化服务

**Files:**
- Create: `backend/src/main/java/com/aiagent/rag/QdrantCollectionInitializer.java`
- Create: `backend/src/main/java/com/aiagent/rag/QdrantCollectionConfig.java`
- Test: `backend/src/test/java/com/aiagent/rag/QdrantCollectionInitializerTests.java`

- [ ] **Step 1: 创建集合配置类**

```java
package com.aiagent.rag;

import lombok.Data;

@Data
public class QdrantCollectionConfig {
    private String collectionName;
    private int vectorSize;
    private String distance = "Cosine"; // Cosine, Euclid, Dot

    public static QdrantCollectionConfig defaultConfig(String collectionName, int vectorSize) {
        QdrantCollectionConfig config = new QdrantCollectionConfig();
        config.setCollectionName(collectionName);
        config.setVectorSize(vectorSize);
        config.setDistance("Cosine");
        return config;
    }
}
```

- [ ] **Step 2: 创建集合初始化器**

```java
package com.aiagent.rag;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Collections.Distance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class QdrantCollectionInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(QdrantCollectionInitializer.class);

    private final QdrantClientFactory clientFactory;
    private final QdrantConfig config;

    public QdrantCollectionInitializer(
            QdrantClientFactory clientFactory,
            QdrantConfig config) {
        this.clientFactory = clientFactory;
        this.config = config;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initializeCollection();
    }

    private void initializeCollection() throws ExecutionException, InterruptedException {
        try (QdrantClient client = clientFactory.createClient()) {
            String collectionName = config.getCollection();

            // Check if collection exists
            boolean exists = client.collectionExists(collectionName).get().getResult();

            if (!exists) {
                log.info("Creating Qdrant collection: {}", collectionName);
                client.createCollectionAsync(
                    collectionName,
                    Collections.VectorParams.newBuilder()
                        .setSize(config.getVectorSize())
                        .setDistance(Distance.Cosine)
                        .build()
                ).get();
                log.info("Collection created successfully");
            } else {
                log.info("Collection {} already exists", collectionName);
            }
        }
    }
}
```

- [ ] **Step 3: 创建初始化器测试 (使用 Testcontainers)**

```java
package com.aiagent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "qdrant.host=localhost",
    "qdrant.port=6334",
    "qdrant.collection=test-init-collection"
})
class QdrantCollectionInitializerTests {

    @Autowired
    private QdrantCollectionInitializer initializer;

    @Test
    void initializer_runsWithoutError() throws Exception {
        // This test verifies the initializer can run without throwing
        // In a real test, you would use Testcontainers for Qdrant
        initializer.run(new org.springframework.boot.DefaultApplicationArguments());
    }
}
```

- [ ] **Step 4: 运行测试**

```bash
cd backend
mvn test -Dtest=QdrantCollectionInitializerTests
```
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/aiagent/rag/QdrantCollection*.java
git add backend/src/test/java/com/aiagent/rag/QdrantCollectionInitializerTests.java
git commit -m "feat: add Qdrant collection auto-initialization"
```

---

## Chunk 2: Python Sidecar 嵌入服务客户端

### Task 3: 创建嵌入服务客户端

**Files:**
- Create: `backend/src/main/java/com/aiagent/embedding/EmbeddingService.java`
- Create: `backend/src/main/java/com/aiagent/embedding/EmbeddingRequest.java`
- Create: `backend/src/main/java/com/aiagent/embedding/EmbeddingResponse.java`
- Create: `backend/src/main/java/com/aiagent/embedding/EmbeddingClient.java`
- Test: `backend/src/test/java/com/aiagent/embedding/EmbeddingClientTests.java`

- [ ] **Step 1: 创建嵌入请求 DTO**

```java
package com.aiagent.embedding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmbeddingRequest {
    private List<String> texts;
}
```

- [ ] **Step 2: 创建嵌入响应 DTO**

```java
package com.aiagent.embedding;

import lombok.Data;
import java.util.List;

@Data
public class EmbeddingResponse {
    private List<List<Float>> embeddings;
}
```

- [ ] **Step 3: 创建嵌入服务接口**

```java
package com.aiagent.embedding;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface EmbeddingService {
    CompletableFuture<List<List<Float>>> embed(List<String> texts);
    CompletableFuture<List<Float>> embedSingle(String text);
}
```

- [ ] **Step 4: 创建嵌入客户端实现**

```java
package com.aiagent.embedding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class EmbeddingClient implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public EmbeddingClient(
            @Value("${embedding.service-url:http://localhost:5000}") String serviceUrl,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
            .baseUrl(serviceUrl)
            .build();
    }

    @Override
    public CompletableFuture<List<List<Float>>> embed(List<String> texts) {
        log.debug("Requesting embeddings for {} texts", texts.size());

        EmbeddingRequest request = new EmbeddingRequest(texts);

        return webClient.post()
            .uri("/embed")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(EmbeddingResponse.class)
            .map(EmbeddingResponse::getEmbeddings)
            .toFuture();
    }

    @Override
    public CompletableFuture<List<Float>> embedSingle(String text) {
        return embed(List.of(text))
            .thenApply(embeddings -> embeddings.get(0));
    }
}
```

- [ ] **Step 5: 添加 WebFlux 依赖 (用于 WebClient)**

在 `pom.xml` 中添加：

```xml
<!-- WebFlux for async HTTP client -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

- [ ] **Step 6: 创建客户端测试 (使用 WireMock)**

```java
package com.aiagent.embedding;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@WireMockTest(httpPort = 9999)
@TestPropertySource(properties = {
    "embedding.service-url=http://localhost:9999"
})
class EmbeddingClientTests {

    @Autowired
    private EmbeddingClient client;

    @Test
    void embed_returnsEmbeddings() throws Exception {
        stubFor(post("/embed")
            .willReturn(okJson("""
                {
                    "embeddings": [[0.1, 0.2, 0.3], [0.4, 0.5, 0.6]]
                }
                """)));

        var result = client.embed(List.of("text1", "text2")).join();

        assertEquals(2, result.size());
        assertEquals(3, result.get(0).size());
    }

    @Test
    void embedSingle_returnsSingleEmbedding() throws Exception {
        stubFor(post("/embed")
            .willReturn(okJson("""
                {
                    "embeddings": [[0.1, 0.2, 0.3]]
                }
                """)));

        var result = client.embedSingle("text1").join();

        assertEquals(3, result.size());
        assertEquals(0.1f, result.get(0));
    }
}
```

- [ ] **Step 7: 添加 WireMock 测试依赖**

```xml
<!-- Test dependencies -->
<dependency>
    <groupId>com.github.tomakehurst</groupId>
    <artifactId>wiremock-jre8</artifactId>
    <version>2.35.1</version>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 8: 运行测试**

```bash
cd backend
mvn test -Dtest=EmbeddingClientTests
```
Expected: PASS

- [ ] **Step 9: 提交**

```bash
git add backend/src/main/java/com/aiagent/embedding/
git add backend/src/test/java/com/aiagent/embedding/
git add backend/pom.xml
git commit -m "feat: implement Python Sidecar embedding client"
```

---

## Chunk 3: RAG 检索服务

### Task 4: 创建检索服务核心逻辑

**Files:**
- Create: `backend/src/main/java/com/aiagent/rag/search/SearchService.java`
- Create: `backend/src/main/java/com/aiagent/rag/search/SearchRequest.java`
- Create: `backend/src/main/java/com/aiagent/rag/search/SearchResult.java`
- Create: `backend/src/main/java/com/aiagent/rag/search/Retriever.java`
- Test: `backend/src/test/java/com/aiagent/rag/search/SearchServiceTests.java`

- [ ] **Step 1: 创建搜索请求 DTO**

```java
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
```

- [ ] **Step 2: 创建搜索结果 DTO**

```java
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
```

- [ ] **Step 3: 创建检索器接口**

```java
package com.aiagent.rag.search;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Retriever {
    CompletableFuture<List<SearchResult>> retrieve(SearchRequest request);
}
```

- [ ] **Step 4: 创建 Qdrant 检索器实现**

```java
package com.aiagent.rag.search;

import com.aiagent.embedding.EmbeddingService;
import com.aiagent.rag.QdrantClientFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Filter;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class QdrantRetriever implements Retriever {

    private static final Logger log = LoggerFactory.getLogger(QdrantRetriever.class);

    private final QdrantClientFactory clientFactory;
    private final EmbeddingService embeddingService;
    private final String collectionName;

    public QdrantRetriever(
            QdrantClientFactory clientFactory,
            EmbeddingService embeddingService,
            @Value("${qdrant.collection:ai-agent-rag}") String collectionName) {
        this.clientFactory = clientFactory;
        this.embeddingService = embeddingService;
        this.collectionName = collectionName;
    }

    @Override
    public CompletableFuture<List<SearchResult>> retrieve(SearchRequest request) {
        log.info("Retrieving for query: {}", request.getQuery());

        return embeddingService.embedSingle(request.getQuery())
            .thenCompose(embedding -> {
                try (QdrantClient client = clientFactory.createClient()) {
                    SearchPoints.Builder searchBuilder = SearchPoints.newBuilder()
                        .setCollectionName(collectionName)
                        .addAllVector(embedding)
                        .setLimit(request.getTopK())
                        .setWithPayload(true)
                        .setWithVectors(false);

                    if (request.getFilter() != null) {
                        // Add filter logic here
                    }

                    return client.searchAsync(searchBuilder.build())
                        .thenApply(response -> {
                            List<SearchResult> results = response.getResultList().stream()
                                .filter(point -> point.getScore() >= request.getScoreThreshold())
                                .map(this::toSearchResult)
                                .collect(Collectors.toList());

                            log.info("Found {} results", results.size());
                            return results;
                        });
                } catch (Exception e) {
                    log.error("Search failed", e);
                    CompletableFuture<List<SearchResult>> failed = new CompletableFuture<>();
                    failed.completeExceptionally(e);
                    return failed;
                }
            });
    }

    private SearchResult toSearchResult(ScoredPoint point) {
        return SearchResult.builder()
            .id(point.getId().getUuid())
            .score(point.getScore())
            .content(point.getPayload().getOrDefault("content", "")
                .getKindValue().toString())
            .source(point.getPayload().getOrDefault("source", "")
                .getKindValue().toString())
            .build();
    }
}
```

- [ ] **Step 5: 创建搜索服务门面**

```java
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
```

- [ ] **Step 6: 创建搜索服务测试**

```java
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
```

- [ ] **Step 7: 运行测试**

```bash
cd backend
mvn test -Dtest=SearchServiceTests
```
Expected: PASS

- [ ] **Step 8: 提交**

```bash
git add backend/src/main/java/com/aiagent/rag/search/
git add backend/src/test/java/com/aiagent/rag/search/
git commit -m "feat: implement RAG search service with Qdrant retriever"
```

---

## Chunk 4: RAG 索引服务

### Task 5: 创建索引服务

**Files:**
- Create: `backend/src/main/java/com/aiagent/rag/index/IndexService.java`
- Create: `backend/src/main/java/com/aiagent/rag/index/Document.java`
- Create: `backend/src/main/java/com/aiagent/rag/index/DocumentChunker.java`
- Create: `backend/src/main/java/com/aiagent/rag/index/IndexController.java`
- Test: `backend/src/test/java/com/aiagent/rag/index/DocumentChunkerTests.java`

- [ ] **Step 1: 创建文档 DTO**

```java
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
```

- [ ] **Step 2: 创建文档分块器**

```java
package com.aiagent.rag.index;

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
```

- [ ] **Step 3: 创建索引服务**

```java
package com.aiagent.rag.index;

import com.aiagent.embedding.EmbeddingService;
import com.aiagent.rag.QdrantClientFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.Value;
import io.qdrant.client.grpc.Points.Vectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class IndexService {

    private static final Logger log = LoggerFactory.getLogger(IndexService.class);

    private final QdrantClientFactory clientFactory;
    private final EmbeddingService embeddingService;
    private final DocumentChunker chunker;
    private final String collectionName;

    public IndexService(
            QdrantClientFactory clientFactory,
            EmbeddingService embeddingService,
            DocumentChunker chunker,
            @Value("${qdrant.collection:ai-agent-rag}") String collectionName) {
        this.clientFactory = clientFactory;
        this.embeddingService = embeddingService;
        this.chunker = chunker;
        this.collectionName = collectionName;
    }

    public CompletableFuture<Integer> indexDocument(Document document) {
        log.info("Indexing document: {}", document.getId());

        List<Document> chunks = chunker.chunkDocument(document);
        log.info("Document split into {} chunks", chunks.size());

        return indexChunks(chunks);
    }

    private CompletableFuture<Integer> indexChunks(List<Document> chunks) {
        // Get embeddings for all chunks
        List<String> texts = chunks.stream()
            .map(Document::getContent)
            .collect(Collectors.toList());

        return embeddingService.embed(texts)
            .thenApply(embeddings -> {
                try (QdrantClient client = clientFactory.createClient()) {
                    List<PointStruct> points = new ArrayList<>();

                    for (int i = 0; i < chunks.size(); i++) {
                        Document chunk = chunks.get(i);
                        List<Float> embedding = embeddings.get(i);

                        Map<String, Value> payload = new HashMap<>();
                        payload.put("content", Value.newBuilder()
                            .setStringValue(chunk.getContent()).build());
                        payload.put("source", Value.newBuilder()
                            .setStringValue(chunk.getSource()).build());
                        if (chunk.getMetadata() != null) {
                            chunk.getMetadata().forEach((k, v) -> {
                                payload.put(k, Value.newBuilder()
                                    .setStringValue(v.toString()).build());
                            });
                        }

                        PointStruct point = PointStruct.newBuilder()
                            .setId(io.qdrant.client.grpc.Points.PointId.newBuilder()
                                .setUuid(chunk.getId()).build())
                            .setVectors(Vectors.newBuilder()
                                .setData(io.qdrant.client.grpc.Points.Vector.newBuilder()
                                    .addAllData(embedding).build()).build())
                            .putAllPayload(payload)
                            .build();

                        points.add(point);
                    }

                    client.upsertAsync(collectionName, points).join();
                    log.info("Indexed {} chunks", chunks.size());
                    return chunks.size();
                }
            });
    }
}
```

- [ ] **Step 4: 创建索引控制器**

```java
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
```

- [ ] **Step 5: 创建分块器测试**

```java
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
```

- [ ] **Step 6: 运行测试**

```bash
cd backend
mvn test -Dtest=DocumentChunkerTests
```
Expected: PASS

- [ ] **Step 7: 提交**

```bash
git add backend/src/main/java/com/aiagent/rag/index/
git add backend/src/test/java/com/aiagent/rag/index/DocumentChunkerTests.java
git commit -m "feat: implement RAG index service with document chunking"
```

---

## Chunk 5: RAG 查询服务和 API

### Task 6: 创建查询服务和聊天集成

**Files:**
- Create: `backend/src/main/java/com/aiagent/rag/query/QueryService.java`
- Create: `backend/src/main/java/com/aiagent/rag/query/QueryRewriter.java`
- Modify: `backend/src/main/java/com/aiagent/controller/ChatController.java`
- Test: `backend/src/test/java/com/aiagent/rag/query/QueryServiceTests.java`

- [ ] **Step 1: 创建查询重写器**

```java
package com.aiagent.rag.query;

import org.springframework.stereotype.Component;

/**
 * Simple query rewriter - can be enhanced with LLM-based HyDE
 */
@Component
public class QueryRewriter {

    /**
     * Rewrite query for better retrieval
     * Currently a simple identity function, can be enhanced with:
     * - HyDE (Hypothetical Document Embeddings)
     * - Query expansion
     * - Synonym addition
     */
    public String rewrite(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }

        // Basic cleanup
        return query.trim()
            .replaceAll("\\s+", " ")
            .toLowerCase();
    }
}
```

- [ ] **Step 2: 创建查询服务**

```java
package com.aiagent.rag.query;

import com.aiagent.rag.search.SearchRequest;
import com.aiagent.rag.search.SearchResult;
import com.aiagent.rag.search.SearchService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class QueryService {

    private final SearchService searchService;
    private final QueryRewriter rewriter;

    public QueryService(SearchService searchService, QueryRewriter rewriter) {
        this.searchService = searchService;
        this.rewriter = rewriter;
    }

    public CompletableFuture<List<SearchResult>> query(String userQuery, int topK) {
        String rewrittenQuery = rewriter.rewrite(userQuery);

        SearchRequest request = SearchRequest.builder()
            .query(rewrittenQuery)
            .topK(topK)
            .scoreThreshold(0.5)
            .build();

        return searchService.search(request);
    }

    public CompletableFuture<String> buildContext(String userQuery, int topK) {
        return query(userQuery, topK)
            .thenApply(results -> {
                StringBuilder context = new StringBuilder();
                context.append("Relevant context:\n\n");

                for (int i = 0; i < results.size(); i++) {
                    SearchResult result = results.get(i);
                    context.append(String.format("[%d] %s (score: %.2f, source: %s)\n",
                        i + 1, result.getContent(), result.getScore(), result.getSource()));
                }

                return context.toString();
            });
    }
}
```

- [ ] **Step 3: 更新 Chat 控制器集成 RAG**

```java
package com.aiagent.controller;

import com.aiagent.dto.ChatRequest;
import com.aiagent.dto.ChatResponse;
import com.aiagent.rag.query.QueryService;
import com.aiagent.rag.search.SearchResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final QueryService queryService;

    public ChatController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping("/chat")
    public CompletableFuture<ChatResponse> chat(@RequestBody ChatRequest request) {
        String sessionId = request.getSessionId() != null
            ? request.getSessionId()
            : UUID.randomUUID().toString();

        // Query RAG for context
        return queryService.buildContext(request.getMessage(), 5)
            .thenApply(context -> {
                ChatResponse response = new ChatResponse();
                response.setSessionId(sessionId);
                response.setModel("rag-enhanced");

                // In a real implementation, this would call an LLM
                // For now, return the context as the response
                response.setMessage(
                    "Context retrieved:\n\n" + context +
                    "\n\n(This is a placeholder - LLM integration pending)");

                return response;
            });
    }
}
```

- [ ] **Step 4: 创建查询服务测试**

```java
package com.aiagent.rag.query;

import com.aiagent.rag.search.SearchResult;
import com.aiagent.rag.search.SearchService;
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
class QueryServiceTests {

    @Autowired
    private QueryService queryService;

    @MockBean
    private SearchService searchService;

    @Test
    void query_rewritesAndSearches() throws Exception {
        SearchResult mockResult = SearchResult.builder()
            .content("test content")
            .score(0.85)
            .source("test-source")
            .build();

        when(searchService.search(any(SearchRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(List.of(mockResult)));

        var results = queryService.query("test query", 5).join();

        assertEquals(1, results.size());
        assertEquals("test content", results.get(0).getContent());
    }

    @Test
    void buildContext_returnsFormattedContext() throws Exception {
        SearchResult mockResult = SearchResult.builder()
            .content("relevant info")
            .score(0.9)
            .source("doc1")
            .build();

        when(searchService.search(any(SearchRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(List.of(mockResult)));

        String context = queryService.buildContext("test", 5).join();

        assertTrue(context.contains("Relevant context"));
        assertTrue(context.contains("relevant info"));
        assertTrue(context.contains("score: 0.90"));
    }
}
```

- [ ] **Step 5: 运行测试**

```bash
cd backend
mvn test -Dtest=QueryServiceTests
```
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/aiagent/rag/query/
git add backend/src/main/java/com/aiagent/controller/ChatController.java
git add backend/src/test/java/com/aiagent/rag/query/
git commit -m "feat: integrate RAG query service with chat endpoint"
```

---

## 测试策略

RAG 服务测试覆盖：
- 单元测试：每个 Service、Chunker、Rewriter
- 集成测试：Qdrant 连接、嵌入服务调用
- 端到端测试：完整 RAG 检索流程

---

## 验收标准

1. `POST /api/rag/index` 成功索引文档
2. `POST /api/chat` 返回 RAG 增强的响应
3. Qdrant 集合自动创建
4. Python Sidecar 嵌入服务正常调用
5. 分块逻辑正确处理长文档

---

## 后续工作

RAG 服务基础功能完成后，可以增强：
1. 语义分块 (Semantic Chunking)
2. HyDE 查询重写
3. Cross-encoder 重排序
4. 多向量索引支持
5. 引用追踪和来源显示
