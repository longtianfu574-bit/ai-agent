# AI Agent 前端界面实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 Vue 3 前端界面，包括聊天界面、技能管理、文档管理和记忆管理功能。

**Architecture:** Vue 3 单页应用，使用 Element Plus 组件库，Pinia 状态管理，Vue Router 路由，Axios HTTP 客户端。

**Tech Stack:** Vue 3, TypeScript, Element Plus, Pinia, Vue Router, Axios, Vite

---

## 前置条件

**依赖计划:**
- `2026-03-13-ai-agent-foundation-plan.md` - 基础架构计划（前端项目骨架已完成）

---

## Chunk 1: 项目配置和基础组件

### Task 1: 完善前端项目配置

**Files:**
- Modify: `frontend/tsconfig.json`
- Create: `frontend/src/router/index.ts`
- Create: `frontend/src/stores/index.ts`
- Create: `frontend/src/api/client.ts`

- [ ] **Step 1: 创建 Vue Router 配置**

```typescript
// frontend/src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Chat',
    component: () => import('@/views/ChatView.vue')
  },
  {
    path: '/skills',
    name: 'Skills',
    component: () => import('@/views/SkillsView.vue')
  },
  {
    path: '/documents',
    name: 'Documents',
    component: () => import('@/views/DocumentsView.vue')
  },
  {
    path: '/memory',
    name: 'Memory',
    component: () => import('@/views/MemoryView.vue')
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/SettingsView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
```

- [ ] **Step 2: 创建 Pinia 状态管理**

```typescript
// frontend/src/stores/index.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
}

export interface Skill {
  id: string
  name: string
  description: string
  category: string
  requiresConfirmation: boolean
}

export const useChatStore = defineStore('chat', () => {
  const messages = ref<Message[]>([])
  const isLoading = ref(false)
  const sessionId = ref<string | null>(null)

  const addMessage = (role: 'user' | 'assistant', content: string) => {
    messages.value.push({
      id: Date.now().toString(),
      role,
      content,
      timestamp: new Date()
    })
  }

  const clearMessages = () => {
    messages.value = []
    sessionId.value = null
  }

  return { messages, isLoading, sessionId, addMessage, clearMessages }
})

export const useSkillStore = defineStore('skills', () => {
  const skills = ref<Skill[]>([])
  const selectedSkill = ref<Skill | null>(null)

  const setSkills = (newSkills: Skill[]) => {
    skills.value = newSkills
  }

  const selectSkill = (skill: Skill) => {
    selectedSkill.value = skill
  }

  return { skills, selectedSkill, setSkills, selectSkill }
})
```

- [ ] **Step 3: 创建 API 客户端**

```typescript
// frontend/src/api/client.ts
import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Health check
export const healthCheck = () => apiClient.get('/health')

// Chat API
export const sendChat = (message: string, sessionId?: string) =>
  apiClient.post('/chat', { message, sessionId })

// Skills API
export const listSkills = () => apiClient.get('/skills')
export const executeSkill = (skillId: string, params: Record<string, any>) =>
  apiClient.post('/skills/execute', { skillId, parameters: params })

// RAG API
export const indexDocument = (content: string, source?: string) =>
  apiClient.post('/rag/index', { content, source })
export const searchDocuments = (query: string, topK?: number) =>
  apiClient.post('/rag/search', { query, topK })

// Memory API
export const listMemories = () => apiClient.get('/memory')
export const createMemory = (type: string, content: string) =>
  apiClient.post('/memory', { type, content })
export const deleteMemory = (id: string) =>
  apiClient.delete(`/memory/${id}`)

export default apiClient
```

- [ ] **Step 4: 更新 main.ts 使用 router 和 store**

```typescript
// frontend/src/main.ts
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router'
import App from './App.vue'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.mount('#app')
```

- [ ] **Step 5: 提交**

```bash
cd frontend
git add src/router/ src/stores/ src/api/
git commit -m "feat: setup router, store, and API client"
```

---

## Chunk 2: 聊天界面

