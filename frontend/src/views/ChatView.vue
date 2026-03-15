<template>
  <div class="chat-view">
    <!-- 顶部导航栏 -->
    <header class="chat-header">
      <div class="header-left">
        <h1 class="page-title">
          <el-icon class="title-icon"><ChatDotRound /></el-icon>
          智能对话
        </h1>
        <el-tag type="info" size="small" effect="plain" class="model-tag">
          <el-icon><Cpu /></el-icon>
          Qwen-3.5-MCP
        </el-tag>
      </div>
      <div class="header-actions">
        <el-tooltip content="清空当前对话" placement="bottom">
          <el-button
            circle
            @click="clearChat"
            :disabled="chatStore.messages.length === 0"
            class="action-btn"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="导出对话记录" placement="bottom">
          <el-button circle class="action-btn">
            <el-icon><Download /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
    </header>

    <!-- 消息列表 -->
    <div class="message-list" ref="messageListRef">
      <div class="messages-container">
        <!-- 欢迎消息 -->
        <div v-if="chatStore.messages.length === 0" class="welcome-card">
          <div class="welcome-icon">
            <el-icon><Monitor /></el-icon>
          </div>
          <h2>欢迎使用 AI Agent</h2>
          <p>我是您的智能助手，可以帮您完成各种任务</p>
          <div class="welcome-suggestions">
            <el-tag
              v-for="suggestion in suggestions"
              :key="suggestion"
              class="suggestion-tag"
              @click="selectSuggestion(suggestion)"
            >
              {{ suggestion }}
            </el-tag>
          </div>
        </div>

        <!-- 消息气泡 -->
        <transition-group name="message-fade">
          <div
            v-for="message in chatStore.messages"
            :key="message.id"
            :class="['message-wrapper', message.role]"
          >
            <div class="message-row">
              <div class="message-avatar">
                <div :class="['avatar', message.role]">
                  <el-icon v-if="message.role === 'user'"><User /></el-icon>
                  <el-icon v-else-if="message.role === 'assistant'"><Monitor /></el-icon>
                  <el-icon v-else><InfoFilled /></el-icon>
                </div>
              </div>
              <div class="message-body">
                <div class="message-header">
                  <span class="sender-name">
                    {{ message.role === 'user' ? '您' : 'AI 助手' }}
                  </span>
                  <span class="message-time">{{ formatTime(message.timestamp) }}</span>
                </div>
                <div :class="['message-bubble', message.role]">
                  <div class="message-content">{{ message.content }}</div>
                </div>
              </div>
            </div>
          </div>
        </transition-group>

        <!-- 加载状态 -->
        <div v-if="chatStore.isLoading" class="message-wrapper assistant loading">
          <div class="message-row">
            <div class="message-avatar">
              <div class="avatar assistant">
                <el-icon><Monitor /></el-icon>
              </div>
            </div>
            <div class="message-body">
              <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <div class="input-wrapper">
        <el-input
          v-model="inputMessage"
          :disabled="chatStore.isLoading"
          placeholder="输入消息，按 Enter 发送..."
          @keydown.enter.exact="sendMessage"
          :rows="1"
          type="textarea"
          resize="none"
          class="chat-input"
        />
        <el-button
          type="primary"
          :loading="chatStore.isLoading"
          @click="sendMessage"
          :disabled="!inputMessage.trim()"
          class="send-btn"
        >
          <el-icon><Promotion /></el-icon>
          发送
        </el-button>
      </div>
      <div class="input-tips">
        <el-text type="info" size="small">
          <el-icon><InfoFilled /></el-icon>
          Enter 发送，Shift+Enter 换行
        </el-text>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { useChatStore } from '@/stores/chat'
import dayjs from 'dayjs'
import {
  ChatDotRound, Monitor, User, InfoFilled, Delete, Download,
  Promotion, Cpu
} from '@element-plus/icons-vue'

const chatStore = useChatStore()
const inputMessage = ref('')
const messageListRef = ref<HTMLElement | null>(null)

const suggestions = [
  '今日天气如何？',
  '帮我查询北京天气',
  '什么是 RAG？',
  '介绍一下 MCP 协议',
]

