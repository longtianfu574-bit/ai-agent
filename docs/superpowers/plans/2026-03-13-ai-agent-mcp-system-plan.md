# AI Agent MCP 系统实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 MCP（Model Context Protocol）系统，包含 5 个独立 Python 进程服务器：filesystem、shell、database、http、git。

**Architecture:** 每个 MCP Server 是独立的 Python FastAPI 进程，通过 HTTP 协议与 Agent Core 通信。使用 OAuth 2.0 进行认证。

**Tech Stack:** Python 3.11, FastAPI, uvicorn, OAuthlib, SQLAlchemy (database), GitPython (git)

---

## 前置条件

**依赖计划:**
- `2026-03-13-ai-agent-foundation-plan.md` - 基础架构计划（必须已完成）

---

## Chunk 1: MCP 协议框架

### Task 1: 创建 MCP 协议基础结构

**Files:**
- Create: `mcp-servers/mcp_protocol/__init__.py`
- Create: `mcp-servers/mcp_protocol/base.py`
- Create: `mcp-servers/mcp_protocol/auth.py`
- Create: `mcp-servers/requirements-common.txt`

- [ ] **Step 1: 创建 MCP 协议基类**

```python
# mcp-servers/mcp_protocol/__init__.py
from .base import MCPServer, Tool, Resource
from .auth import require_auth, validate_token

__all__ = ['MCPServer', 'Tool', 'Resource', 'require_auth', 'validate_token']
```

- [ ] **Step 2: 创建 MCP 基类定义**

```python
# mcp-servers/mcp_protocol/base.py
from abc import ABC, abstractmethod
from typing import Any, Dict, List, Optional
from pydantic import BaseModel
from enum import Enum


class ToolResult(BaseModel):
    success: bool
    output: Any
    error: Optional[str] = None


class Tool(BaseModel):
    name: str
    description: str
    parameters: Dict[str, Any]

    class Config:
        arbitrary_types_allowed = True


class Resource(BaseModel):
    uri: str
    name: str
    description: str
    mime_type: str = "text/plain"


class MCPServer(ABC):
    def __init__(self, name: str, version: str = "1.0.0"):
        self.name = name
        self.version = version
        self._tools: Dict[str, Tool] = {}
        self._resources: Dict[str, Resource] = {}

    @abstractmethod
    async def initialize(self) -> None:
        """Initialize the server"""
        pass

    @abstractmethod
    async def call_tool(self, name: str, args: Dict[str, Any]) -> ToolResult:
        """Call a tool by name with arguments"""
        pass

    @abstractmethod
    async def read_resource(self, uri: str) -> Any:
        """Read a resource by URI"""
        pass

    def register_tool(self, tool: Tool, handler) -> None:
        """Register a tool and its handler"""
        self._tools[tool.name] = tool

    def register_resource(self, resource: Resource) -> None:
        """Register a resource"""
        self._resources[resource.uri] = resource

    def list_tools(self) -> List[Tool]:
        """List all available tools"""
        return list(self._tools.values())

    def list_resources(self) -> List[Resource]:
        """List all available resources"""
        return list(self._resources.values())
```

- [ ] **Step 3: 创建认证模块**

```python
# mcp-servers/mcp_protocol/auth.py
import os
import secrets
from functools import wraps
from typing import Optional
from fastapi import HTTPException, Security, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials

security = HTTPBearer()

# In production, use a proper token store
API_TOKENS: set = set()


def init_auth() -> None:
    """Initialize authentication with tokens from environment"""
    tokens = os.getenv("MCP_API_TOKENS", "").split(",")
    API_TOKENS.update(filter(None, tokens))

    # Generate a default token if none provided
    if not API_TOKENS:
        default_token = secrets.token_urlsafe(32)
        print(f"Generated default API token: {default_token}")
        API_TOKENS.add(default_token)


def validate_token(token: str) -> bool:
    """Validate an API token"""
    return token in API_TOKENS


def require_auth(func):
    """Decorator to require authentication on endpoints"""
    @wraps(func)
    async def wrapper(
        credentials: HTTPAuthorizationCredentials = Security(security)
    ):
        if not validate_token(credentials.credentials):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid or missing API token"
            )
        return await func(credentials.credentials)
    return wrapper
```

- [ ] **Step 4: 创建通用依赖文件**

