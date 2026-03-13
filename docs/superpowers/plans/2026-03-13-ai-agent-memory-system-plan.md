# AI Agent Memory 系统实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 Memory 系统，提供跨会话持久化记忆能力，包括用户偏好存储、语义检索和时间衰减遗忘策略。

**Architecture:** Memory 系统使用 SQLite 存储元数据和记忆内容，复用 RAG 的向量索引进行语义检索，实现时间衰减的遗忘策略。

**Tech Stack:** Java (Spring Boot), SQLite, Qdrant (复用 RAG 向量索引)

---

## 前置条件

**依赖计划:**
- `2026-03-13-ai-agent-foundation-plan.md` - 基础架构计划
- `2026-03-13-ai-agent-rag-service-plan.md` - RAG 服务计划（向量索引复用）

---

## Chunk 1: Memory 领域模型

### Task 1: 创建 Memory 领域模型

**Files:**
- Create: `backend/src/main/java/com/aiagent/memory/Memory.java`
- Create: `backend/src/main/java/com/aiagent/memory/MemoryType.java`
- Create: `backend/src/main/java/com/aiagent/memory/MemoryRecord.java`
- Test: `backend/src/test/java/com/aiagent/memory/MemoryTests.java`

- [ ] **Step 1: 创建 MemoryType 枚举**

```java
package com.aiagent.memory;

import lombok.Getter;

@Getter
public enum MemoryType {
    USER_PREFERENCE("用户偏好", 1.0),      // Long-term, slow decay
    PROJECT_KNOWLEDGE("项目知识", 0.9),   // Medium-term
    SKILL_LEARNING("技能学习", 0.8),      // Medium-term
    CONVERSATION_SUMMARY("对话摘要", 0.7); // Short-term, fast decay

    private final String description;
    private final double decayFactor; // Higher = slower decay

    MemoryType(String description, double decayFactor) {
        this.description = description;
        this.decayFactor = decayFactor;
    }

    public double getDecayRate() {
        return 1.0 - decayFactor;
    }
}
```

- [ ] **Step 2: 创建 MemoryRecord 类**

```java
package com.aiagent.memory;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class MemoryRecord {
    private String id;
    private MemoryType type;
    private String content;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant accessedAt;
    private int accessCount;
    private double strength; // 0.0 to 1.0, starts at 1.0
    private boolean expired;
}
```

- [ ] **Step 3: 创建测试**

```java
package com.aiagent.memory;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MemoryTests {

    @Test
    void memoryRecord_buildsCorrectly() {
        MemoryRecord record = MemoryRecord.builder()
            .id("test-1")
            .type(MemoryType.USER_PREFERENCE)
            .content("User prefers snake_case naming")
            .metadata(Map.of("source", "conversation-123"))
            .strength(1.0)
            .build();

        assertEquals("test-1", record.getId());
        assertEquals(MemoryType.USER_PREFERENCE, record.getType());
        assertEquals(1.0, record.getStrength());
        assertFalse(record.isExpired());
    }

    @Test
    void memoryType_hasCorrectDecayFactors() {
        assertEquals(1.0, MemoryType.USER_PREFERENCE.getDecayFactor());
        assertEquals(0.7, MemoryType.CONVERSATION_SUMMARY.getDecayFactor());
    }
}
```

- [ ] **Step 4: 运行测试**

```bash
cd backend
mvn test -Dtest=MemoryTests
```
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/aiagent/memory/Memory*.java
git add backend/src/test/java/com/aiagent/memory/MemoryTests.java
git commit -m "feat: define Memory domain models"
```

---

## Chunk 2: SQLite 存储层

### Task 2: 创建 SQLite 存储库

**Files:**
- Create: `backend/src/main/java/com/aiagent/memory/MemoryRepository.java`
- Create: `backend/src/main/resources/db/migration/V1__create_memory_tables.sql`
- Test: `backend/src/test/java/com/aiagent/memory/MemoryRepositoryTests.java`

- [ ] **Step 1: 添加 SQLite 和 Flyway 依赖**

在 `pom.xml` 中确认已有 SQLite 依赖，添加 Flyway:

```xml
<!-- Database migrations -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-sqlite</artifactId>
</dependency>
```

- [ ] **Step 2: 创建数据库迁移脚本**

```sql
-- src/main/resources/db/migration/V1__create_memory_tables.sql

