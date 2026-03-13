# AI Agent 基础架构实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建 AI Agent 系统的基础架构，包括项目骨架、Docker Compose 配置和 API Gateway。

**Architecture:** 微服务架构，使用 Docker Compose 编排所有服务。Java Spring Boot 作为后端核心，Vue 3 作为前端界面。

**Tech Stack:** Java 17, Spring Boot 3.x, Vue 3, TypeScript, Docker Compose, Qdrant, SQLite

---

## Chunk 1: 项目骨架

### Task 1: 创建 Java Spring Boot 项目结构

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/aiagent/AgentApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/java/com/aiagent/AgentApplicationTests.java`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <groupId>com.aiagent</groupId>
    <artifactId>ai-agent</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>AI Agent</name>
    <description>AI Agent with Skills, MCP, RAG, Memory</description>

    <properties>
        <java.version>17</java.version>
        <spring-ai.version>1.0.0-M4</spring-ai.version>
        <langchain4j.version>0.25.0</langchain4j.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Core -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring AI -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter</artifactId>
            <version>${spring-ai.version}</version>
        </dependency>

        <!-- Qdrant Client -->
        <dependency>
            <groupId>io.qdrant</groupId>
            <artifactId>qdrant-client</artifactId>
            <version>1.7.1</version>
        </dependency>

        <!-- SQLite -->
        <dependency>
            <groupId>org.xerial</groupId>
            <artifactId>sqlite-jdbc</artifactId>
            <version>3.44.1.0</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建主应用类**

```java
package com.aiagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建 application.yml**

```yaml
server:
  port: 8080

spring:
  application:
    name: ai-agent
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:your-api-key}
      chat:
        options:
          model: gpt-4

qdrant:
  host: localhost
  port: 6334
  collection: ai-agent-rag

memory:
  path: ./data/memory.db
```

- [ ] **Step 4: 创建测试类**

```java
package com.aiagent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AgentApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 5: 运行测试验证**

```bash
cd backend
mvn test
```
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add backend/
git commit -m "feat: initialize Spring Boot backend project"
```

---

### Task 2: 创建 Vue 3 前端项目结构

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig.json`
- Create: `frontend/index.html`
- Create: `frontend/src/main.ts`
- Create: `frontend/src/App.vue`

- [ ] **Step 1: 创建 package.json**

```json
{
  "name": "ai-agent-frontend",
  "version": "0.0.1",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc && vite build",
    "preview": "vite preview",
    "test": "vitest"
  },
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.2.5",
    "pinia": "^2.1.7",
    "element-plus": "^2.5.0",
    "axios": "^1.6.2"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "typescript": "^5.3.0",
    "vite": "^5.0.0",
    "vue-tsc": "^1.8.0",
    "vitest": "^1.1.0"
  }
}
```

- [ ] **Step 2: 创建 vite.config.ts**

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

- [ ] **Step 3: 创建 tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "preserve",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true
  },
  "include": ["src/**/*.ts", "src/**/*.tsx", "src/**/*.vue"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

- [ ] **Step 4: 创建 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI Agent</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.ts"></script>
  </body>
</html>
```

- [ ] **Step 5: 创建 main.ts**

```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(ElementPlus)
app.mount('#app')
```

- [ ] **Step 6: 创建 App.vue**

```vue
<template>
  <div id="app">
    <el-container>
      <el-header>
        <h1>AI Agent</h1>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
</script>

<style>
#app {
  height: 100vh;
}
.el-header {
  background: #409EFF;
  color: white;
  display: flex;
  align-items: center;
}
.el-header h1 {
  margin: 0;
  font-size: 1.5rem;
}
</style>
```

- [ ] **Step 7: 安装依赖并验证**

```bash
cd frontend
npm install
npm run build
```
Expected: BUILD SUCCESS

- [ ] **Step 8: 提交**

```bash
git add frontend/
git commit -m "feat: initialize Vue 3 frontend project"
```

---

## Chunk 2: Docker Compose 配置

### Task 3: 创建 Docker Compose 基础设施

**Files:**
- Create: `docker-compose.yml`
- Create: `docker/qdrant/Dockerfile`
- Create: `docker/python-sidecar/Dockerfile`
- Create: `docker/python-sidecar/requirements.txt`
- Create: `docker/python-sidecar/embedding_service.py`

- [ ] **Step 1: 创建 docker-compose.yml**

