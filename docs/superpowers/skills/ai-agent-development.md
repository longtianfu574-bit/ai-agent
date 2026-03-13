# AI Agent 开发 Skills

**日期**: 2026-03-13
**版本**: 1.0
**用途**: 指导 AI Agent 项目的日常开发工作

---

## 如何使用本 Skills

本 Skills 文档定义了 AI Agent 项目开发的标准流程和命令。每天开始开发时：

1. 查看 **今日任务清单**
2. 执行对应的 **Skill 命令**
3. 完成任务后打勾确认

---

## Skill 列表

### Skill 1: 项目启动

**触发词**: `/start`, `开始开发`, `启动项目`

**用途**: 启动所有开发环境和服务

**执行步骤**:

```bash
# 1. 进入项目目录
cd "e:\works\ai agent"

# 2. 激活 Python 虚拟环境
source .venv/Scripts/activate  # Git Bash
# 或 .venv\Scripts\activate    # CMD

# 3. 启动 Docker 服务
docker-compose up -d

# 4. 验证服务状态
docker-compose ps

# 5. 检查健康状态
curl http://localhost:8080/api/health
curl http://localhost:5000/health
```

**预期输出**:
- 所有 Docker 容器状态为 `Up`
- 后端 API 返回 `{"status": "healthy"}`
- Python Sidecar 返回健康状态

---

### Skill 2: 后端开发

**触发词**: `/backend`, `后端开发`, `Java 开发`

**用途**: 运行和调试 Spring Boot 后端

**执行步骤**:

```bash
# 1. 进入后端目录
cd backend

# 2. 运行 Spring Boot (开发模式)
mvn spring-boot:run

# 或运行测试
mvn test

# 或打包
mvn clean package -DskipTests
```

**常用命令**:

| 命令 | 用途 |
|------|------|
| `mvn spring-boot:run` | 启动后端服务 |
| `mvn test` | 运行所有测试 |
| `mvn test -Dtest=ClassName` | 运行单个测试类 |
| `mvn clean install` | 清理并重新构建 |
| `mvn dependency:tree` | 查看依赖树 |

**端口**: `8080`

**配置文件**: `backend/src/main/resources/application.yml`

---

### Skill 3: 前端开发

**触发词**: `/frontend`, `前端开发`, `Vue 开发`

**用途**: 运行和调试 Vue 3 前端

**执行步骤**:

```bash
# 1. 进入前端目录
cd frontend

# 2. 安装依赖 (首次运行)
npm install

# 3. 启动开发服务器
npm run dev

# 4. 构建生产版本
npm run build

# 5. 运行测试
npm run test
```

**常用命令**:

| 命令 | 用途 |
|------|------|
| `npm run dev` | 启动开发服务器 |
| `npm run build` | 生产构建 |
| `npm run test` | 运行测试 |
| `npm run lint` | 代码检查 |

**端口**: `3000`

**访问地址**: http://localhost:3000

---

### Skill 4: RAG 服务开发

**触发词**: `/rag`, `RAG 开发`, `检索服务`

**用途**: 开发和测试 RAG 检索功能

**执行步骤**:

```bash
# 1. 启动 Qdrant
docker-compose up -d qdrant

# 2. 访问 Qdrant Dashboard
# http://localhost:6333/dashboard

# 3. 测试嵌入服务
curl http://localhost:5000/embed \
  -H "Content-Type: application/json" \
  -d '{"input": "Hello world"}'

# 4. 创建索引集合
curl -X PUT http://localhost:6334/collections/ai-agent-rag \
  -H "Content-Type: application/json" \
  -d '{
    "vectors": {
      "size": 768,
      "distance": "Cosine"
    }
  }'
```

**相关服务**:
- Qdrant: `http://localhost:6334` (gRPC), `http://localhost:6333` (REST)
- Embedding: `http://localhost:58080/embedding`

---

### Skill 5: MCP 服务开发

**触发词**: `/mcp`, `MCP 开发`, `工具服务`

**用途**: 开发和管理 MCP Servers

**可用 Servers**:

| Server | 端口 | 功能 |
|--------|------|------|
| filesystem | 5001 | 文件读写 |
| shell | 5002 | 命令执行 |
| database | 5003 | SQL 查询 |
| http | 5004 | API 调用 |
| git | 5005 | Git 操作 |