```
# mcp-servers/requirements-common.txt
fastapi==0.109.0
uvicorn[standard]==0.27.0
pydantic==2.5.0
python-multipart==0.0.6
httpx==0.26.0
```

- [ ] **Step 5: 提交**

```bash
git add mcp-servers/mcp_protocol/
git add mcp-servers/requirements-common.txt
git commit -m "feat: create MCP protocol base framework"
```

---

## Chunk 2: Filesystem MCP Server

### Task 2: 创建文件系统 MCP 服务器

**Files:**
- Create: `mcp-servers/filesystem/Dockerfile`
- Create: `mcp-servers/filesystem/requirements.txt`
- Create: `mcp-servers/filesystem/server.py`
- Create: `mcp-servers/filesystem/main.py`

- [ ] **Step 1: 创建 Filesystem 服务器依赖**

```
# mcp-servers/filesystem/requirements.txt
-r ../requirements-common.txt
```

- [ ] **Step 2: 创建 Filesystem 服务器实现**

```python
# mcp-servers/filesystem/server.py
import os
import shutil
from pathlib import Path
from typing import Any, Dict, List

from mcp_protocol.base import MCPServer, Tool, ToolResult, Resource


class FilesystemServer(MCPServer):
    def __init__(self, root_path: str = "/data"):
        super().__init__(name="filesystem", version="1.0.0")
        self.root_path = Path(root_path)
        self._ensure_root_exists()

    def _ensure_root_exists(self):
        """Ensure root path exists"""
        self.root_path.mkdir(parents=True, exist_ok=True)

    def _safe_path(self, path: str) -> Path:
        """Ensure path is within root directory"""
        full_path = (self.root_path / path).resolve()
        try:
            full_path.relative_to(self.root_path.resolve())
            return full_path
        except ValueError:
            raise ValueError(f"Path {path} is outside root directory")

    async def initialize(self) -> None:
        """Register tools and resources"""
        # Register tools
        self.register_tool(
            Tool(
                name="read_file",
                description="Read contents of a file",
                parameters={
                    "path": {"type": "string", "description": "File path relative to root"}
                }
            ),
            self.read_file
        )

        self.register_tool(
            Tool(
                name="write_file",
                description="Write contents to a file",
                parameters={
                    "path": {"type": "string", "description": "File path"},
                    "content": {"type": "string", "description": "File content"}
                }
            ),
            self.write_file
        )

        self.register_tool(
            Tool(
                name="list_directory",
                description="List contents of a directory",
                parameters={
                    "path": {"type": "string", "description": "Directory path"}
                }
            ),
            self.list_directory
        )

        self.register_tool(
            Tool(
                name="delete_file",
                description="Delete a file",
                parameters={
                    "path": {"type": "string", "description": "File path"}
                }
            ),
            self.delete_file
        )

        # Register resources
        self.register_resource(Resource(
            uri="filesystem://",
            name="Filesystem Root",
            description="Root of the filesystem"
        ))

    async def call_tool(self, name: str, args: Dict[str, Any]) -> ToolResult:
        """Dispatch tool calls"""
        handlers = {
            "read_file": self.read_file,
            "write_file": self.write_file,
            "list_directory": self.list_directory,
            "delete_file": self.delete_file,
        }
        handler = handlers.get(name)
        if not handler:
            return ToolResult(success=False, output=None, error=f"Unknown tool: {name}")
        try:
            result = await handler(**args)
            return ToolResult(success=True, output=result)
        except Exception as e:
            return ToolResult(success=False, output=None, error=str(e))

    async def read_resource(self, uri: str) -> Any:
        """Read a resource"""
        raise NotImplementedError("Resource reading not implemented")

    # Tool implementations
    async def read_file(self, path: str) -> str:
        """Read file contents"""
        safe_path = self._safe_path(path)
        if not safe_path.exists():
            raise FileNotFoundError(f"File not found: {path}")
        return safe_path.read_text()

    async def write_file(self, path: str, content: str) -> str:
        """Write file contents"""
        safe_path = self._safe_path(path)
        safe_path.parent.mkdir(parents=True, exist_ok=True)
        safe_path.write_text(content)
        return f"Successfully wrote to {path}"

    async def list_directory(self, path: str) -> List[Dict[str, Any]]:
        """List directory contents"""
        safe_path = self._safe_path(path)
        if not safe_path.exists():
            raise FileNotFoundError(f"Directory not found: {path}")
        if not safe_path.is_dir():
            raise NotADirectoryError(f"Not a directory: {path}")

        result = []
        for item in safe_path.iterdir():
            result.append({
                "name": item.name,
                "type": "directory" if item.is_dir() else "file",
                "size": item.stat().st_size if item.is_file() else None
            })
        return result

    async def delete_file(self, path: str) -> str:
        """Delete a file"""
        safe_path = self._safe_path(path)
        if not safe_path.exists():
            raise FileNotFoundError(f"File not found: {path}")
        if safe_path.is_dir():
            raise IsADirectoryError(f"Cannot delete directory: {path}")
        safe_path.unlink()
        return f"Successfully deleted {path}"
```