```yaml
version: '3.8'

services:
  # API Gateway / Backend
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - QDRANT_HOST=qdrant
      - QDRANT_PORT=6334
      - MEMORY_PATH=/app/data/memory.db
    volumes:
      - ./data:/app/data
    depends_on:
      - qdrant
      - python-sidecar
    networks:
      - ai-agent-network

  # Frontend
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    ports:
      - "3000:80"
    depends_on:
      - backend
    networks:
      - ai-agent-network

  # Qdrant Vector Database
  qdrant:
    image: qdrant/qdrant:latest
    ports:
      - "6334:6334"
      - "6333:6333"
    volumes:
      - ./data/qdrant:/qdrant/storage
    networks:
      - ai-agent-network

  # Python Sidecar for Embeddings
  python-sidecar:
    build:
      context: ./docker/python-sidecar
      dockerfile: Dockerfile
    ports:
      - "5000:5000"
    volumes:
      - ./models:/app/models
    networks:
      - ai-agent-network

  # MCP Server - Filesystem
  mcp-filesystem:
    build:
      context: ./mcp-servers/filesystem
      dockerfile: Dockerfile
    ports:
      - "5001:5000"
    volumes:
      - ./data/files:/data
    networks:
      - ai-agent-network

  # MCP Server - Shell
  mcp-shell:
    build:
      context: ./mcp-servers/shell
      dockerfile: Dockerfile
    ports:
      - "5002:5000"
    networks:
      - ai-agent-network

networks:
  ai-agent-network:
    driver: bridge

volumes:
  data:
  models:
```

- [ ] **Step 2: 创建后端 Dockerfile**

```dockerfile
# backend/Dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 3: 创建前端 Dockerfile**

```dockerfile
# frontend/Dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

- [ ] **Step 4: 创建 Python Sidecar Dockerfile**

```dockerfile
# docker/python-sidecar/Dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY embedding_service.py .
EXPOSE 5000
CMD ["python", "embedding_service.py"]
```

- [ ] **Step 5: 创建 Python Sidecar requirements.txt**

```
fastapi==0.109.0
uvicorn==0.27.0
sentence-transformers==2.2.2
torch==2.1.0
pydantic==2.5.0
```

- [ ] **Step 6: 创建 Python Sidecar 嵌入服务**

```python
# docker/python-sidecar/embedding_service.py
from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
from typing import List

app = FastAPI()

# Load bge-m3 model
model = SentenceTransformer('BAAI/bge-m3', trust_remote_code=True)

class EmbeddingRequest(BaseModel):
    texts: List[str]

class EmbeddingResponse(BaseModel):
    embeddings: List[List[float]]

@app.post("/embed")
async def embed(request: EmbeddingRequest) -> EmbeddingResponse:
    embeddings = model.encode(request.texts, normalize_embeddings=True)
    return EmbeddingResponse(embeddings=embeddings.tolist())

@app.get("/health")
async def health():
    return {"status": "healthy"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)
```

- [ ] **Step 7: 提交**

```bash
git add docker-compose.yml docker/ backend/Dockerfile frontend/Dockerfile frontend/nginx.conf
git commit -m "feat: add Docker Compose infrastructure"
```

---

## Chunk 3: API Gateway 基础端点

### Task 4: 创建健康检查和基础 API

**Files:**
- Create: `backend/src/main/java/com/aiagent/controller/HealthController.java`
- Create: `backend/src/main/java/com/aiagent/controller/ChatController.java`
- Create: `backend/src/main/java/com/aiagent/dto/ChatRequest.java`
- Create: `backend/src/main/java/com/aiagent/dto/ChatResponse.java`
- Test: `backend/src/test/java/com/aiagent/controller/HealthControllerTests.java`

- [ ] **Step 1: 创建健康检查 Controller**

```java
package com.aiagent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "ai-agent");
        return response;
    }
}
```

- [ ] **Step 2: 创建 ChatRequest DTO**

```java
package com.aiagent.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private String sessionId;
}
```

- [ ] **Step 3: 创建 ChatResponse DTO**

```java
package com.aiagent.dto;

import lombok.Data;

@Data
public class ChatResponse {
    private String message;
    private String sessionId;
    private String model;
}
```

- [ ] **Step 4: 创建 Chat Controller (占位实现)**

```java
package com.aiagent.controller;

import com.aiagent.dto.ChatRequest;
import com.aiagent.dto.ChatResponse;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ChatController {

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        ChatResponse response = new ChatResponse();
        response.setMessage("Echo: " + request.getMessage());
        response.setSessionId(request.getSessionId() != null
            ? request.getSessionId()
            : UUID.randomUUID().toString());
        response.setModel("placeholder");
        return response;
    }
}
```

- [ ] **Step 5: 创建健康检查测试**

```java
package com.aiagent.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
class HealthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpoint_returnsHealthyStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("healthy"))
            .andExpect(jsonPath("$.service").value("ai-agent"));
    }
}
```

- [ ] **Step 6: 运行测试**

```bash
cd backend
mvn test -Dtest=HealthControllerTests
```
Expected: PASS

- [ ] **Step 7: 提交**

```bash
git add backend/src/main/java/com/aiagent/controller/ backend/src/main/java/com/aiagent/dto/ backend/src/test/java/com/aiagent/controller/
git commit -m "feat: add health check and placeholder chat endpoint"
```

---

## Chunk 4: 技能系统基础