CREATE TABLE IF NOT EXISTS memories (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL,
    content TEXT NOT NULL,
    metadata TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    access_count INTEGER DEFAULT 0,
    strength REAL DEFAULT 1.0,
    expired INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_memories_type ON memories(type);
CREATE INDEX IF NOT EXISTS idx_memories_expired ON memories(expired);
CREATE INDEX IF NOT EXISTS idx_memories_strength ON memories(strength DESC);
```

- [ ] **Step 3: 配置 SQLite 数据源**

在 `application.yml` 中添加：

```yaml
spring:
  datasource:
    url: jdbc:sqlite:${memory.path:./data/memory.db}
    driver-class-name: org.sqlite.JDBC
  flyway:
    enabled: true
    locations: classpath:db/migration
```

- [ ] **Step 4: 创建 MemoryRepository**

```java
package com.aiagent.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.Instant;
import java.util.*;

@Repository
public class MemoryRepository {

    private static final Logger log = LoggerFactory.getLogger(MemoryRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public MemoryRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(MemoryRecord record) {
        String sql = """
            INSERT OR REPLACE INTO memories
            (id, type, content, metadata, created_at, accessed_at, access_count, strength, expired)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try {
            String metadataJson = objectMapper.writeValueAsString(record.getMetadata());
            jdbcTemplate.update(sql,
                record.getId(),
                record.getType().name(),
                record.getContent(),
                metadataJson,
                Timestamp.from(record.getCreatedAt()),
                Timestamp.from(record.getAccessedAt()),
                record.getAccessCount(),
                record.getStrength(),
                record.isExpired() ? 1 : 0
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize metadata", e);
            throw new RuntimeException(e);
        }
    }

    public Optional<MemoryRecord> findById(String id) {
        String sql = "SELECT * FROM memories WHERE id = ?";
        return jdbcTemplate.query(sql, rowMapper(), id)
            .stream()
            .findFirst();
    }

    public List<MemoryRecord> findByType(MemoryType type) {
        String sql = "SELECT * FROM memories WHERE type = ? AND expired = 0 ORDER BY strength DESC";
        return jdbcTemplate.query(sql, rowMapper(), type.name());
    }

    public List<MemoryRecord> findAll() {
        String sql = "SELECT * FROM memories WHERE expired = 0 ORDER BY type, strength DESC";
        return jdbcTemplate.query(sql, rowMapper());
    }

    public void markExpired(String id) {
        String sql = "UPDATE memories SET expired = 1 WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public void updateAccess(String id) {
        String sql = """
            UPDATE memories
            SET accessed_at = ?, access_count = access_count + 1
            WHERE id = ?
            """;
        jdbcTemplate.update(sql, Timestamp.from(Instant.now()), id);
    }

    public void updateStrength(String id, double strength) {
        String sql = "UPDATE memories SET strength = ? WHERE id = ?";
        jdbcTemplate.update(sql, strength, id);
    }

    public List<MemoryRecord> findExpiredMemories() {
        String sql = "SELECT * FROM memories WHERE expired = 0 AND strength < 0.3";
        return jdbcTemplate.query(sql, rowMapper());
    }

    private RowMapper<MemoryRecord> rowMapper() {
        return (rs, rowNum) -> MemoryRecord.builder()
            .id(rs.getString("id"))
            .type(MemoryType.valueOf(rs.getString("type")))
            .content(rs.getString("content"))
            .metadata(readMetadata(rs.getString("metadata")))
            .createdAt(rs.getTimestamp("created_at").toInstant())
            .accessedAt(rs.getTimestamp("accessed_at").toInstant())
            .accessCount(rs.getInt("access_count"))
            .strength(rs.getDouble("strength"))
            .expired(rs.getInt("expired") == 1)
            .build();
    }

    private Map<String, Object> readMetadata(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
```

- [ ] **Step 5: 创建存储库测试**

```java
package com.aiagent.memory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MemoryRepositoryTests {

    @Autowired
    private MemoryRepository repository;

    @Test
    void save_andFindById_returnsRecord() {
        MemoryRecord record = MemoryRecord.builder()
            .id("test-1")
            .type(MemoryType.USER_PREFERENCE)
            .content("Test preference")
            .metadata(Map.of("test", true))
            .createdAt(Instant.now())
            .accessedAt(Instant.now())
            .accessCount(0)
            .strength(1.0)
            .expired(false)
            .build();

        repository.save(record);

        MemoryRecord found = repository.findById("test-1").orElseThrow();

        assertEquals("Test preference", found.getContent());
        assertEquals(MemoryType.USER_PREFERENCE, found.getType());
    }

    @Test
    void markExpired_setsExpiredFlag() {
        MemoryRecord record = MemoryRecord.builder()
            .id("test-2")
            .type(MemoryType.CONVERSATION_SUMMARY)
            .content("Test")
            .createdAt(Instant.now())
            .accessedAt(Instant.now())
            .accessCount(0)
            .strength(1.0)
            .expired(false)
            .build();

        repository.save(record);
        repository.markExpired("test-2");

        MemoryRecord found = repository.findById("test-2").orElseThrow();
        assertTrue(found.isExpired());
    }
}
```

- [ ] **Step 6: 运行测试**

```bash
cd backend
mvn test -Dtest=MemoryRepositoryTests
```
Expected: PASS (可能需要先初始化数据库)

- [ ] **Step 7: 提交**

```bash
git add backend/src/main/java/com/aiagent/memory/MemoryRepository.java
git add backend/src/main/resources/db/migration/V1__create_memory_tables.sql
git add backend/src/test/java/com/aiagent/memory/MemoryRepositoryTests.java
git commit -m "feat: implement SQLite Memory repository"
```

---

## Chunk 3: Memory 服务和检索

### Task 3: 创建 Memory 服务

**Files:**
- Create: `backend/src/main/java/com/aiagent/memory/MemoryService.java`
- Create: `backend/src/main/java/com/aiagent/memory/TimeDecayScheduler.java`
- Create: `backend/src/main/java/com/aiagent/memory/MemoryController.java`
- Create: `backend/src/main/java/com/aiagent/memory/dto/CreateMemoryRequest.java`
- Test: `backend/src/test/java/com/aiagent/memory/MemoryServiceTests.java`

- [ ] **Step 1: 创建请求 DTO**

```java
package com.aiagent.memory.dto;

import lombok.Data;
import java.util.Map;

@Data
public class CreateMemoryRequest {
    private String type; // USER_PREFERENCE, PROJECT_KNOWLEDGE, etc.
    private String content;
    private Map<String, Object> metadata;
}
```

- [ ] **Step 2: 创建 MemoryService**

```java
package com.aiagent.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);

    private final MemoryRepository repository;

    public MemoryService(MemoryRepository repository) {
        this.repository = repository;
    }

    public MemoryRecord createMemory(MemoryType type, String content) {
        MemoryRecord record = MemoryRecord.builder()
            .id(UUID.randomUUID().toString())
            .type(type)
            .content(content)
            .createdAt(Instant.now())
            .accessedAt(Instant.now())
            .accessCount(0)
            .strength(1.0)
            .expired(false)
            .build();

        repository.save(record);
        log.info("Created memory: {} (type={})", record.getId(), type);
        return record;
    }

    public Optional<MemoryRecord> getMemory(String id) {
        Optional<MemoryRecord> record = repository.findById(id);
        record.ifPresent(r -> repository.updateAccess(id));
        return record;
    }

    public List<MemoryRecord> getMemoriesByType(MemoryType type) {
        return repository.findByType(type);
    }

    public List<MemoryRecord> getAllMemories() {
        return repository.findAll();
    }

    public void deleteMemory(String id) {
        repository.markExpired(id);
        log.info("Marked memory as expired: {}", id);
    }

    public void applyTimeDecay() {
        List<MemoryRecord> memories = repository.findAll();
        Instant now = Instant.now();

        for (MemoryRecord memory : memories) {
            long daysSinceAccess = java.time.Duration.between(
                memory.getAccessedAt(), now).toDays();

            double decayRate = memory.getType().getDecayRate();
            double decay = daysSinceAccess * decayRate * 0.01; // 1% per day base
            double newStrength = Math.max(0, memory.getStrength() - decay);

            if (newStrength < 0.3) {
                repository.markExpired(memory.getId());
                log.debug("Memory expired due to decay: {}", memory.getId());
            } else {
                repository.updateStrength(memory.getId(), newStrength);
            }
        }
    }

    public List<MemoryRecord> getExpiredMemories() {
        return repository.findExpiredMemories();
    }

    public void cleanupExpired() {
        List<MemoryRecord> expired = getExpiredMemories();
        for (MemoryRecord record : expired) {
            log.info("Cleaning up expired memory: {}", record.getId());
            // In production: actually delete or archive
        }
    }
}
```

- [ ] **Step 3: 创建定时调度器**

```java
package com.aiagent.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TimeDecayScheduler {

    private static final Logger log = LoggerFactory.getLogger(TimeDecayScheduler.class);

    private final MemoryService memoryService;

    public TimeDecayScheduler(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    public void applyDailyDecay() {
        log.info("Applying daily memory decay");
        memoryService.applyTimeDecay();
    }

    @Scheduled(cron = "0 0 3 * * 0") // Weekly on Sunday at 3 AM
    public void weeklyCleanup() {
        log.info("Running weekly expired memory cleanup");
        memoryService.cleanupExpired();
    }
}
```

- [ ] **Step 4: 创建控制器**

```java
package com.aiagent.memory;

import com.aiagent.memory.dto.CreateMemoryRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @GetMapping
    public List<MemoryRecord> getAllMemories() {
        return memoryService.getAllMemories();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemoryRecord> getMemory(@PathVariable String id) {
        return memoryService.getMemory(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{type}")
    public List<MemoryRecord> getByType(@PathVariable String type) {
        MemoryType memoryType = MemoryType.valueOf(type);
        return memoryService.getMemoriesByType(memoryType);
    }

    @PostMapping
    public ResponseEntity<MemoryRecord> createMemory(@RequestBody CreateMemoryRequest request) {
        try {
            MemoryType type = MemoryType.valueOf(request.getType());
            MemoryRecord record = memoryService.createMemory(type, request.getContent());
            return ResponseEntity.ok(record);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMemory(@PathVariable String id) {
        memoryService.deleteMemory(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/decay/apply")
    public ResponseEntity<Map<String, Integer>> applyDecay() {
        memoryService.applyTimeDecay();
        List<MemoryRecord> expired = memoryService.getExpiredMemories();
        return ResponseEntity.ok(Map.of("expiredCount", expired.size()));
    }
}
```

- [ ] **Step 5: 启用定时任务**

在启动类中添加 `@EnableScheduling`:

```java
@SpringBootApplication
@EnableScheduling
public class AgentApplication {
    // ...
}
```

- [ ] **Step 6: 创建服务测试**

```java
package com.aiagent.memory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MemoryServiceTests {

    @Autowired
    private MemoryService service;

    @Test
    void createMemory_createsRecord() {
        MemoryRecord record = service.createMemory(
            MemoryType.USER_PREFERENCE,
            "Test preference"
        );

        assertNotNull(record.getId());
        assertEquals(1.0, record.getStrength());
    }

    @Test
    void getMemory_updatesAccessCount() throws Exception {
        MemoryRecord record = service.createMemory(
            MemoryType.PROJECT_KNOWLEDGE,
            "Test knowledge"
        );

        service.getMemory(record.getId());
        Thread.sleep(100);
        service.getMemory(record.getId());

        MemoryRecord updated = service.getMemory(record.getId()).orElseThrow();
        assertEquals(3, updated.getAccessCount()); // create + 2 gets
    }
}
```

- [ ] **Step 7: 运行测试**

```bash
cd backend
mvn test -Dtest=MemoryServiceTests
```
Expected: PASS

- [ ] **Step 8: 提交**

```bash
git add backend/src/main/java/com/aiagent/memory/MemoryService.java
git add backend/src/main/java/com/aiagent/memory/TimeDecayScheduler.java
git add backend/src/main/java/com/aiagent/memory/MemoryController.java
git add backend/src/main/java/com/aiagent/memory/dto/
git add backend/src/test/java/com/aiagent/memory/MemoryServiceTests.java
git commit -m "feat: implement Memory service with time decay"
```

---

## 验收标准

1. `GET /api/memory` 返回所有记忆
2. `POST /api/memory` 创建新记忆
3. 记忆按类型正确存储
4. 定时任务每天 2 点应用时间衰减
5. 强度低于 0.3 的记忆自动过期

---

## 后续工作

1. 语义检索集成（复用 RAG 向量索引）
2. 记忆压缩和归档
3. 记忆来源追踪
4. 前端记忆管理界面