- [ ] **Step 3: 创建 FastAPI 入口**

```python
# mcp-servers/filesystem/main.py
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Any, Dict, List, Optional

from mcp_protocol import require_auth, init_auth
from server import FilesystemServer

app = FastAPI(title="MCP Filesystem Server")

# Initialize
init_auth()
server = FilesystemServer(root_path=os.getenv("DATA_ROOT", "/data"))


@app.on_event("startup")
async def startup():
    await server.initialize()


class ToolCallRequest(BaseModel):
    tool_name: str
    args: Dict[str, Any]


class ToolCallResponse(BaseModel):
    success: bool
    output: Any
    error: Optional[str] = None


@app.get("/health")
async def health():
    return {"status": "healthy", "server": server.name}


@app.get("/tools")
async def list_tools():
    return [t.model_dump() for t in server.list_tools()]


@app.get("/resources")
async def list_resources():
    return [r.model_dump() for r in server.list_resources()]


@app.post("/tools/call", response_model=ToolCallResponse)
@require_auth
async def call_tool(request: ToolCallRequest):
    result = await server.call_tool(request.tool_name, request.args)
    return ToolCallResponse(
        success=result.success,
        output=result.output,
        error=result.error
    )
```

- [ ] **Step 4: 创建 Dockerfile**

```dockerfile
# mcp-servers/filesystem/Dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 5000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "5000"]
```

- [ ] **Step 5: 创建测试文件**

```python
# mcp-servers/filesystem/test_server.py
import pytest
import tempfile
from pathlib import Path

from server import FilesystemServer


@pytest.fixture
def temp_server():
    with tempfile.TemporaryDirectory() as tmpdir:
        server = FilesystemServer(root_path=tmpdir)
        yield server


@pytest.mark.asyncio
async def test_initialize_registers_tools(temp_server):
    await temp_server.initialize()
    tools = temp_server.list_tools()
    assert len(tools) == 4
    tool_names = [t.name for t in tools]
    assert "read_file" in tool_names
    assert "write_file" in tool_names


@pytest.mark.asyncio
async def test_write_and_read_file(temp_server):
    await temp_server.initialize()

    # Write a file
    result = await temp_server.call_tool("write_file", {
        "path": "test.txt",
        "content": "Hello, World!"
    })
    assert result.success

    # Read it back
    result = await temp_server.call_tool("read_file", {"path": "test.txt"})
    assert result.success
    assert result.output == "Hello, World!"


@pytest.mark.asyncio
async def test_list_directory(temp_server):
    await temp_server.initialize()

    # Create some files
    await temp_server.call_tool("write_file", {"path": "a.txt", "content": "a"})
    await temp_server.call_tool("write_file", {"path": "b.txt", "content": "b"})

    result = await temp_server.call_tool("list_directory", {"path": "."})
    assert result.success
    assert len(result.output) == 2
```

- [ ] **Step 6: 运行测试**

```bash
cd mcp-servers/filesystem
pip install -r requirements.txt pytest pytest-asyncio
pytest test_server.py -v
```
Expected: PASS

- [ ] **Step 7: 提交**

```bash
git add mcp-servers/filesystem/
git commit -m "feat: implement filesystem MCP server"
```

---

## Chunk 3: Shell MCP Server

### Task 3: 创建 Shell 命令执行服务器

**Files:**
- Create: `mcp-servers/shell/server.py`
- Create: `mcp-servers/shell/main.py`
- Create: `mcp-servers/shell/Dockerfile`
- Create: `mcp-servers/shell/requirements.txt`

- [ ] **Step 1: 创建 Shell 服务器实现**