### Task 2: 创建聊天界面组件

**Files:**
- Create: `frontend/src/views/ChatView.vue`
- Create: `frontend/src/components/ChatMessage.vue`
- Create: `frontend/src/components/ChatInput.vue`
- Create: `frontend/src/components/ChatSidebar.vue`

- [ ] **Step 1: 创建聊天主视图**

```vue
<!-- frontend/src/views/ChatView.vue -->
<template>
  <div class="chat-view">
    <el-container>
      <el-aside width="300px">
        <ChatSidebar />
      </el-aside>
      <el-container>
        <el-main class="chat-main">
          <div class="messages-container">
            <ChatMessage
              v-for="message in chatStore.messages"
              :key="message.id"
              :message="message"
            />
          </div>
          <div class="input-container">
            <ChatInput
              @send="handleSend"
              :disabled="chatStore.isLoading"
            />
          </div>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { useChatStore } from '@/stores'
import ChatMessage from '@/components/ChatMessage.vue'
import ChatInput from '@/components/ChatInput.vue'
import ChatSidebar from '@/components/ChatSidebar.vue'
import { sendChat } from '@/api/client'

const chatStore = useChatStore()

const handleSend = async (content: string) => {
  chatStore.addMessage('user', content)
  chatStore.isLoading = true

  try {
    const response = await sendChat(content, chatStore.sessionId || undefined)
    const data = response.data

    if (data.sessionId) {
      chatStore.sessionId = data.sessionId
    }

    chatStore.addMessage('assistant', data.message)
  } catch (error) {
    chatStore.addMessage('assistant', 'Error: Failed to send message')
  } finally {
    chatStore.isLoading = false
  }
}
</script>

<style scoped>
.chat-view {
  height: 100vh;
}

.chat-main {
  display: flex;
  flex-direction: column;
  padding: 0;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.input-container {
  border-top: 1px solid #e0e0e0;
  padding: 20px;
}
</style>
```

- [ ] **Step 2: 创建消息组件**

```vue
<!-- frontend/src/components/ChatMessage.vue -->
<template>
  <div :class="['message', message.role]">
    <div class="avatar">
      <el-icon v-if="message.role === 'user'"><User /></el-icon>
      <el-icon v-else><ChatDotRound /></el-icon>
    </div>
    <div class="content">
      <div class="text">{{ message.content }}</div>
      <div class="timestamp">{{ formatTime(message.timestamp) }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { User, ChatDotRound } from '@element-plus/icons-vue'

interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
}

defineProps<{
  message: Message
}>()

const formatTime = (date: Date) => {
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.message {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.message.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #409EFF;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message.user .avatar {
  background: #67C23A;
}

.content {
  max-width: 70%;
}

.text {
  background: #f5f5f5;
  padding: 12px 16px;
  border-radius: 8px;
  line-height: 1.5;
}

.message.user .text {
  background: #409EFF;
  color: white;
}

.timestamp {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  text-align: right;
}
</style>
```

- [ ] **Step 3: 创建输入组件**

```vue
<!-- frontend/src/components/ChatInput.vue -->
<template>
  <div class="chat-input">
    <el-input
      v-model="input"
      type="textarea"
      :rows="3"
      placeholder="输入消息... (Shift+Enter 换行)"
      :disabled="disabled"
      @keydown.enter.exact.prevent="handleSend"
    />
    <div class="actions">
      <el-button
        type="primary"
        :loading="disabled"
        @click="handleSend"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const input = ref('')

const emit = defineEmits<{
  send: [content: string]
}>()

defineProps<{
  disabled: boolean
}>()

const handleSend = () => {
  if (input.value.trim() && !this.disabled) {
    emit('send', input.value.trim())
    input.value = ''
  }
}
</script>

<style scoped>
.chat-input {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.actions {
  display: flex;
  justify-content: flex-end;
}
</style>
```

- [ ] **Step 4: 创建侧边栏组件**

