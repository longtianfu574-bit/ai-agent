# AI Agent

基于 Spring Boot 3 + Vue 3 的智能对话助手，集成 MCP (Model Context Protocol)、RAG (检索增强生成) 和本地记忆系统。

## 系统架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              Frontend (Vue 3)                            │
│                         http://localhost:3000                            │
│  ┌─────────────┬─────────────┬─────────────┬─────────────┬──────────┐  │
│  │ 智能对话    │ 技能管理    │ 文档管理    │ 记忆管理    │ 系统设置 │  │
│  └─────────────┴─────────────┴─────────────┴─────────────┴──────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ REST API
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Backend (Spring Boot 3)                          │
│                        http://localhost:8080                             │
│  ┌─────────────┬─────────────┬─────────────┬─────────────────────────┐  │
│  │   Controller│   RAG Serv. │   MCP Serv. │    Memory / LLM         │  │
│  └─────────────┴─────────────┴─────────────┴─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
          ┌─────────────────────────┼─────────────────────────┐
          ▼                         ▼                         ▼
┌──────────────────┐    ┌──────────────────┐    ┌──────────────────────────┐
│  MCP Servers     │    │   Qdrant DB      │    │   LLM (Qwen-3.5)         │
│  - Filesystem    │    │  :6334           │    │   Embedding Service      │
│  - Shell         │    │  Vectors         │    │   :58080                 │
│  - Database      │    │                  │    │                          │
│  - HTTP          │    │                  │    │                          │
│  - Git           │    │                  │    │                          │
│  :5001-5005      │    │                  │    │                          │
└──────────────────┘    └──────────────────┘    └──────────────────────────┘
```

## 核心特性

- **智能对话** - 基于 Qwen-3.5 大模型的自然语言对话能力
- **MCP 工具调用** - 5 个 MCP 微服务支持文件操作、Shell 命令、数据库查询、HTTP 请求、Git 操作
- **RAG 检索增强** - 基于 Qdrant 向量数据库的文档检索和知识增强
- **本地记忆** - SQLite 存储用户偏好和对话历史
- **现代化 UI** - Vue 3 + Element Plus 构建的响应式界面

## 技术栈

### 后端
| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 3.2.0 | Web 框架 |
| Qdrant Client | 1.9.0 | 向量数据库客户端 |
| SQLite | 3.44.1 | 本地记忆存储 |
| gRPC | 1.65.1 | 高性能 RPC 通信 |

### 前端
| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5.0 | 渐进式框架 |
| TypeScript | 5.5.0 | 类型安全 |
| Element Plus | 2.6.0 | UI 组件库 |
| Pinia | 2.2.0 | 状态管理 |
| Vue Router | 4.3.0 | 路由管理 |
| Axios | 1.7.0 | HTTP 客户端 |

### MCP 服务
| 服务 | 端口 | 技术栈 |
|------|------|--------|
| Filesystem | 5001 | Python FastAPI |
| Shell | 5002 | Python FastAPI |
| Database | 5003 | Python FastAPI |
| HTTP | 5004 | Python FastAPI |
| Git | 5005 | Python FastAPI |

## 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- Python 3.10+ with uvicorn
- Docker & Docker Compose (可选，用于容器化部署)

### 1. 启动 MCP 服务

```bash
# 方式一：单独启动每个服务
cd mcp-servers/filesystem
uvicorn main:app --host 0.0.0.0 --port 5001

cd ../shell
uvicorn main:app --host 0.0.0.0 --port 5002

cd ../database
uvicorn main:app --host 0.0.0.0 --port 5003

cd ../http
uvicorn main:app --host 0.0.0.0 --port 5004

cd ../git
uvicorn main:app --host 0.0.0.0 --port 5005
```

### 2. 启动后端服务

```bash
cd backend

# 设置环境变量（可选）
export LLM_API_KEY=your-api-key
export QDRANT_HOST=localhost