```python
# mcp-servers/shell/server.py
import asyncio
import shlex
from typing import Any, Dict, List

from mcp_protocol.base import MCPServer, Tool, ToolResult, Resource


class ShellServer(MCPServer):
    def __init__(self, allowed_commands: List[str] = None):
        super().__init__(name="shell", version="1.0.0")
        self.allowed_commands = allowed_commands or []

    async def initialize(self) -> None:
        """Register tools"""
        self.register_tool(
            Tool(
                name="execute",
                description="Execute a shell command",
                parameters={
                    "command": {"type": "string", "description": "Command to execute"},
                    "timeout": {"type": "integer", "description": "Timeout in seconds"}
                }
            ),
            self.execute
        )

        self.register_tool(
            Tool(
                name="execute_safe",
                description="Execute command from allowed list only",
                parameters={
                    "command_name": {"type": "string", "description": "Allowed command name"},
                    "args": {"type": "array", "description": "Command arguments"}
                }
            ),
            self.execute_safe
        )

    async def call_tool(self, name: str, args: Dict[str, Any]) -> ToolResult:
        """Dispatch tool calls"""
        handlers = {
            "execute": self.execute,
            "execute_safe": self.execute_safe,
        }
        handler = handlers.get(name)
        if not handler:
            return ToolResult(success=False, output=None, error=f"Unknown tool: {name}")
        try:
            result = await handler(**args)
            return ToolResult(success=True, output=result)
        except Exception as e:
            return ToolResult(success=False, output=None, error=str(e))

    async def read_resource(self, uri: str) -> Any:
        raise NotImplementedError("Resource reading not implemented")

    async def execute(self, command: str, timeout: int = 30) -> Dict[str, Any]:
        """Execute a shell command"""
        try:
            proc = await asyncio.create_subprocess_shell(
                command,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE
            )
            stdout, stderr = await asyncio.wait_for(
                proc.communicate(),
                timeout=timeout
            )
            return {
                "stdout": stdout.decode(),
                "stderr": stderr.decode(),
                "returncode": proc.returncode
            }
        except asyncio.TimeoutError:
            raise TimeoutError(f"Command timed out after {timeout} seconds")

    async def execute_safe(self, command_name: str, args: List[str]) -> Dict[str, Any]:
        """Execute from allowed commands only"""
        if command_name not in self.allowed_commands:
            raise ValueError(f"Command '{command_name}' is not in allowed list")

        full_command = shlex.join([command_name] + args)
        return await self.execute(full_command)
```

- [ ] **Step 2: 创建 FastAPI 入口**

```python
# mcp-servers/shell/main.py
import os
from fastapi import FastAPI

from mcp_protocol import require_auth, init_auth
from server import ShellServer

app = FastAPI(title="MCP Shell Server")

# Initialize
init_auth()
allowed = os.getenv("ALLOWED_COMMANDS", "").split(",")
server = ShellServer(allowed_commands=allowed)


@app.on_event("startup")
async def startup():
    await server.initialize()


# Reuse same endpoints as filesystem (could be extracted to base)
@app.get("/health")
async def health():
    return {"status": "healthy", "server": server.name}


@app.get("/tools")
async def list_tools():
    return [t.model_dump() for t in server.list_tools()]


# ... (same ToolCallRequest/Response and /tools/call endpoint)
```

- [ ] **Step 3: 创建 Dockerfile**

```dockerfile
# mcp-servers/shell/Dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 5000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "5000"]
```

- [ ] **Step 4: 创建 requirements.txt**

```
# mcp-servers/shell/requirements.txt
-r ../requirements-common.txt
```

- [ ] **Step 5: 提交**

```bash
git add mcp-servers/shell/
git commit -m "feat: implement shell MCP server"
```

---

## Chunk 4: Database/HTTP/Git MCP Servers

### Task 4: 创建剩余 MCP 服务器

**Files:**
- Create: `mcp-servers/database/server.py`
- Create: `mcp-servers/database/main.py`
- Create: `mcp-servers/http/server.py`
- Create: `mcp-servers/http/main.py`
- Create: `mcp-servers/git/server.py`
- Create: `mcp-servers/git/main.py`

由于这三个服务器结构类似，我提供核心实现代码：

- [ ] **Step 1: Database MCP Server**