```vue
<!-- frontend/src/components/ChatSidebar.vue -->
<template>
  <div class="chat-sidebar">
    <div class="header">
      <el-button type="primary" plain @click="clearChat">
        <el-icon><Delete /></el-icon>
        清空对话
      </el-button>
    </div>

    <div class="section">
      <h4>快捷技能</h4>
      <el-space direction="vertical" style="width: 100%">
        <el-tag
          v-for="skill in quickSkills"
          :key="skill.id"
          size="small"
          closable
        >
          {{ skill.name }}
        </el-tag>
      </el-space>
    </div>

    <div class="section">
      <h4>最近记忆</h4>
      <ul class="memory-list">
        <li v-for="mem in recentMemories" :key="mem.id">
          {{ mem.content.substring(0, 30) }}...
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { useChatStore } from '@/stores'
import { listSkills, listMemories } from '@/api/client'

const chatStore = useChatStore()
const quickSkills = ref<any[]>([])
const recentMemories = ref<any[]>([])

const clearChat = () => {
  chatStore.clearMessages()
}

onMounted(async () => {
  const skillsRes = await listSkills()
  quickSkills.value = skillsRes.data.slice(0, 5)

  const memRes = await listMemories()
  recentMemories.value = memRes.data.slice(0, 3)
})
</script>

<style scoped>
.chat-sidebar {
  padding: 20px;
  border-right: 1px solid #e0e0e0;
  height: 100%;
}

.header {
  margin-bottom: 20px;
}

.section {
  margin-bottom: 24px;
}

.section h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #666;
}

.memory-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.memory-list li {
  padding: 8px 0;
  font-size: 13px;
  color: #666;
  border-bottom: 1px solid #eee;
}
</style>
```

- [ ] **Step 5: 提交**

```bash
cd frontend
git add src/views/ChatView.vue src/components/
git commit -m "feat: implement chat interface components"
```

---

## Chunk 3: 技能管理界面

### Task 3: 创建技能管理界面

**Files:**
- Create: `frontend/src/views/SkillsView.vue`
- Create: `frontend/src/components/SkillCard.vue`
- Create: `frontend/src/components/SkillDetail.vue`

- [ ] **Step 1: 创建技能主视图**

```vue
<!-- frontend/src/views/SkillsView.vue -->
<template>
  <div class="skills-view">
    <el-header class="view-header">
      <h2>技能管理</h2>
      <el-button type="primary" @click="refreshSkills">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </el-header>

    <el-main>
      <div class="skills-grid">
        <SkillCard
          v-for="skill in skills"
          :key="skill.id"
          :skill="skill"
          @select="$emit('select', $event)"
        />
      </div>
    </el-main>

    <el-drawer
      v-model="detailVisible"
      title="技能详情"
      size="50%"
    >
      <SkillDetail v-if="selectedSkill" :skill="selectedSkill" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import SkillCard from '@/components/SkillCard.vue'
import SkillDetail from '@/components/SkillDetail.vue'
import { listSkills } from '@/api/client'

const skills = ref<any[]>([])
const selectedSkill = ref<any>(null)
const detailVisible = ref(false)

const refreshSkills = async () => {
  const res = await listSkills()
  skills.value = res.data
}

onMounted(() => {
  refreshSkills()
})
</script>

<style scoped>
.skills-view {
  height: 100vh;
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e0e0e0;
}

.skills-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}
</style>
```

- [ ] **Step 2: 创建技能卡片组件**

