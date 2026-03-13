# AI Agent 前期准备指南

**日期**: 2026-03-13
**版本**: 1.0

本文档描述启动 AI Agent 项目前需要完成的所有准备工作。

---

## 1. 环境依赖安装

### 1.1 必须安装的工具

| 工具 | 版本要求 | 用途 | 下载地址 |
|------|----------|------|----------|
| **Java JDK** | 17+ | 后端开发 | https://adoptium.net/ |
| **Maven** | 3.9+ | Java 包管理 | https://maven.apache.org/ |
| **Node.js** | 20+ | 前端开发 | https://nodejs.org/ |
| **Python** | 3.11+ | MCP Servers 和嵌入服务 | https://python.org/ |
| **Docker Desktop** | 最新版 | 容器编排 | https://docker.com/ |
| **Git** | 最新版 | 版本控制 | https://git-scm.com/ |

### 1.2 验证安装

运行以下命令验证所有依赖已正确安装：

```bash
# 检查所有依赖
java -version      # 应输出 Java 17+
mvn -version       # 应输出 Maven 3.9+
node -version      # 应输出 v20+
npm -version       # 应输出 9+
python --version   # 应输出 3.11+
docker --version   # 应输出 Docker 20+
git --version      # 应输出 git 2+
```

### 1.3 Windows 环境特别注意事项

1. **Java 环境变量**：确保 `JAVA_HOME` 指向 JDK 17 安装目录
2. **Python 路径**：确保 Python 已添加到系统 PATH
3. **Docker WSL2**：建议使用 WSL2 后端运行 Docker
4. **路径分隔符**：在 Git Bash 中使用正斜杠 `/`，不是反斜杠 `\`

---

## 2. API Key 和凭证准备

### 2.1 可选 API 服务

| 服务 | 用途 | 获取方式 |
|------|------|----------|
| **OpenAI API Key** | LLM 调用 | https://platform.openai.com/ |
| **Claude API Key** | LLM 调用 | https://console.anthropic.com/ |
| **GitHub OAuth** | Git MCP 认证 | https://github.com/settings/developers |

> **注**：初期可以使用本地模型（如 Ollama）替代付费 API，详见第 6 节。

---

## 3. 项目目录结构

在项目根目录创建以下结构：

```
e:\works\ai agent/
├── backend/                    # Java 后端
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/
│   └── Dockerfile
│
├── frontend/                   # Vue 前端
│   ├── package.json
│   ├── vite.config.ts
│   ├── src/
│   │   ├── views/
│   │   ├── components/
│   │   ├── router/
│   │   ├── stores/
│   │   └── api/
│   └── Dockerfile
│
├── mcp-servers/               # Python MCP 服务
│   ├── mcp_protocol/          # 协议基础库
│   ├── filesystem/
│   ├── shell/
│   ├── database/
│   ├── http/
│   └── git/
│
├── docker/                     # Docker 配置
│   └── python-sidecar/
│       ├── Dockerfile
│       ├── requirements.txt
│       └── embedding_service.py
│
├── data/                       # 运行时数据（.gitignore）
│   ├── qdrant/
│   ├── files/
│   ├── repos/
│   └── memory.db
│
├── docs/
│   └── superpowers/
│       ├── specs/             # 设计文档
│       └── plans/             # 实施计划
│
├── docker-compose.yml
├── .env
└── .gitignore
```

### 创建目录命令

```bash
cd "e:\works\ai agent"

# 创建后端目录
mkdir -p backend/src/main/java/com/aiagent
mkdir -p backend/src/main/resources
mkdir -p backend/src/test/java/com/aiagent

# 创建前端目录
mkdir -p frontend/src/{views,components,router,stores,api}

# 创建 MCP 服务目录
mkdir -p mcp-servers/{mcp_protocol,filesystem,shell,database,http,git}

# 创建 Docker 目录
mkdir -p docker/python-sidecar

# 创建数据目录
mkdir -p data/{qdrant,files,repos}

# 创建文档目录
mkdir -p docs/superpowers/{specs,plans}
```

---

## 4. 配置文件

### 4.1 创建 `.env` 文件

在项目根目录创建 `.env` 文件：

```bash
# .env - 环境变量配置

