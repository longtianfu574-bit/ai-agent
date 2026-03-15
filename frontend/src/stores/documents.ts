import { defineStore } from 'pinia'
import { ref } from 'vue'
import apiClient from '@/api/client'

export interface Document {
  id: string
  content: string
  source: string
  metadata?: Record<string, any>
  createdAt?: number
}

export interface IndexRequest {
  documents: Array<{
    content: string
    source?: string
    metadata?: Record<string, any>
  }>
}

export const useDocumentsStore = defineStore('documents', () => {
  const documents = ref<Document[]>([])
  const isLoading = ref(false)
  const isIndexing = ref(false)

  // 搜索文档
  async function searchDocuments(query: string, topK: number = 5) {
    isLoading.value = true
    try {
      const response = await apiClient.post('/rag/search', {
        query,
        topK,
      })
      documents.value = response.data.documents || []
      return documents.value
    } finally {
      isLoading.value = false
    }
  }

  // 批量索引文档
  async function indexDocuments(docs: IndexRequest['documents']) {
    isIndexing.value = true
    try {
      const response = await apiClient.post('/rag/batch/index-documents', {
        documents: docs,
      })
      return response.data
    } finally {
      isIndexing.value = false
    }
  }

  // 清空文档列表
  function clearDocuments() {
    documents.value = []
  }

  return {
    documents,
    isLoading,
    isIndexing,
    searchDocuments,
    indexDocuments,
    clearDocuments,
  }
})
