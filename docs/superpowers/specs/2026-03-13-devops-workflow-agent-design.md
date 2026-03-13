# DevOps 工作流自动化 Agent - 设计规格文档

**创建日期**: 2026-03-13
**版本**: 1.0
**状态**: 已批准

---

## 1. 概述

### 1.1 项目定位
基于 Java Spring Boot 的 DevOps 自动化工作流 Agent，提供容器化、可扩展的智能任务编排系统。

### 1.2 核心目标
- 自动化 DevOps/运维工作流（Git 操作、CI/CD、容器部署、云资源管理、监控告警、项目管理）
- 支持多种触发方式（定时、事件驱动、手动、自然语言）
- 提供多种交互界面（CLI、Web 控制台、REST API）
- 采用混合编排模式（YAML 配置 + Java 代码扩展）

---

## 2. 系统架构

### 2.1 分层架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      User Interface Layer                     │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────┐  │
│  │   CLI       │  │  Web Dashboard│  │  REST/GraphQL API   │  │
│  └─────────────┘  └──────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway / Controller Layer           │
│  - Request routing  - Authentication  - Rate limiting         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Core Engine Layer                        │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────┐  │
│  │ Workflow    │  │   Task       │  │   Event             │  │
│  │ Orchestrator│  │   Scheduler  │  │   Bus               │  │
│  └─────────────┘  └──────────────┘  └─────────────────────┘  │
│  ┌─────────────┐  ┌──────────────┐                            │
│  │   Plugin    │  │    AI        │                            │
│  │   Registry  │  │    Adapter   │                            │
│  └─────────────┘  └─────────────────┘                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Integration Layer                        │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐     │
│  │  Git   │ │  CI/CD │ │  K8s   │ │ Cloud  │ │  Jira  │     │
│  │ Provider│ │Provider│ │Provider│ │Provider│ │Provider│     │
│  └────────┘ └────────┘ └────────┘ └────────┘ └────────┘     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Infrastructure Layer                     │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────┐  │
│  │  PostgreSQL │  │    Redis     │  │   Message Queue     │  │
│  │  (State)    │  │   (Cache)    │  │   (Async Tasks)     │  │
│  └─────────────┘  └──────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 模块说明

#### User Interface Layer
| 组件 | 职责 | 技术选型 |
|------|------|----------|
| CLI | 命令行交互，脚本集成 | Java CLI (Picocli) |
| Web Dashboard | 可视化工作流管理、状态监控 | React + TypeScript |
| REST/GraphQL API | 外部系统集成 | Spring Boot REST + GraphQL |

#### API Gateway / Controller Layer
- **Request Routing**: 请求路由和分发
- **Authentication**: JWT/OAuth2 认证
- **Rate Limiting**: 请求限流
- **Request Logging**: 请求日志记录

#### Core Engine Layer
| 组件 | 职责 |
|------|------|
| Workflow Orchestrator | 工作流解析、执行、状态管理 |
| Task Scheduler | 定时任务调度（基于 Cron） |
| Event Bus | 事件发布/订阅，解耦组件通信 |
| Plugin Registry | 任务插件注册、发现、生命周期管理 |
| AI Adapter | 自然语言解析、AI 服务适配 |

#### Integration Layer
| Provider | 集成服务 |
|----------|----------|
| Git Provider | GitHub, GitLab, Bitbucket |
| CI/CD Provider | Jenkins, GitHub Actions, GitLab CI |
| K8s Provider | Kubernetes, Docker |
| Cloud Provider | AWS, Azure, 阿里云 |
| Monitoring Provider | Prometheus, Grafana, ELK |
| Project Management Provider | Jira, Trello |

#### Infrastructure Layer
| 组件 | 职责 |
|------|------|
| PostgreSQL | 工作流状态、执行历史、配置存储 |
| Redis | 缓存、分布式锁 |
| Message Queue | 异步任务队列（RabbitMQ/Kafka） |

---

## 3. 核心设计

### 3.1 工作流定义（YAML）

```yaml
workflow:
  name: "自动部署流程"
  description: "从代码提交到生产环境部署的完整流程"
  trigger:
    - type: webhook
      event: github.push
      branch: main
    - type: cron
      expression: "0 2 * * *"  # 每天凌晨 2 点

  variables:
    ENV: production
    TIMEOUT: 300

  steps:
    - id: checkout
      type: git.checkout
      config:
        repository: "{{ repo }}"
        branch: "{{ branch }}"
        path: /tmp/workspace

    - id: test
      type: ci.run_tests
      config:
        command: mvn test
        timeout: "{{ TIMEOUT }}"

    - id: build
      type: docker.build
      config:
        context: /tmp/workspace
        tags: ["myapp:latest", "myapp:{{ version }}"]

    - id: deploy
      type: k8s.deploy
      config:
        namespace: default
        image: myapp:{{ version }}
        replicas: 3
      depends_on: [test, build]

    - id: notify
      type: slack.notify
      config:
        channel: "#deployments"
        message: "部署完成：{{ version }}"
      depends_on: [deploy]
```

### 3.2 自定义任务扩展（Java）

```java
@Component
@TaskType("custom.health_check")
public class HealthCheckTask implements TaskExecutor {

    @Override
    public TaskResult execute(TaskContext context) {
        // 自定义健康检查逻辑
        String endpoint = context.getConfig().getString("endpoint");
        boolean healthy = performHealthCheck(endpoint);

        return healthy
            ? TaskResult.success()
            : TaskResult.failure("健康检查失败");
    }
}
```

### 3.3 事件模型