# LLM API (可选，初期可用本地模型)
OPENAI_API_KEY=your-api-key-here

# MCP 认证 Token
MCP_API_TOKENS=your-mcp-token-here

# Qdrant 配置
QDRANT_HOST=localhost
QDRANT_PORT=6334
QDRANT_COLLECTION=ai-agent-rag

# 嵌入服务配置
EMBEDDING_SERVICE_URL=http://localhost:5000

# Memory 配置
MEMORY_PATH=./data/memory.db

# MCP Servers 配置
MCP_FILESYSTEM_URL=http://localhost:5001
MCP_SHELL_URL=http://localhost:5002
MCP_DATABASE_URL=http://localhost:5003
MCP_HTTP_URL=http://localhost:5004
MCP_GIT_URL=http://localhost:5005
```

### 4.2 创建 `.gitignore` 文件

```gitignore
# 环境和构建
.superpowers/
data/
.env
*.db
*.log

# Node.js
node_modules/
dist/
.npm

# Java
target/
*.class
*.jar
*.war

# Python
__pycache__/
*.pyc
*.pyo
.venv/
*.egg-info/

# IDE
.idea/
.vscode/
*.iml
.DS_Store
Thumbs.db

# Docker
.docker/
```

---

## 5. Python 虚拟环境

### 5.1 创建虚拟环境

```bash
cd "e:\works\ai agent"

# 创建虚拟环境
python -m venv .venv

# 激活虚拟环境 (Windows Git Bash)
source .venv/Scripts/activate

# 激活虚拟环境 (Windows CMD)
# .venv\Scripts\activate

# 激活虚拟环境 (PowerShell)
# .venv\Scripts\Activate.ps1
```

### 5.2 升级 pip

```bash
pip install --upgrade pip
```

### 5.3 安装通用依赖

```bash
# 创建通用依赖文件
cat > mcp-servers/requirements-common.txt << EOF
fastapi==0.109.0
uvicorn[standard]==0.27.0
pydantic==2.5.0
python-multipart==0.0.6
httpx==0.26.0
EOF
```

---

## 6. 本地模型部署（可选）

如果不想使用付费 API，可以部署本地模型。

### 6.1 安装 Ollama（推荐）

Ollama 用于运行本地嵌入模型和对话模型。

**Windows 安装：**

1. 访问 https://ollama.ai/ 下载安装包
2. 运行安装程序
3. 验证安装：

```bash
ollama --version
```

### 6.2 拉取模型

```bash
# 拉取嵌入模型 (用于 RAG 检索)
ollama pull bge-m3

# 拉取对话模型 (可选，已有千问 3.5)
ollama pull qwen2.5

# 验证模型
ollama list
```

### 6.3 测试嵌入 API

```bash
curl http://localhost:11434/api/embeddings \
  -H "Content-Type: application/json" \
  -d '{"model": "bge-m3", "prompt": "你好"}'
```

### 6.4 Ollama 服务配置

Ollama 默认在 `http://localhost:11434` 运行。

在 `.env` 文件中配置：

```bash
# Ollama 配置
EMBEDDING_PROVIDER=ollama
EMBEDDING_BASE_URL=http://localhost:11434
EMBEDDING_MODEL=bge-m3
```

### 6.5 详细配置文档

查看 [ollama-embedding-setup.md](ollama-embedding-setup.md) 获取完整指南。

---

## 7. IDE/编辑器配置

### 7.1 VS Code 推荐插件

| 插件 | 用途 |
|------|------|
| Spring Boot Extension Pack | Spring Boot 开发 |
| Volar | Vue 3 开发 |
| Python (Microsoft) | Python 开发 |
| Docker (Microsoft) | Docker 支持 |
| GitLens | Git 增强 |
| REST Client | API 测试 |

### 7.2 IntelliJ IDEA 推荐插件

| 插件 | 用途 |
|------|------|
| Spring Boot | Spring Boot 支持 |
| Lombok | Lombok 注解处理 |
| Docker | Docker 集成 |
| MyBatisX | MyBatis 支持（如使用） |

### 7.3 PyCharm 推荐配置