```vue
<!-- frontend/src/components/SkillCard.vue -->
<template>
  <el-card class="skill-card" shadow="hover" @click="handleClick">
    <template #header>
      <div class="card-header">
        <span class="name">{{ skill.name }}</span>
        <el-tag size="small">{{ skill.category }}</el-tag>
      </div>
    </template>
    <p class="description">{{ skill.description }}</p>
    <div class="meta">
      <el-tag
        v-if="skill.requiresConfirmation"
        type="warning"
        size="small"
      >
        需要确认
      </el-tag>
      <span class="triggers">
        {{ skill.triggers?.slice(0, 3).join(', ') }}
      </span>
    </div>
  </el-card>
</template>

<script setup lang="ts">
interface Skill {
  id: string
  name: string
  description: string
  category: string
  requiresConfirmation: boolean
  triggers?: string[]
}

defineProps<{
  skill: Skill
}>()

const emit = defineEmits<{
  select: [skill: Skill]
}>()

const handleClick = () => {
  emit('select', props.skill)
}
</script>

<style scoped>
.skill-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.skill-card:hover {
  transform: translateY(-4px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.name {
  font-weight: bold;
  font-size: 16px;
}

.description {
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  margin: 12px 0;
}

.meta {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.triggers {
  font-size: 12px;
  color: #999;
}
</style>
```

- [ ] **Step 3: 提交**

```bash
cd frontend
git add src/views/SkillsView.vue src/components/Skill*.vue
git commit -m "feat: implement skills management interface"
```

---

## Chunk 4: 文档和记忆界面

### Task 4: 创建文档管理界面

**Files:**
- Create: `frontend/src/views/DocumentsView.vue`
- Create: `frontend/src/views/MemoryView.vue`
- Create: `frontend/src/views/SettingsView.vue`

- [ ] **Step 1: 创建文档视图**

```vue
<!-- frontend/src/views/DocumentsView.vue -->
<template>
  <div class="documents-view">
    <el-header class="view-header">
      <h2>文档管理</h2>
    </el-header>

    <el-main>
      <el-card>
        <h3>上传文档</h3>
        <el-input
          v-model="content"
          type="textarea"
          :rows="6"
          placeholder="粘贴文档内容..."
        />
        <el-input
          v-model="source"
          placeholder="来源 (可选)"
          style="margin-top: 12px"
        />
        <el-button
          type="primary"
          style="margin-top: 12px"
          :loading="loading"
          @click="handleIndex"
        >
          索引文档
        </el-button>
      </el-card>

      <el-card style="margin-top: 20px">
        <h3>搜索文档</h3>
        <el-input
          v-model="searchQuery"
          placeholder="搜索内容..."
          @change="handleSearch"
        >
          <template #append>
            <el-button @click="handleSearch">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>

        <div v-if="searchResults.length" class="results">
          <div
            v-for="result in searchResults"
            :key="result.id"
            class="result-item"
          >
            <div class="score">Score: {{ result.score.toFixed(3) }}</div>
            <p>{{ result.content }}</p>
          </div>
        </div>
      </el-card>
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { indexDocument, searchDocuments } from '@/api/client'

const content = ref('')
const source = ref('')
const searchQuery = ref('')
const searchResults = ref<any[]>([])
const loading = ref(false)

const handleIndex = async () => {
  if (!content.value.trim()) return

  loading.value = true
  try {
    await indexDocument(content.value, source.value || undefined)
    ElMessage.success('文档索引成功')
    content.value = ''
  } catch (error) {
    ElMessage.error('索引失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  if (!searchQuery.value.trim()) return

  const res = await searchDocuments(searchQuery.value, 10)
  searchResults.value = res.data
}
</script>

<style scoped>
.documents-view {
  height: 100vh;
}

.view-header {
  border-bottom: 1px solid #e0e0e0;
}

.results {
  margin-top: 20px;
}

.result-item {
  padding: 12px;
  border-bottom: 1px solid #eee;
}

.score {
  font-size: 12px;
  color: #409EFF;
  margin-bottom: 4px;
}
</style>
```

- [ ] **Step 2: 创建记忆视图**