### Task 5: 创建 Skills 系统核心结构

**Files:**
- Create: `backend/src/main/java/com/aiagent/skill/Skill.java`
- Create: `backend/src/main/java/com/aiagent/skill/SkillDefinition.java`
- Create: `backend/src/main/java/com/aiagent/skill/SkillExecutor.java`
- Create: `backend/src/main/java/com/aiagent/skill/SkillParser.java`
- Create: `backend/src/main/resources/skills/debug.md`
- Test: `backend/src/test/java/com/aiagent/skill/SkillParserTests.java`

- [ ] **Step 1: 创建 Skill 定义类**

```java
package com.aiagent.skill;

import lombok.Data;
import java.util.List;

@Data
public class SkillDefinition {
    private String name;
    private List<String> triggers;
    private List<String> parameters;
    private List<String> steps;
    private boolean requiresConfirmation;
    private String description;
}
```

- [ ] **Step 2: 创建 Skill 接口**

```java
package com.aiagent.skill;

import java.util.Map;

public interface Skill {
    SkillDefinition getDefinition();
    SkillResult execute(Map<String, Object> context);
}

@Data
class SkillResult {
    private boolean success;
    private String output;
    private Map<String, Object> data;
}
```

- [ ] **Step 3: 创建 Skill 解析器**

```java
package com.aiagent.skill;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SkillParser {

    public SkillDefinition parse(Path markdownFile) throws IOException {
        String content = Files.readString(markdownFile);
        SkillDefinition definition = new SkillDefinition();

        // Parse name
        Matcher nameMatcher = Pattern.compile("^#\\s+Skill:\\s*(.+)$", Pattern.MULTILINE)
            .matcher(content);
        if (nameMatcher.find()) {
            definition.setName(nameMatcher.group(1).trim());
        }

        // Parse triggers
        List<String> triggers = new ArrayList<>();
        Matcher triggerMatcher = Pattern.compile("##\\s+触发词\\s*-\\s*(.+)", Pattern.MULTILINE)
            .matcher(content);
        while (triggerMatcher.find()) {
            triggers.add(triggerMatcher.group(1).trim());
        }
        definition.setTriggers(triggers);

        // Parse parameters
        List<String> parameters = new ArrayList<>();
        Matcher paramMatcher = Pattern.compile("##\\s+参数\\s*-\\s*(.+):", Pattern.MULTILINE)
            .matcher(content);
        while (paramMatcher.find()) {
            parameters.add(paramMatcher.group(1).trim());
        }
        definition.setParameters(parameters);

        // Parse confirmation requirement
        definition.setRequiresConfirmation(content.contains("需要确认：是"));

        return definition;
    }
}
```

- [ ] **Step 4: 创建示例 Skill - debug.md**

```markdown
# Skill: debug

## 描述
调试代码问题，分析错误并提供修复建议

## 触发词
- /debug
- 帮我调试
- 查找问题

## 参数
- file: 目标文件路径
- error: 错误信息

## 执行步骤
1. 读取错误日志
2. 定位问题代码
3. 分析可能原因
4. 提供修复建议

## 权限
- 需要确认：否
```

- [ ] **Step 5: 创建解析器测试**

```java
package com.aiagent.skill;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SkillParserTests {

    @Autowired
    private SkillParser skillParser;

    @Test
    void parseSkillDefinition_parsesMarkdownFile() throws Exception {
        Path skillFile = Paths.get("src/main/resources/skills/debug.md");
        SkillDefinition definition = skillParser.parse(skillFile);

        assertEquals("debug", definition.getName());
        assertFalse(definition.getTriggers().isEmpty());
        assertFalse(definition.getParameters().isEmpty());
        assertFalse(definition.isRequiresConfirmation());
    }
}
```

- [ ] **Step 6: 运行测试**

```bash
cd backend
mvn test -Dtest=SkillParserTests
```
Expected: PASS

- [ ] **Step 7: 提交**

```bash
git add backend/src/main/java/com/aiagent/skill/ backend/src/main/resources/skills/ backend/src/test/java/com/aiagent/skill/
git commit -m "feat: implement basic Skill parsing system"
```

---

## 测试策略

基础架构的测试覆盖：
- 单元测试：每个 Controller、Service、Parser
- 集成测试：Docker Compose 启动后验证服务间通信
- 端到端测试：前端调用 `/api/health` 验证全流程

---

## 前置依赖

在开始本计划前，需要：
- Java 17 已安装
- Node.js 20 已安装
- Docker Desktop 已安装并运行
- Maven 3.9+ 已安装

---

## 验收标准

1. `docker-compose up -d` 启动所有服务
2. 访问 `http://localhost:8080/api/health` 返回健康状态
3. 访问 `http://localhost:3000` 显示前端界面
4. Qdrant 在 `http://localhost:6333` 可访问
5. Python Sidecar 在 `http://localhost:5000/health` 返回健康状态