# 启动 Spring Boot
mvn spring-boot:run
```

### 3. 启动前端

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

访问 http://localhost:3000 即可使用应用。

## 项目结构

```
ai-agent/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/
│   │   └── com/aiagent/
│   │       ├── controller/     # REST 控制器
│   │       ├── mcp/            # MCP 客户端和服务
│   │       ├── rag/            # RAG 服务
│   │       ├── llm/            # LLM 集成
│   │       └── embedding/      # Embedding 服务
│   ├── src/main/resources/
│   │   ├── application.yml     # 主配置
│   │   └── application-rag.yml # RAG 配置
│   └── pom.xml
│
├── frontend/                   # Vue 3 前端
│   ├── src/
│   │   ├── components/         # 可复用组件
│   │   ├── views/              # 页面视图
│   │   ├── stores/             # Pinia 状态管理
│   │   ├── router/             # 路由配置
│   │   ├── api/                # API 调用
│   │   ├── main.ts             # 入口文件
│   │   └── App.vue             # 根组件
│   ├── package.json
│   └── vite.config.ts
│
├── mcp-servers/                # MCP 微服务
│   ├── filesystem/             # 文件操作服务
│   ├── shell/                  # Shell 命令服务
│   ├── database/               # 数据库查询服务
│   ├── http/                   # HTTP 请求服务
│   └── git/                    # Git 操作服务
│
├── docs/                       # 项目文档
└── docker-compose.yml          # Docker 编排配置
```

## MCP 工具列表

### Filesystem Server (:5001)
| 工具 | 描述 | 参数 |
|------|------|------|
| `read_file` | 读取文件 | `path` |
| `write_file` | 写入文件 | `path`, `content` |
| `list_directory` | 列出目录 | `path` |
| `delete_file` | 删除文件 | `path` |

### Shell Server (:5002)
| 工具 | 描述 | 参数 |
|------|------|------|
| `execute` | 执行命令 | `command`, `timeout` |
| `execute_safe` | 执行安全命令 | `command_name`, `args` |

### Database Server (:5003)
| 工具 | 描述 | 参数 |
|------|------|------|
| `query` | SQL 查询 | `sql`, `params` |
| `execute` | SQL 执行 | `sql`, `params` |

### HTTP Server (:5004)
| 工具 | 描述 | 参数 |
|------|------|------|
| `request` | HTTP 请求 | `url`, `method`, `headers`, `body` |
| `get` | HTTP GET | `url`, `headers` |
| `post` | HTTP POST | `url`, `body`, `headers` |

### Git Server (:5005)
| 工具 | 描述 | 参数 |
|------|------|------|
| `clone` | 克隆仓库 | `url`, `path` |
| `init` | 初始化仓库 | `path` |
| `status` | 仓库状态 | `path` |
| `commit` | 提交更改 | `path`, `message` |
| `log` | 查看日志 | `path`, `count` |

## API 接口

### 对话接口

```bash
# 基础对话 (RAG 模式)
POST http://localhost:8080/api/chat
Content-Type: application/json

{
    "message": "项目文档在哪里？",
    "sessionId": "optional-id"
}

# 带工具调用的对话
POST http://localhost:8080/api/chat/tools
Content-Type: application/json

{
    "message": "帮我查看 /home 目录下有哪些文件",
    "sessionId": "optional-id"
}
```

### MCP 直接调用

```bash
# 列出所有工具
GET http://localhost:8080/api/mcp/tools

# 调用工具
POST http://localhost:8080/api/mcp/tools/call
Content-Type: application/json

{
    "server": "filesystem",
    "tool": "read_file",
    "args": { "path": "config.txt" }
}

# 快捷方式
GET http://localhost:8080/api/mcp/filesystem/list?path=/home
POST http://localhost:8080/api/mcp/shell/execute?command=ls%20-la
GET http://localhost:8080/api/mcp/database/query?sql=SELECT%20*%20FROM%20users
```

详细 API 文档请参考 [docs/mcp-api.md](docs/mcp-api.md)

## 配置说明

### 后端配置 (application.yml)

```yaml
# 服务器配置
server:
  port: 8080

# LLM 配置
llm:
  base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
  api-key: ${LLM_API_KEY}
  model: qwen-plus

# Qdrant 向量数据库
qdrant:
  host: ${QDRANT_HOST:localhost}
  port: 6334
  collection: ai-agent-rag

# Embedding 服务
embedding:
  service-url: http://localhost:58080/v1
  model: Qwen3-Embedding

# MCP 服务地址
mcp:
  servers:
    filesystem: http://localhost:5001
    shell: http://localhost:5002
    database: http://localhost:5003
    http: http://localhost:5004
    git: http://localhost:5005
```

### 前端代理配置 (vite.config.ts)

```typescript
server: {
  port: 3000,
  host: '0.0.0.0',
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

## 界面功能

### 1. 智能对话 (/chat)
- 实时对话流式响应
- 支持建议标签快速提问
- 对话历史导出
- 打字动画加载指示器

### 2. 技能管理 (/skills)
- MCP 工具能力展示
- 工具调用状态监控

### 3. 文档管理 (/documents)
- RAG 知识库管理
- 文档向量化处理

### 4. 记忆管理 (/memory)
- 用户偏好记忆
- 对话摘要存储
- 项目知识库

### 5. 系统设置 (/settings)
- LLM 服务配置
- MCP 服务配置
- 向量数据库配置

## 使用示例

### 示例 1：文件操作
```
用户：帮我查看 /data 目录下有什么文件
AI：在 /data 目录下有以下文件：config.json, logs/, backup.tar.gz
```

### 示例 2：Shell 命令
```
用户：运行 ls -la 命令
AI：命令执行结果：
total 32
drwxr-xr-x  4 user  staff  128 Mar 15 10:00 .
drwxr-xr-x  6 user  staff  192 Mar 15 09:58 ..
```

### 示例 3：数据库查询
```
用户：查询所有用户数据
AI：查询到以下用户：
1. admin (admin@example.com)
2. user1 (user1@example.com)
```

### 示例 4：HTTP 请求
```
用户：获取 GitHub 用户 octocat 的信息
AI：GitHub 用户 octocat 的信息：
- 名称：Octocat
- 仓库：120
- 关注者：3500
```

## 开发指南

### 添加新的 MCP 工具

1. 在对应的 MCP 服务目录添加 endpoint
2. 在后端 `McpService` 中注册工具
3. 更新 `docs/mcp-api.md` 文档

### 添加新的前端页面

1. 在 `frontend/src/views/` 创建 Vue 组件
2. 在 `frontend/src/router/index.ts` 添加路由
3. 在 `frontend/src/App.vue` 添加菜单项

## 许可证

MIT License