```vue
<!-- frontend/src/views/MemoryView.vue -->
<template>
  <div class="memory-view">
    <el-header class="view-header">
      <h2>记忆管理</h2>
    </el-header>

    <el-main>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card>
            <h3>创建记忆</h3>
            <el-select v-model="memoryType" placeholder="类型" style="width: 100%">
              <el-option label="用户偏好" value="USER_PREFERENCE" />
              <el-option label="项目知识" value="PROJECT_KNOWLEDGE" />
              <el-option label="技能学习" value="SKILL_LEARNING" />
              <el-option label="对话摘要" value="CONVERSATION_SUMMARY" />
            </el-select>
            <el-input
              v-model="memoryContent"
              type="textarea"
              :rows="4"
              placeholder="记忆内容"
              style="margin-top: 12px"
            />
            <el-button
              type="primary"
              style="margin-top: 12px"
              @click="handleCreate"
            >
              创建
            </el-button>
          </el-card>
        </el-col>

        <el-col :span="16">
          <el-card>
            <h3>记忆列表</h3>
            <el-table :data="memories" style="width: 100%">
              <el-table-column prop="type" label="类型" width="120" />
              <el-table-column prop="content" label="内容" />
              <el-table-column prop="strength" label="强度" width="80">
                <template #default="{ row }">
                  <el-progress :percentage="row.strength * 100" :status="row.strength < 0.3 ? 'exception' : undefined" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button
                    type="danger"
                    size="small"
                    @click="handleDelete(row.id)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listMemories, createMemory, deleteMemory } from '@/api/client'

const memoryType = ref('USER_PREFERENCE')
const memoryContent = ref('')
const memories = ref<any[]>([])

const loadMemories = async () => {
  const res = await listMemories()
  memories.value = res.data
}

const handleCreate = async () => {
  await createMemory(memoryType.value, memoryContent.value)
  memoryContent.value = ''
  loadMemories()
}

const handleDelete = async (id: string) => {
  await deleteMemory(id)
  loadMemories()
}

onMounted(() => {
  loadMemories()
})
</script>

<style scoped>
.memory-view {
  height: 100vh;
}

.view-header {
  border-bottom: 1px solid #e0e0e0;
}
</style>
```

- [ ] **Step 3: 创建设置视图**

```vue
<!-- frontend/src/views/SettingsView.vue -->
<template>
  <div class="settings-view">
    <el-header class="view-header">
      <h2>设置</h2>
    </el-header>

    <el-main>
      <el-card>
        <h3>API 配置</h3>
        <el-form label-width="150px">
          <el-form-item label="API Key">
            <el-input v-model="apiKey" type="password" />
          </el-form-item>
          <el-form-item label="Base URL">
            <el-input v-model="baseUrl" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary">保存</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const apiKey = ref('')
const baseUrl = ref('/api')
</script>

<style scoped>
.settings-view {
  height: 100vh;
}

.view-header {
  border-bottom: 1px solid #e0e0e0;
}
</style>
```

- [ ] **Step 4: 更新 App.vue 添加导航**

```vue
<!-- frontend/src/App.vue -->
<template>
  <el-container>
    <el-header class="app-header">
      <div class="logo">AI Agent</div>
      <el-menu
        mode="horizontal"
        :default-active="activeMenu"
        router
        style="flex: 1; border: none; background: transparent"
      >
        <el-menu-item index="/">聊天</el-menu-item>
        <el-menu-item index="/skills">技能</el-menu-item>
        <el-menu-item index="/documents">文档</el-menu-item>
        <el-menu-item index="/memory">记忆</el-menu-item>
        <el-menu-item index="/settings">设置</el-menu-item>
      </el-menu>
    </el-header>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  background: #409EFF;
  color: white;
  padding: 0 20px;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  margin-right: 40px;
}

.app-main {
  padding: 0;
}
</style>
```

- [ ] **Step 5: 提交**

```bash
cd frontend
git add src/views/DocumentsView.vue src/views/MemoryView.vue src/views/SettingsView.vue src/App.vue
git commit -m "feat: implement documents, memory, and settings views"
```

---

## 验收标准

1. 聊天界面可以发送消息并显示响应
2. 技能界面显示所有可用技能
3. 文档界面支持索引和搜索
4. 记忆界面支持创建和删除
5. 路由导航正常工作

---

## 后续工作

1. 技能执行界面交互
2. 对话历史持久化
3. 主题切换
4. 响应式移动端适配