**执行步骤**:

```bash
# 1. 进入 MCP 服务目录
cd mcp-servers/filesystem

# 2. 安装依赖
pip install -r requirements.txt

# 3. 启动服务
uvicorn main:app --reload --port 5001

# 4. 测试端点
curl http://localhost:5001/health
```

**开发新 MCP Server**:

```bash
# 创建新服务
mkdir -p mcp-servers/<name>
cd mcp-servers/<name>

# 创建基础文件
touch main.py requirements.txt Dockerfile
```

---

### Skill 6: 技能系统开发

**触发词**: `/skills`, `技能开发`, `Skill 系统`

**用途**: 创建和管理 AI Skills

**Skill 定义格式** (Markdown):

```markdown
# Skill: <skill-name>

## 描述
<技能功能描述>

## 触发词
- /<command>
- <自然语言触发 1>
- <自然语言触发 2>

## 参数
- <param1>: <描述>
- <param2>: <描述>

## 执行步骤
1. <步骤 1>
2. <步骤 2>
3. <步骤 3>

## 权限
- 需要确认：是/否
```

**执行步骤**:

```bash
# 1. 创建新的 Skill 定义
# 在 backend/src/main/resources/skills/ 创建 <name>.md

# 2. 验证 Skill 解析
curl http://localhost:8080/api/skills

# 3. 执行 Skill
curl -X POST http://localhost:8080/api/skills/<name>/execute \
  -H "Content-Type: application/json" \
  -d '{"parameters": {...}}'
```

---

### Skill 7: Memory 系统开发

**触发词**: `/memory`, `记忆开发`, `持久化`

**用途**: 开发和管理 Memory 系统

**执行步骤**:

```bash
# 1. 查看 Memory 数据库
sqlite3 data/memory.db

# 2. 查看表结构
.schema

# 3. 查询用户偏好
SELECT * FROM user_preferences;

# 4. 测试语义检索
curl -X POST http://localhost:8080/api/memory/search \
  -H "Content-Type: application/json" \
  -d '{"query": "用户偏好"}'
```

**数据库位置**: `data/memory.db`

---

### Skill 8: 测试运行

**触发词**: `/test`, `运行测试`, `测试`

**用途**: 运行所有测试

**执行步骤**:

```bash
# 1. 后端测试
cd backend
mvn test

# 2. 前端测试
cd frontend
npm run test

# 3. 集成测试
docker-compose up -d
# 等待服务启动后
./scripts/run-integration-tests.sh

# 4. 查看测试报告
# 后端：backend/target/surefire-reports/
# 前端：frontend/coverage/
```

---

### Skill 9: Docker 管理

**触发词**: `/docker`, `容器管理`, `Docker`

**用途**: 管理 Docker 容器

**执行步骤**:

```bash
# 启动所有服务
docker-compose up -d

# 停止所有服务
docker-compose down

# 查看日志
docker-compose logs -f <service-name>

# 重启单个服务
docker-compose restart <service-name>

# 重建容器
docker-compose up -d --build

# 查看资源使用
docker stats

# 清理无用资源
docker system prune -a
```

**服务列表**:
- `backend` - Java 后端
- `frontend` - Vue 前端
- `qdrant` - 向量数据库
- `python-sidecar` - 嵌入服务
- `mcp-filesystem` - 文件 MCP
- `mcp-shell` - Shell MCP

---

### Skill 10: 代码提交

**触发词**: `/commit`, `提交代码`, `Git 提交`

**用途**: 提交代码到 Git

**执行步骤**:

```bash
# 1. 查看状态
git status

# 2. 查看变更
git diff

# 3. 添加文件
git add <files>

# 4. 提交
git commit -m "<type>: <description>"

# 5. 推送
git push origin main
```

**提交类型**:
- `feat:` 新功能
- `fix:` Bug 修复
- `docs:` 文档更新
- `chore:` 构建/工具配置
- `refactor:` 代码重构
- `test:` 测试相关

---

### Skill 11: 调试模式

**触发词**: `/debug`, `调试`, `排查问题`

**用途**: 调试和排查问题

**执行步骤**:

```bash
# 1. 查看后端日志
docker-compose logs backend

# 2. 查看前端网络请求
# 浏览器 DevTools → Network

# 3. 测试 API 端点
curl http://localhost:8080/api/health
curl http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "test"}'

# 4. 检查数据库
sqlite3 data/memory.db "SELECT * FROM user_preferences;"

# 5. 检查 Qdrant 集合
curl http://localhost:6334/collections

# 6. 检查嵌入服务
curl http://localhost:58080/embedding \
  -H "Content-Type: application/json" \
  -d '{"input": "test"}'
```

---

### Skill 12: 环境重置

**触发词**: `/reset`, `重置环境`, `清理`

**用途**: 重置开发环境

**执行步骤**:

```bash
# 警告：以下操作会删除数据！

# 1. 停止所有服务
docker-compose down

# 2. 清理数据 (谨慎！)
rm -rf data/qdrant/*
rm -rf data/memory.db

# 3. 清理构建缓存
cd backend && mvn clean
cd frontend && rm -rf node_modules dist

# 4. 重新初始化
docker-compose up -d

# 5. 验证
docker-compose ps
curl http://localhost:8080/api/health
```

---

## 今日任务清单模板

```markdown
### 日期：__________

**今日目标**:

- [ ]
- [ ]
- [ ]

**Skills 使用记录**:

| 时间 | Skill | 说明 | 状态 |
|------|-------|------|------|
|      |       |      |      |
|      |       |      |      |

**问题记录**:


**明日计划**:


```

---

## 快速参考

### 端口一览

| 服务 | 端口 | 地址 |
|------|------|------|
| 后端 API | 8080 | http://localhost:8080 |
| 前端界面 | 3000 | http://localhost:3000 |
| Qdrant REST | 6333 | http://localhost:6333 |
| Qdrant gRPC | 6334 | localhost:6334 |
| Embedding | 58080 | http://localhost:58080/embedding |
| MCP Filesystem | 5001 | http://localhost:5001 |
| MCP Shell | 5002 | http://localhost:5002 |
| MCP Database | 5003 | http://localhost:5003 |
| MCP HTTP | 5004 | http://localhost:5004 |
| MCP Git | 5005 | http://localhost:5005 |

### 配置文件位置

| 文件 | 路径 |
|------|------|
| 后端配置 | `backend/src/main/resources/application.yml` |
| 前端配置 | `frontend/vite.config.ts` |
| 环境变量 | `.env` |
| Docker 编排 | `docker-compose.yml` |

### 文档位置

| 文档 | 路径 |
|------|------|
| 架构设计 | `docs/superpowers/specs/2026-03-13-ai-agent-architecture-design.md` |
| 实施计划 | `docs/superpowers/plans/` |
| 准备指南 | `docs/superpowers/guides/2026-03-13-ai-agent-preparation-guide.md` |
| Ollama 配置 | `docs/superpowers/guides/ollama-embedding-setup.md` |

---

## 附录：常见开发场景

### 场景 1: 添加新的 API 端点

```bash
# 1. 创建 Controller
# backend/src/main/java/com/aiagent/controller/<Name>Controller.java

# 2. 创建 DTO (如需要)
# backend/src/main/java/com/aiagent/dto/

# 3. 编写测试
# backend/src/test/java/com/aiagent/controller/

# 4. 运行测试
mvn test -Dtest=<Name>ControllerTests

# 5. 验证端点
curl http://localhost:8080/api/<endpoint>
```

### 场景 2: 创建新的 Vue 组件

```bash
# 1. 创建组件文件
# frontend/src/components/<Name>.vue

# 2. 添加到路由
# frontend/src/router/index.ts

# 3. 创建 API 调用
# frontend/src/api/<name>.ts

# 4. 测试
npm run dev
```

### 场景 3: 调试 RAG 检索问题

```bash
# 1. 检查 Embedding 服务
curl http://localhost:58080/embedding \
  -H "Content-Type: application/json" \
  -d '{"input": "test query"}'

# 2. 检查 Qdrant 集合
curl http://localhost:6334/collections

# 3. 执行相似度搜索
curl http://localhost:6334/collections/ai-agent-rag/points/search \
  -H "Content-Type: application/json" \
  -d '{
    "vector": [...],
    "limit": 5
  }'

# 4. 查看后端日志
docker-compose logs backend | grep -i rag
```

---

**Skills 版本**: 1.0
**最后更新**: 2026-03-13
**维护者**: AI Agent Team
