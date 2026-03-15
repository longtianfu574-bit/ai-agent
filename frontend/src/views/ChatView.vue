<template>
  <div class="chat-view">
    <el-container>
      <!-- 聊天主区域 -->
      <el-main>
        <div class="chat-header">
          <h2>智能对话</h2>
          <el-button @click="clearChat" :disabled="chatStore.messages.length === 0">
            <el-icon><Delete /></el-icon>
            清空对话
          </el-button>
        </div>

        <!-- 消息列表 -->
        <div class="message-list" ref="messageListRef">
          <div
            v-for="message in chatStore.messages"
            :key="message.id"
            :class="['message', message.role]"
          >
            <div class="message-avatar">
              <el-icon v-if="message.role === 'user'"><User /></el-icon>
              <el-icon v-else-if="message.role === 'assistant'"><Monitor /></el-icon>
              <el-icon v-else><InfoFilled /></el-icon>
            </div>
            <div class="message-content">
              <div class="message-text">{{ message.content }}</div>
              <div class="message-time">{{ formatTime(message.timestamp) }}</div>
            </div>
          </div>

          <!-- 加载状态 -->
          <div v-if="chatStore.isLoading" class="message assistant loading">
            <div class="message-avatar">
              <el-icon><Monitor /></el-icon>
            </div>
            <div class="message-content">
              <el-skeleton :rows="2" animated />
            </div>
          </div>
        </div>
      </el-main>

      <!-- 输入区域 -->
      <el-footer height="auto">
        <div class="input-container">
          <el-input
            v-model="inputMessage"
            :disabled="chatStore.isLoading"
            placeholder="输入消息，按 Enter 发送..."
            @keydown.enter.exact="sendMessage"
            :rows="2"
            type="textarea"
            resize="none"
          />
          <el-button
            type="primary"
            :loading="chatStore.isLoading"
            @click="sendMessage"
            :disabled="!inputMessage.trim()"
          >
            <el-icon><Promotion /></el-icon>
            发送
          </el-button>
        </div>
      </el-footer>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { useChatStore } from '@/stores/chat'
import dayjs from 'dayjs'

const chatStore = useChatStore()
const inputMessage = ref('')
const messageListRef = ref<HTMLElement | null>(null)

const formatTime = (timestamp: number) => {
  return dayjs(timestamp).format('HH:mm:ss')
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || chatStore.isLoading) return

  const content = inputMessage.value.trim()
  inputMessage.value = ''

  try {
    await chatStore.sendMessage(content, true)
    await scrollToBottom()
  } catch (error) {
    console.error('发送消息失败:', error)
  }
}

const clearChat = () => {
  chatStore.clearChat()
}
</script>

<style scoped>
.chat-view {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.el-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.el-main {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #fff;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.chat-header h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.message-list {
  max-height: calc(100vh - 250px);
  overflow-y: auto;
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding: 15px;
  border-radius: 8px;
  background: #f5f7fa;
}

.message.user {
  flex-direction: row-reverse;
  background: #ecf5ff;
}

.message.assistant {
  background: #f0f9eb;
}

.message.system {
  background: #fef0f0;
  border-left: 3px solid #f56c6c;
}

.message-avatar {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #409EFF;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.message.user .message-avatar {
  background: #67c23a;
}

.message.system .message-avatar {
  background: #f56c6c;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  color: #333;
}

.message-time {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}

.el-footer {
  padding: 20px;
  background: #fff;
  border-top: 1px solid #eee;
}

.input-container {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-container :deep(.el-textarea__inner) {
  flex: 1;
}

.el-footer .el-button {
  flex-shrink: 0;
  padding: 12px 24px;
}
</style>
