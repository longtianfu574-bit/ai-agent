# Ollama 本地嵌入模型配置指南

**日期**: 2026-03-13
**版本**: 1.0

---

## 1. 安装 Ollama

### Windows 安装

1. 访问 https://ollama.ai/ 下载 Windows 安装包
2. 运行安装程序，按提示完成安装
3. 验证安装：

```bash
ollama --version
```

### 手动下载（如安装包不可用）

```bash
# 下载 Ollama Windows 版
curl -L https://ollama.com/download/ollama-windows-amd64.exe -o ollama.exe

# 或使用 PowerShell
Invoke-WebRequest https://ollama.com/download/ollama-windows-amd64.exe -OutFile ollama.exe
```

---

## 2. 拉取嵌入模型

### 推荐模型：bge-m3

```bash
# 拉取 bge-m3 嵌入模型（推荐）
ollama pull bge-m3

# 验证模型
ollama list

# 输出应包含：
# NAME      ID              SIZE      MODIFIED
# bge-m3    xxxxxxx         ~1.1GB    2 days ago
```

### 备选模型

| 模型 | 大小 | 语言 | 说明 |
|------|------|------|------|
| **bge-m3** | ~1.1GB | 多语言 | 推荐，支持中文 |
| **nomic-embed-text** | ~270MB | 英文为主 | 轻量级 |
| **mxbai-embed-large** | ~1.2GB | 多语言 | 高质量 |

---

## 3. 测试嵌入模型

```bash
# 测试嵌入 API
curl http://localhost:11434/api/embeddings \
  -H "Content-Type: application/json" \
  -d '{
    "model": "bge-m3",
    "prompt": "你好，世界"
  }'
```

预期响应：
```json
{
  "embedding": [0.123, -0.456, 0.789, ...]
}
```

---

## 4. 配置项目

### 更新 `.env` 文件

```bash
# 嵌入模型配置 - Ollama
EMBEDDING_PROVIDER=ollama
EMBEDDING_BASE_URL=http://localhost:11434
EMBEDDING_MODEL=bge-m3
```

### Spring Boot 配置

在 `backend/src/main/resources/application.yml` 中添加：

```yaml
# Ollama 配置
ollama:
  base-url: http://localhost:11434
  embedding-model: bge-m3
  chat-model: qwen-3.5

# LLM 配置 - 千问 3.5
llm:
  base-url: http://123.57.224.128:58080/v1
  api-key: sk-a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0
  model: Qwen3.5-4B-Q4_K_M.gguf
```

---

## 5. Java 集成代码

### 创建 Ollama 嵌入客户端

```java
// backend/src/main/java/com/aiagent/embedding/OllamaEmbeddingClient.java
package com.aiagent.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class OllamaEmbeddingClient implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingClient.class);

    private final WebClient webClient;
    private final String embeddingModel;

    public OllamaEmbeddingClient(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.embedding-model:bge-m3}") String embeddingModel) {

        this.embeddingModel = embeddingModel;
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    @Override
    public CompletableFuture<List<List<Float>>> embed(List<String> texts) {
        if (texts.size() == 1) {
            return embedSingle(texts.get(0))
                .thenApply(List::of);
        }

        // Batch embedding
        List<CompletableFuture<List<Float>>> futures = texts.stream()
            .map(this::embedSingle)
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<List<Float>> embedSingle(String text) {
        log.debug("Requesting embedding for text: {}", text.substring(0, Math.min(50, text.length())));

        Map<String, Object> request = Map.of(
            "model", embeddingModel,
            "prompt", text
        );

        return webClient.post()
            .uri("/api/embeddings")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OllamaEmbeddingResponse.class)
            .map(OllamaEmbeddingResponse::embedding)
            .toFuture();
    }

    private record OllamaEmbeddingResponse(List<Float> embedding) {}
}
```

---

## 6. 启动服务

### 启动 Ollama 服务

Ollama 安装后会自动运行后台服务。验证服务状态：

```bash
# 检查服务状态
curl http://localhost:11434/api/version

# 预期输出：
# {"version":"0.x.x"}
```

### 启动项目

```bash
# 确保 Ollama 运行中
ollama list

# 启动后端
cd backend
mvn spring-boot:run

# 启动前端
cd frontend
npm run dev
```

---

## 7. 故障排查

### 问题：连接被拒绝

```
Connection refused: localhost:11434
```

**解决**：
1. 确保 Ollama 已安装并运行
2. Windows 服务检查：任务管理器 → Ollama App
3. 手动启动：`ollama serve`

### 问题：模型不存在

```
model 'bge-m3' not found
```

**解决**：
```bash
ollama pull bge-m3
```

### 问题：嵌入请求超时

**解决**：
1. 首次请求需要加载模型，可能需要 10-30 秒
2. 增加超时配置：
```yaml
spring:
  webflux:
    client:
      timeout: 60000
```

---

## 8. 性能优化建议

| 优化项 | 说明 |
|--------|------|
| **模型缓存** | Ollama 会自动缓存已加载模型 |
| **批量嵌入** | 多个文本一起发送减少开销 |
| **GPU 加速** | 如有 NVIDIA 显卡，Ollama 会自动使用 |
| **量化模型** | 使用 `bge-m3:q4_K_M` 等量化版本 |

---

## 9. 完整配置检查清单

- [ ] Ollama 已安装
- [ ] `ollama --version` 输出版本号
- [ ] 已拉取 `bge-m3` 模型
- [ ] `ollama list` 显示模型
- [ ] Ollama 服务运行中
- [ ] `curl http://localhost:11434/api/version` 返回版本
- [ ] `.env` 配置 EMBEDDING_BASE_URL=http://localhost:11434
- [ ] 测试嵌入 API 成功

---

## 10. 下一步

配置完成后，继续实施：

1. 更新基础架构计划中的嵌入客户端代码
2. 创建 `OllamaEmbeddingClient` 类
3. 测试完整的 RAG 检索流程
