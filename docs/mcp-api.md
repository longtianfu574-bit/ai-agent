# MCP 工具调用 API 文档

## 概述

MCP (Model Context Protocol) 系统允许大模型在对话过程中根据用户提示词自动调用工具。

## 架构

```
用户请求 -> ChatController -> RagQueryService -> LlmClient (带 function calling)
                                 |
                                 v
                         McpToolExecutor -> McpService -> McpClient -> MCP Servers
```

## API Endpoints

### 1. 对话 endpoint (带工具调用)

```
POST /api/chat/tools
Content-Type: application/json

{
    "message": "帮我查看 /home 目录下有哪些文件",
    "sessionId": "optional-session-id"
}
```

**响应:**
```json
{
    "sessionId": "uuid",
    "model": "qwen-3.5-mcp",
    "message": "在 /home 目录下有以下文件：..."
}
```

### 2. 对话 endpoint (RAG 模式)

```
POST /api/chat
Content-Type: application/json

{
    "message": "项目文档在哪里？",
    "sessionId": "optional-session-id"
}
```

### 3. 直接调用 MCP 工具

#### 列出所有服务器
```
GET /api/mcp/servers
```

#### 列出所有工具
```
GET /api/mcp/tools
```

#### 调用工具
```
POST /api/mcp/tools/call
Content-Type: application/json

{
    "server": "filesystem",
    "tool": "read_file",
    "args": {
        "path": "config.txt"
    }
}
```

#### 快捷方式

**读取文件:**
```
GET /api/mcp/filesystem/read?path=/config/app.json
```

**写入文件:**
```
POST /api/mcp/filesystem/write?path=/test.txt
Content-Type: text/plain

文件内容
```

**列出目录:**
```
GET /api/mcp/filesystem/list?path=/home
```

**执行 shell 命令:**
```
POST /api/mcp/shell/execute?command=ls -la&timeout=30
```

**执行 SQL 查询:**
```
POST /api/mcp/database/query?sql=SELECT * FROM users
```

**HTTP GET:**
```
GET /api/mcp/http/get?url=https://api.example.com/data
```

**HTTP POST:**
```
POST /api/mcp/http/post?url=https://api.example.com/data
Content-Type: application/json

{"key": "value"}
```

**Git 状态:**
```
GET /api/mcp/git/status?path=my-repo
```

**Git 初始化:**
```
POST /api/mcp/git/init?path=new-repo
```

## 可用工具列表

### Filesystem Server
| 工具名 | 描述 | 参数 |
|--------|------|------|
| `read_file` | 读取文件内容 | `path`: 文件路径 |
| `write_file` | 写入文件内容 | `path`: 文件路径，`content`: 内容 |
| `list_directory` | 列出目录内容 | `path`: 目录路径 |
| `delete_file` | 删除文件 | `path`: 文件路径 |

### Shell Server
| 工具名 | 描述 | 参数 |
|--------|------|------|
| `execute` | 执行 shell 命令 | `command`: 命令，`timeout`: 超时 (秒) |
| `execute_safe` | 执行允许的命令 | `command_name`: 命令名，`args`: 参数 |

### Database Server
| 工具名 | 描述 | 参数 |
|--------|------|------|
| `query` | 执行 SQL 查询 | `sql`: SQL 语句，`params`: 参数 |
| `execute` | 执行 SQL 语句 | `sql`: SQL 语句，`params`: 参数 |

### HTTP Server
| 工具名 | 描述 | 参数 |
|--------|------|------|
| `request` | 发起 HTTP 请求 | `url`: URL，`method`: 方法，`headers`: 头，`body`: 体 |
| `get` | HTTP GET | `url`: URL，`headers`: 头 |
| `post` | HTTP POST | `url`: URL，`body`: 体，`headers`: 头 |

### Git Server
| 工具名 | 描述 | 参数 |
|--------|------|------|
| `clone` | 克隆仓库 | `url`: 仓库 URL，`path`: 本地路径 |
| `init` | 初始化仓库 | `path`: 目录路径 |
| `status` | 获取状态 | `path`: 仓库路径 |
| `commit` | 提交更改 | `path`: 仓库路径，`message`: 提交信息 |
| `log` | 查看日志 | `path`: 仓库路径，`count`: 数量 |

## 使用示例

### 示例 1: 查询文件系统

**用户:** "帮我看看 /data 目录下有什么文件"

**LLM 调用:** `list_directory` with `path: "/data"`

**响应:** "在 /data 目录下有以下文件：..."

### 示例 2: 执行 shell 命令

**用户:** "运行 ls -la 命令"

**LLM 调用:** `execute_shell` with `command: "ls -la"`

**响应:** "命令执行结果：\n total 32\n drwxr-xr-x ..."

### 示例 3: 读取文件

**用户:** "读取配置文件的内容"

**LLM 调用:** `read_file` with `path: "config.json"`

**响应:** "配置文件内容如下：\n {\"key\": \"value\"}"

### 示例 4: 数据库查询

**用户:** "查询所有用户数据"

**LLM 调用:** `execute_sql` with `sql: "SELECT * FROM users"`

**响应:** "查询到以下用户：..."

### 示例 5: HTTP 请求

**用户:** "获取 https://api.github.com/users/octocat 的信息"

**LLM 调用:** `http_get` with `url: "https://api.github.com/users/octocat"`

**响应:** "GitHub 用户 octocat 的信息：..."

## 启动服务

1. 启动 MCP 服务器:
```bash
cd mcp-servers/filesystem
uvicorn main:app --host 0.0.0.0 --port 5001

cd ../shell
uvicorn main:app --host 0.0.0.0 --port 5002

# ... 类似启动其他服务器
```

2. 启动 Spring Boot 应用:
```bash
cd backend
mvn spring-boot:run
```

3. 设置环境变量 (可选):
```bash
export MCP_API_TOKEN=your-token
export MCP_FILESYSTEM_URL=http://localhost:5001
export MCP_SHELL_URL=http://localhost:5002
# ...
```