```java
// 事件定义
public class WorkflowEvent {
    private String eventId;
    private EventType type;  // WORKFLOW_STARTED, STEP_COMPLETED, STEP_FAILED, etc.
    private String workflowId;
    private String stepId;
    private Map<String, Object> payload;
    private Instant timestamp;
}

// 事件监听器
@Component
public class DeploymentEventListener {

    @EventListener
    public void onDeploymentCompleted(WorkflowEvent event) {
        // 处理部署完成事件
    }
}
```

---

## 4. 接口设计

### 4.1 REST API

| 端点 | 方法 | 描述 |
|------|------|------|
| `/api/workflows` | GET | 获取工作流列表 |
| `/api/workflows` | POST | 创建工作流 |
| `/api/workflows/{id}` | GET | 获取工作流详情 |
| `/api/workflows/{id}/execute` | POST | 执行工作流 |
| `/api/workflows/{id}/ executions` | GET | 获取执行历史 |
| `/api/executions/{id}` | GET | 获取执行详情 |
| `/api/executions/{id}/cancel` | POST | 取消执行 |
| `/api/providers` | GET | 获取已配置的 Provider |
| `/api/providers/{type}/test` | POST | 测试 Provider 连接 |

### 4.2 CLI 命令

```bash
# 工作流管理
devops-agent workflow list
devops-agent workflow create <file.yaml>
devops-agent workflow show <id>
devops-agent workflow delete <id>

# 执行控制
devops-agent run <workflow-id> [--vars key=value]
devops-agent executions list [--workflow=<id>]
devops-agent executions show <execution-id>
devops-agent executions cancel <execution-id>

# 系统管理
devops-agent provider list
devops-agent provider configure <type>
devops-agent server start
devops-agent server status
```

---

## 5. 数据模型

### 5.1 核心表结构

```sql
-- 工作流定义
CREATE TABLE workflows (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    yaml_definition TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 执行实例
CREATE TABLE executions (
    id UUID PRIMARY KEY,
    workflow_id UUID REFERENCES workflows(id),
    status VARCHAR(50),  -- PENDING, RUNNING, SUCCESS, FAILED, CANCELLED
    triggered_by VARCHAR(100),  -- MANUAL, SCHEDULED, WEBHOOK
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT
);

-- 步骤执行记录
CREATE TABLE execution_steps (
    id UUID PRIMARY KEY,
    execution_id UUID REFERENCES executions(id),
    step_id VARCHAR(100),
    step_type VARCHAR(100),
    status VARCHAR(50),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    output TEXT,
    error_message TEXT
);

-- Provider 配置
CREATE TABLE providers (
    id UUID PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    config JSONB NOT NULL,
    encrypted_credentials JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 6. 安全设计

### 6.1 认证与授权
- **认证**: JWT Token + OAuth2（支持 GitHub/GitLab 登录）
- **授权**: RBAC 角色权限控制（Admin, Developer, Viewer）
- **API Key**: 为外部系统提供 API Key 认证

### 6.2 凭据管理
- 敏感配置（API Key、Token、密码）使用 Jasypt 加密存储
- 支持集成外部密钥管理服务（HashiCorp Vault、AWS Secrets Manager）

### 6.3 审计日志
- 所有操作记录审计日志
- 支持操作追溯和合规性检查

---

## 7. 部署架构

### 7.1 Docker Compose 部署

```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DB_HOST=postgres
      - REDIS_HOST=redis
    depends_on:
      - postgres
      - redis
      - rabbitmq

  postgres:
    image: postgres:15
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine

  rabbitmq:
    image: rabbitmq:3-management

  web:
    build: ./web
    ports:
      - "3000:3000"
    depends_on:
      - app

volumes:
  postgres_data:
```

---

## 8. 开发计划（阶段划分）

### Phase 1: 核心框架
- Spring Boot 项目骨架
- 工作流 YAML 解析引擎
- 基础任务类型实现（Shell、Git）
- 简单的 CLI 界面

### Phase 2: 持久化与调度
- PostgreSQL 数据层
- 执行状态管理
- Cron 定时调度器
- Webhook 事件接收

### Phase 3: Provider 集成
- GitHub/GitLab Provider
- CI/CD Provider
- Docker/K8s Provider
- 通知 Provider（Slack/邮件）

### Phase 4: Web 控制台
- React 前端框架
- 工作流可视化编辑器
- 执行状态实时监控
- 日志查看器

### Phase 5: AI 增强
- 自然语言命令解析
- 工作流模板生成
- 错误诊断与建议

### Phase 6: 完善与优化
- 认证授权系统
- 审计日志
- 性能优化
- 文档完善

---

## 9. 技术选型汇总

| 类别 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2+ |
| 数据库 | PostgreSQL | 15+ |
| 缓存 | Redis | 7+ |
| 消息队列 | RabbitMQ | 3.x |
| 前端框架 | React | 18+ |
| CLI 框架 | Picocli | 4.x |
| YAML 解析 | SnakeYAML | 2.x |
| 容器 | Docker | latest |
| 编排 | Docker Compose | v3 |

---

## 10. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Provider API 变更 | 集成失效 | 抽象 Provider 接口，编写单元测试 |
| 工作流循环依赖 | 执行死锁 | 执行前进行 DAG 验证 |
| 长时间运行任务 | 资源占用 | 实现任务超时和取消机制 |
| 凭据泄露 | 安全风险 | 加密存储，最小权限原则 |

---

## 批准记录

- [x] 用户批准：2026-03-13
- [ ] Spec Review 批准：待执行
- [ ] 最终批准：待执行