```python
# mcp-servers/database/server.py
import os
from typing import Any, Dict, Optional
from contextlib import contextmanager

import sqlalchemy as sa
from mcp_protocol.base import MCPServer, Tool, ToolResult


class DatabaseServer(MCPServer):
    def __init__(self, connection_string: Optional[str] = None):
        super().__init__(name="database", version="1.0.0")
        self.connection_string = connection_string or os.getenv("DATABASE_URL", "sqlite:///./data.db")
        self.engine = None

    async def initialize(self) -> None:
        self.engine = sa.create_engine(self.connection_string)
        self.register_tool(
            Tool(
                name="query",
                description="Execute a SQL query",
                parameters={
                    "sql": {"type": "string", "description": "SQL query"},
                    "params": {"type": "object", "description": "Query parameters"}
                }
            ),
            self.query
        )

    @contextmanager
    def get_connection(self):
        conn = self.engine.connect()
        try:
            yield conn
        finally:
            conn.close()

    async def call_tool(self, name: str, args: Dict[str, Any]) -> ToolResult:
        if name == "query":
            try:
                result = await self.query(args["sql"], args.get("params"))
                return ToolResult(success=True, output=result)
            except Exception as e:
                return ToolResult(success=False, output=None, error=str(e))
        return ToolResult(success=False, output=None, error=f"Unknown tool: {name}")

    async def read_resource(self, uri: str) -> Any:
        raise NotImplementedError

    async def query(self, sql: str, params: Optional[Dict] = None) -> list:
        with self.get_connection() as conn:
            result = conn.execute(sa.text(sql), params or {})
            if result.returns_rows:
                return [dict(row._mapping) for row in result]
            return [{"rows_affected": result.rowcount}]
```

- [ ] **Step 2: HTTP MCP Server**

```python
# mcp-servers/http/server.py
from typing import Any, Dict
import httpx
from mcp_protocol.base import MCPServer, Tool, ToolResult


class HttpServer(MCPServer):
    def __init__(self):
        super().__init__(name="http", version="1.0.0")
        self.client = httpx.AsyncClient()

    async def initialize(self) -> None:
        self.register_tool(
            Tool(
                name="request",
                description="Make an HTTP request",
                parameters={
                    "url": {"type": "string", "description": "URL to request"},
                    "method": {"type": "string", "description": "HTTP method"},
                    "headers": {"type": "object", "description": "Request headers"},
                    "body": {"type": "string", "description": "Request body"}
                }
            ),
            self.request
        )

    async def call_tool(self, name: str, args: Dict[str, Any]) -> ToolResult:
        if name == "request":
            try:
                result = await self.request(**args)
                return ToolResult(success=True, output=result)
            except Exception as e:
                return ToolResult(success=False, output=None, error=str(e))
        return ToolResult(success=False, output=None, error=f"Unknown tool: {name}")

    async def read_resource(self, uri: str) -> Any:
        raise NotImplementedError

    async def request(
        self,
        url: str,
        method: str = "GET",
        headers: Dict = None,
        body: str = None
    ) -> Dict[str, Any]:
        response = await self.client.request(
            method,
            url,
            headers=headers,
            content=body
        )
        return {
            "status": response.status_code,
            "headers": dict(response.headers),
            "body": response.text
        }
```

- [ ] **Step 3: Git MCP Server**

```python
# mcp-servers/git/server.py
import os
from pathlib import Path
from typing import Any, Dict, List
from git import Repo
from mcp_protocol.base import MCPServer, Tool, ToolResult


class GitServer(MCPServer):
    def __init__(self, root_path: str = "/data/repos"):
        super().__init__(name="git", version="1.0.0")
        self.root_path = Path(root_path)
        self.root_path.mkdir(parents=True, exist_ok=True)

    async def initialize(self) -> None:
        self.register_tool(
            Tool(
                name="clone",
                description="Clone a git repository",
                parameters={
                    "url": {"type": "string", "description": "Repository URL"},
                    "path": {"type": "string", "description": "Local path"}
                }
            ),
            self.clone
        )
        self.register_tool(
            Tool(
                name="status",
                description="Get git status",
                parameters={
                    "path": {"type": "string", "description": "Repository path"}
                }
            ),
            self.status
        )
        self.register_tool(
            Tool(
                name="commit",
                description="Commit changes",
                parameters={
                    "path": {"type": "string", "description": "Repository path"},
                    "message": {"type": "string", "description": "Commit message"}
                }
            ),
            self.commit
        )

    async def call_tool(self, name: str, args: Dict[str, Any]) -> ToolResult:
        handlers = {
            "clone": self.clone,
            "status": self.status,
            "commit": self.commit,
        }
        handler = handlers.get(name)
        if not handler:
            return ToolResult(success=False, output=None, error=f"Unknown tool: {name}")
        try:
            result = await handler(**args)
            return ToolResult(success=True, output=result)
        except Exception as e:
            return ToolResult(success=False, output=None, error=str(e))

    async def read_resource(self, uri: str) -> Any:
        raise NotImplementedError

    def _get_repo(self, path: str) -> Repo:
        repo_path = self.root_path / path
        return Repo(repo_path)

    async def clone(self, url: str, path: str) -> str:
        repo_path = self.root_path / path
        Repo.clone_from(url, repo_path)
        return f"Cloned {url} to {path}"

    async def status(self, path: str) -> Dict[str, Any]:
        repo = self._get_repo(path)
        return {
            "branch": repo.active_branch.name,
            "untracked": repo.untracked_files,
            "modified": [item.a_path for item in repo.index.diff(None)]
        }

    async def commit(self, path: str, message: str) -> str:
        repo = self._get_repo(path)
        repo.index.commit(message)
        return f"Committed: {message}"
```