const formatTime = (timestamp: number) => {
  return dayjs(timestamp).format('HH:mm')
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTo({
      top: messageListRef.value.scrollHeight,
      behavior: 'smooth'
    })
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

const selectSuggestion = (text: string) => {
  inputMessage.value = text
  sendMessage()
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
  background: #fff;
}

/* 顶部导航栏 */
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 28px;
  border-bottom: 1px solid #eef2f6;
  background: #fff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #1a1a2e;
}

.title-icon {
  font-size: 28px;
  color: #667eea;
}

.model-tag {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  width: 40px;
  height: 40px;
  border: 1px solid #eef2f6;
  background: #f8fafc;
  transition: all 0.3s;
}

.action-btn:hover:not(:disabled) {
  background: #667eea;
  border-color: #667eea;
  color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

/* 消息列表 */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px 28px;
  scroll-behavior: smooth;
}

.messages-container {
  max-width: 900px;
  margin: 0 auto;
}

/* 欢迎卡片 */
.welcome-card {
  text-align: center;
  padding: 60px 20px;
}

.welcome-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  border-radius: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 30px rgba(102, 126, 234, 0.3);
}

.welcome-icon .el-icon {
  font-size: 40px;
  color: #fff;
}

.welcome-card h2 {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 12px;
}

.welcome-card p {
  font-size: 16px;
  color: #64748b;
  margin: 0 0 32px;
}

.welcome-suggestions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
}

.suggestion-tag {
  padding: 10px 20px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  background: #f1f5f9;
  color: #475569;
  border: 1px solid transparent;
}

.suggestion-tag:hover {
  background: #667eea;
  color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

/* 消息气泡 */
.message-wrapper {
  margin-bottom: 24px;
  opacity: 0;
  animation: messageSlideIn 0.3s ease-out forwards;
}

@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-row {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.message-wrapper.user .message-row {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.avatar {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.avatar.user {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #fff;
}

.avatar.assistant {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.avatar.system {
  background: linear-gradient(135deg, #f56c6c 0%, #e74239 100%);
  color: #fff;
}

.message-body {
  flex: 1;
  min-width: 0;
  max-width: 70%;
}

.message-wrapper.user .message-body {
  max-width: none;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.sender-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
}

.message-time {
  font-size: 12px;
  color: #94a3b8;
}

.message-bubble {
  padding: 16px 20px;
  border-radius: 18px;
  line-height: 1.7;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.message-bubble.assistant {
  background: #f8fafc;
  color: #334155;
  border-bottom-left-radius: 4px;
}

.message-bubble.user {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-content {
  white-space: pre-wrap;
  word-break: break-word;
}

/* 输入区域 */
.input-area {
  padding: 20px 28px;
  background: #fff;
  border-top: 1px solid #eef2f6;
}

.input-wrapper {
  display: flex;
  gap: 12px;
  max-width: 900px;
  margin: 0 auto;
  align-items: flex-end;
}

.chat-input {
  flex: 1;
}

.chat-input :deep(.el-textarea__inner) {
  padding: 16px 20px;
  border-radius: 16px;
  border: 2px solid #eef2f6;
  background: #f8fafc;
  font-size: 15px;
  line-height: 1.6;
  transition: all 0.3s;
  resize: none;
}

.chat-input :deep(.el-textarea__inner):focus {
  border-color: #667eea;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
}

.send-btn {
  height: 52px;
  padding: 0 28px;
  border-radius: 16px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
  transition: all 0.3s;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.send-btn:disabled {
  opacity: 0.5;
}

.input-tips {
  text-align: center;
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

/* 打字动画 */
.typing-indicator {
  display: flex;
  gap: 6px;
  padding: 16px 20px;
  background: #f8fafc;
  border-radius: 18px;
  width: fit-content;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #667eea;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-8px);
    opacity: 1;
  }
}

/* 消息淡入动画 */
.message-fade-enter-active,
.message-fade-leave-active {
  transition: all 0.3s ease;
}

.message-fade-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.message-fade-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}
</style>
