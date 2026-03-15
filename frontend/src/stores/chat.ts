import { defineStore } from 'pinia'
import { ref } from 'vue'
import apiClient from '@/api/client'

export interface Message {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  timestamp: number
  toolCalls?: ToolCall[]
  toolResults?: ToolResult[]
}

export interface ToolCall {
  name: string
  arguments: Record<string, any>
}

export interface ToolResult {
  name: string
  result: any
}

export const useChatStore = defineStore('chat', () => {
  const messages = ref<Message[]>([])
  const isLoading = ref(false)
  const sessionId = ref<string>(localStorage.getItem('chat_session_id') || '')

  // 发送消息
  async function sendMessage(content: string, useTools: boolean = true) {
    if (!content.trim()) return

    // 添加用户消息
    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: content.trim(),
      timestamp: Date.now(),
    }
    messages.value.push(userMessage)

    isLoading.value = true

    try {
      const endpoint = useTools ? '/chat/tools' : '/chat'
      const response = await apiClient.post(endpoint, {
        sessionId: sessionId.value || undefined,
        message: content.trim(),
      })

      // 更新 sessionId
      if (response.data.sessionId) {
        sessionId.value = response.data.sessionId
        localStorage.setItem('chat_session_id', sessionId.value)
      }

      // 添加助手消息
      const assistantMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: response.data.message || '抱歉，我没有理解您的问题。',
        timestamp: Date.now(),
      }
      messages.value.push(assistantMessage)

      return assistantMessage
    } catch (error: any) {
      const errorMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: `发生错误：${error.response?.data?.message || error.message || '请求失败'}`,
        timestamp: Date.now(),
      }
      messages.value.push(errorMessage)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  // 清空对话
  function clearChat() {
    messages.value = []
    sessionId.value = ''
    localStorage.removeItem('chat_session_id')
  }

  // 添加系统消息
  function addSystemMessage(content: string) {
    messages.value.push({
      id: Date.now().toString(),
      role: 'system',
      content,
      timestamp: Date.now(),
    })
  }

  return {
    messages,
    isLoading,
    sessionId,
    sendMessage,
    clearChat,
    addSystemMessage,
  }
})