- [ ] **Step 4: 为每个服务器创建 main.py 和 Dockerfile**

（结构同 filesystem server）

- [ ] **Step 5: 更新主 docker-compose.yml**

确保所有 MCP servers 都已配置：

```yaml
# docker-compose.yml 中已有的配置
services:
  mcp-filesystem:
    # ...
  mcp-shell:
    # ...
  mcp-database:
    build:
      context: ./mcp-servers/database
    ports:
      - "5003:5000"
    environment:
      - DATABASE_URL=sqlite:///./data/data.db
    volumes:
      - ./data:/data
  mcp-http:
    build:
      context: ./mcp-servers/http
    ports:
      - "5004:5000"
  mcp-git:
    build:
      context: ./mcp-servers/git
    ports:
      - "5005:5000"
    volumes:
      - ./data/repos:/data/repos
```

- [ ] **Step 6: 提交**

```bash
git add mcp-servers/database/ mcp-servers/http/ mcp-servers/git/
git commit -m "feat: implement database, http, and git MCP servers"
```

---

## Chunk 5: Java MCP 客户端

### Task 5: 创建 Java MCP 客户端

**Files:**
- Create: `backend/src/main/java/com/aiagent/mcp/McpClient.java`
- Create: `backend/src/main/java/com/aiagent/mcp/McpTool.java`
- Create: `backend/src/main/java/com/aiagent/mcp/McpService.java`
- Test: `backend/src/test/java/com/aiagent/mcp/McpClientTests.java`

- [ ] **Step 1: 创建 MCP Tool DTO**

```java
package com.aiagent.mcp;

import lombok.Data;
import java.util.Map;

@Data
public class McpTool {
    private String name;
    private String description;
    private Map<String, Object> parameters;
}
```

- [ ] **Step 2: 创建 MCP 客户端**

```java
package com.aiagent.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
public class McpClient {

    private static final Logger log = LoggerFactory.getLogger(McpClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiToken;

    public McpClient(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${mcp.api-token:default-token}") String apiToken) {
        this.objectMapper = objectMapper;
        this.apiToken = apiToken;
        this.webClient = webClientBuilder
            .defaultHeader("Authorization", "Bearer " + apiToken)
            .build();
    }

    public CompletableFuture<List<McpTool>> listTools(String serverUrl) {
        return webClient.get()
            .uri(serverUrl + "/tools")
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(node -> {
                List<McpTool> tools = new ArrayList<>();
                if (node.isArray()) {
                    for (JsonNode toolNode : node) {
                        McpTool tool = objectMapper.convertValue(toolNode, McpTool.class);
                        tools.add(tool);
                    }
                }
                return tools;
            })
            .toFuture();
    }

    public CompletableFuture<ToolCallResult> callTool(
            String serverUrl,
            String toolName,
            Map<String, Object> args) {

        Map<String, Object> request = Map.of(
            "tool_name", toolName,
            "args", args
        );

        return webClient.post()
            .uri(serverUrl + "/tools/call")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ToolCallResult.class)
            .toFuture();
    }

    public record ToolCallResult(boolean success, Object output, String error) {}
}
```

- [ ] **Step 3: 创建 MCP 服务**