- 启用 FastAPI 支持
- 配置 Python 解释器为项目 `.venv`
- 安装 Docker 插件

---

## 8. 环境验证脚本

创建 `scripts/verify-env.sh` 脚本来验证环境：

```bash
#!/bin/bash
# scripts/verify-env.sh
# 环境验证脚本

echo "======================================"
echo "       AI Agent 环境验证"
echo "======================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

check_command() {
    local name=$1
    local cmd=$2
    local version=$($cmd 2>&1 | head -1)
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $name: $version"
        return 0
    else
        echo -e "${RED}✗${NC} $name: 未安装或配置错误"
        return 1
    fi
}

echo "--- 工具版本检查 ---"
check_command "Java" "java -version"
check_command "Maven" "mvn -version"
check_command "Node" "node -v"
check_command "NPM" "npm -v"
check_command "Python" "python --version"
check_command "Docker" "docker --version"
check_command "Git" "git --version"

echo ""
echo "--- 目录检查 ---"

dirs=("backend" "frontend" "mcp-servers" "docker" "data" "docs")
for dir in "${dirs[@]}"; do
    if [ -d "$dir" ]; then
        echo -e "${GREEN}✓${NC} $dir/ 存在"
    else
        echo -e "${RED}✗${NC} $dir/ 不存在"
    fi
done

echo ""
echo "--- Docker 状态 ---"
docker info > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓${NC} Docker 运行中"
else
    echo -e "${RED}✗${NC} Docker 未运行"
fi

echo ""
echo "--- 文件检查 ---"
files=("docker-compose.yml" ".env" ".gitignore")
for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo -e "${GREEN}✓${NC} $file 存在"
    else
        echo -e "${RED}✗${NC} $file 不存在"
    fi
done

echo ""
echo "======================================"
echo "验证完成"
echo "======================================"
```

### 使用脚本

```bash
cd "e:\works\ai agent"
chmod +x scripts/verify-env.sh
./scripts/verify-env.sh
```

---

## 9. 检查清单

在开始编码前，确认以下项目全部完成：

### 基础环境
- [ ] Java 17+ 已安装且 `JAVA_HOME` 配置正确
- [ ] Maven 3.9+ 已安装
- [ ] Node.js 20+ 已安装
- [ ] Python 3.11+ 已安装
- [ ] Docker Desktop 已安装并正在运行
- [ ] Git 已安装

### 项目配置
- [ ] 项目目录结构已创建
- [ ] `.env` 文件已创建
- [ ] `.gitignore` 文件已配置
- [ ] Python 虚拟环境已创建（可选）

### 可选配置
- [ ] Ollama 已安装并运行
- [ ] 本地模型已拉取
- [ ] IDE 插件已安装

### API 凭证
- [ ] OpenAI API Key 已配置（如使用）
- [ ] Claude API Key 已配置（如使用）

---

## 10. 下一步

完成前期准备后，按照以下顺序开始实施：

1. **基础架构计划** → `docs/superpowers/plans/2026-03-13-ai-agent-foundation-plan.md`
2. **RAG 服务计划** → `docs/superpowers/plans/2026-03-13-ai-agent-rag-service-plan.md`
3. **MCP 系统计划** → `docs/superpowers/plans/2026-03-13-ai-agent-mcp-system-plan.md`
4. **Skills 系统计划** → `docs/superpowers/plans/2026-03-13-ai-agent-skills-system-plan.md`
5. **Memory 系统计划** → `docs/superpowers/plans/2026-03-13-ai-agent-memory-system-plan.md`
6. **前端界面计划** → `docs/superpowers/plans/2026-03-13-ai-agent-frontend-plan.md`

---

## 常见问题

### Q: Docker Desktop 启动失败
**A**: 确保 WSL2 已启用，或在 Docker 设置中切换到 Hyper-V 后端。

### Q: Python 版本不兼容
**A**: 确保使用 Python 3.11 或更高版本。使用 `py -3.11 -m venv .venv` 指定版本。

### Q: 端口冲突
**A**: 修改 `.env` 中的端口配置，或停止占用端口的服务。

### Q: 模型下载缓慢
**A**: 使用镜像源或离线下载模型文件。