```java
package com.aiagent.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class McpService {

    private static final Logger log = LoggerFactory.getLogger(McpService.class);

    private final McpClient mcpClient;
    private final Map<String, String> serverUrls;

    public McpService(
            McpClient mcpClient,
            @Value("${mcp.servers.filesystem:http://localhost:5001}") String filesystemUrl,
            @Value("${mcp.servers.shell:http://localhost:5002}") String shellUrl,
            @Value("${mcp.servers.database:http://localhost:5003}") String databaseUrl,
            @Value("${mcp.servers.http:http://localhost:5004}") String httpUrl,
            @Value("${mcp.servers.git:http://localhost:5005}") String gitUrl) {

        this.mcpClient = mcpClient;
        this.serverUrls = Map.of(
            "filesystem", filesystemUrl,
            "shell", shellUrl,
            "database", databaseUrl,
            "http", httpUrl,
            "git", gitUrl
        );
    }

    public CompletableFuture<List<McpTool>> listAllTools() {
        List<CompletableFuture<List<McpTool>>> futures = serverUrls.entrySet().stream()
            .map(entry -> mcpClient.listTools(entry.getValue())
                .thenApply(tools -> {
                    tools.forEach(tool ->
                        tool.setName(entry.getKey() + "/" + tool.getName()));
                    return tools;
                }))
            .collect(Collectors.toList());

        return CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .flatMap(f -> f.join().stream())
                .collect(Collectors.toList()));
    }

    public CompletableFuture<McpClient.ToolCallResult> callTool(
            String server,
            String toolName,
            Map<String, Object> args) {

        String url = serverUrls.get(server);
        if (url == null) {
            CompletableFuture<McpClient.ToolCallResult> failed = new CompletableFuture<>();
            failed.completeExceptionally(
                new IllegalArgumentException("Unknown server: " + server));
            return failed;
        }

        log.info("Calling tool {} on server {}", toolName, server);
        return mcpClient.callTool(url, toolName, args);
    }
}
```

- [ ] **Step 4: 添加 MCP 配置到 application.yml**

```yaml
mcp:
  api-token: ${MCP_API_TOKEN:default-token}
  servers:
    filesystem: ${MCP_FILESYSTEM_URL:http://localhost:5001}
    shell: ${MCP_SHELL_URL:http://localhost:5002}
    database: ${MCP_DATABASE_URL:http://localhost:5003}
    http: ${MCP_HTTP_URL:http://localhost:5004}
    git: ${MCP_GIT_URL:http://localhost:5005}
```

- [ ] **Step 5: 创建测试**

```java
package com.aiagent.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class McpServiceTests {

    @Autowired
    private McpService mcpService;

    @MockBean
    private McpClient mcpClient;

    @Test
    void listAllTools_returnsToolsFromAllServers() throws Exception {
        List<McpTool> mockTools = List.of(
            new McpTool("read_file", "Read file", Map.of())
        );

        when(mcpClient.listTools(anyString()))
            .thenReturn(CompletableFuture.completedFuture(mockTools));

        List<McpTool> allTools = mcpService.listAllTools().join();

        assertEquals(5, allTools.size()); // 5 servers
    }

    @Test
    void callTool_callsCorrectServer() throws Exception {
        var mockResult = new McpClient.ToolCallResult(true, "output", null);

        when(mcpClient.callTool(anyString(), anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(mockResult));

        var result = mcpService.callTool("filesystem", "read_file", Map.of("path", "test.txt")).join();

        assertTrue(result.success());
        assertEquals("output", result.output());
    }
}
```

- [ ] **Step 6: 运行测试**

```bash
cd backend
mvn test -Dtest=McpServiceTests
```
Expected: PASS

- [ ] **Step 7: 提交**

```bash
git add backend/src/main/java/com/aiagent/mcp/
git add backend/src/test/java/com/aiagent/mcp/
git commit -m "feat: implement Java MCP client and service"
```

---

## 验收标准

1. 所有 5 个 MCP servers 可以独立启动
2. `docker-compose up -d` 启动所有 servers
3. Java MCP 客户端可以列出所有 tools
4. Java MCP 客户端可以调用 tools 并获取结果
5. 认证机制正常工作

---

## 后续工作

1. OAuth 2.0 完整集成（目前是简单的 API token）
2. MCP Server 健康检查和自动重连
3. 请求/响应日志和审计
4. 资源模板支持
