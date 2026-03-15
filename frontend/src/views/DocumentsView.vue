<template>
  <div class="documents-view">
    <!-- 页面头部 -->
    <header class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          <el-icon class="title-icon"><Document /></el-icon>
          文档管理
        </h1>
        <p class="page-desc">基于 RAG 技术的文档索引与语义搜索</p>
      </div>
      <el-button type="primary" @click="showIndexDialog = true" class="upload-btn">
        <el-icon><Upload /></el-icon>
        索引新文档
      </el-button>
    </header>

    <!-- 搜索区域 -->
    <div class="search-section">
      <div class="search-box-wrapper">
        <el-input
          v-model="searchQuery"
          placeholder="输入关键词搜索文档内容..."
          :disabled="documentsStore.isLoading"
          @keydown.enter="handleSearch"
          class="search-input"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button
              type="primary"
              @click="handleSearch"
              :loading="documentsStore.isLoading"
              class="search-btn"
            >
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
          </template>
        </el-input>
        <div class="search-tips">
          <el-text type="info" size="small">
            <el-icon><InfoFilled /></el-icon>
            支持语义搜索，可输入自然语言问题进行查询
          </el-text>
        </div>
      </div>
    </div>

    <!-- 搜索结果 -->
    <main class="results-section">
      <div v-if="documentsStore.isLoading" class="loading-state">
        <el-skeleton :rows="5" animated />
      </div>

      <el-empty
        v-else-if="documentsStore.documents.length === 0 && searched"
        description="未找到相关文档"
        class="empty-state"
      >
        <el-button type="primary" @click="showIndexDialog = true">
          <el-icon><Upload /></el-icon>
          索引文档
        </el-button>
      </el-empty>

      <div v-else-if="documentsStore.documents.length === 0" class="initial-state">
        <div class="empty-icon">
          <el-icon><Document /></el-icon>
        </div>
        <h3>暂无文档</h3>
        <p>开始索引您的第一个文档吧</p>
        <el-button type="primary" @click="showIndexDialog = true">
          <el-icon><Upload /></el-icon>
          索引文档
        </el-button>
      </div>

      <div v-else class="results-list">
        <div class="results-info">
          <el-text type="info">
            找到 {{ documentsStore.documents.length }} 条相关文档
          </el-text>
        </div>
        <transition-group name="list-fade">
          <div
            v-for="(doc, index) in documentsStore.documents"
            :key="doc.id"
            :style="{ animationDelay: `${index * 0.1}s` }"
            class="result-card"
          >
            <div class="result-content">
              <div class="result-header">
                <el-tag type="info" size="small">
                  <el-icon><Link /></el-icon>
                  {{ doc.source || '未知来源' }}
                </el-tag>
                <el-popover placement="right" trigger="click" width="350">
                  <template #reference>
                    <el-button text type="primary" size="small">
                      <el-icon><Document /></el-icon>
                      元数据
                    </el-button>
                  </template>
                  <div class="metadata-content">
                    <pre>{{ JSON.stringify(doc.metadata, null, 2) || '暂无元数据' }}</pre>
                  </div>
                </el-popover>
              </div>
              <p class="result-text">{{ doc.content }}</p>
            </div>
          </div>
        </transition-group>
      </div>
    </main>

    <!-- 索引对话框 -->
    <el-dialog
      v-model="showIndexDialog"
      title=""
      width="650px"
      :close-on-click-modal="false"
      class="index-dialog"
    >
      <template #header>
        <div class="dialog-header">
          <h2>
            <el-icon><Upload /></el-icon>
            索引新文档
          </h2>
          <p class="dialog-desc">将文档内容添加到向量数据库，支持语义检索</p>
        </div>
      </template>

      <el-form :model="indexForm" label-position="top">
        <el-form-item label="文档内容" required>
          <el-input
            v-model="indexForm.content"
            type="textarea"
            :rows="8"
            placeholder="请输入要索引的文档内容..."
            class="content-input"
          />
        </el-form-item>
        <el-form-item label="来源标识">
          <el-input
            v-model="indexForm.source"
            placeholder="例如：user_upload, manual, knowledge_base"
          >
            <template #prefix>
              <el-icon><Link /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="元数据（可选）">
          <el-input
            v-model="indexForm.metadataStr"
            type="textarea"
            :rows="4"
            placeholder='JSON 格式，例如：{"category": "guide", "version": "1.0"}'
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showIndexDialog = false">取消</el-button>
          <el-button
            type="primary"
            :loading="documentsStore.isIndexing"
            @click="handleIndex"
          >
            <el-icon><Upload /></el-icon>
            {{ documentsStore.isIndexing ? '索引中...' : '确认索引' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useDocumentsStore } from '@/stores/documents'
import {
  Document, Search, Upload, Link, InfoFilled
} from '@element-plus/icons-vue'

const documentsStore = useDocumentsStore()
const searchQuery = ref('')
const showIndexDialog = ref(false)
const searched = ref(false)
const indexForm = ref({
  content: '',
  source: '',
  metadataStr: '',
})

const handleSearch = async () => {
  if (!searchQuery.value.trim()) return
  searched.value = true
  await documentsStore.searchDocuments(searchQuery.value.trim(), 10)
}

const handleIndex = async () => {
  if (!indexForm.value.content.trim()) {
    return
  }

  try {
    let metadata = {}
    if (indexForm.value.metadataStr.trim()) {
      metadata = JSON.parse(indexForm.value.metadataStr)
    }

    await documentsStore.indexDocuments([
      {
        content: indexForm.value.content,
        source: indexForm.value.source || 'manual',
        metadata,
      },
    ])

    showIndexDialog.value = false
    indexForm.value = { content: '', source: '', metadataStr: '' }
    handleSearch()
  } catch (error: any) {
    console.error('索引失败:', error)
  }
}
</script>

<style scoped>
.documents-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f8fafc;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28px 40px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #fff;
}

.header-content {
  flex: 1;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
}

.title-icon {
  font-size: 32px;
}

.page-desc {
  margin: 0;
  font-size: 15px;
  opacity: 0.9;
}

.upload-btn {
  height: 44px;
  padding: 0 24px;
  border-radius: 12px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.3);
  transition: all 0.3s;
}

.upload-btn:hover {
  background: #fff;
  color: #10b981;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

/* 搜索区域 */
.search-section {
  padding: 24px 40px 0;
  background: #fff;
  border-bottom: 1px solid #eef2f6;
}

.search-box-wrapper {
  max-width: 800px;
  margin: 0 auto;
}

.search-input {
  height: 56px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 16px;
  padding: 0 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 2px solid #eef2f6;
}

.search-input :deep(.el-input__inner) {
  font-size: 15px;
}

.search-input :deep(.el-input-group__append) {
  border-radius: 0 16px 16px 0;
  padding: 0 24px;
}

.search-btn {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border: none;
  font-weight: 600;
}

.search-tips {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

/* 结果区域 */
.results-section {
  flex: 1;
  overflow-y: auto;
  padding: 24px 40px;
}

.loading-state,
.empty-state,
.initial-state {
  max-width: 600px;
  margin: 60px auto;
  text-align: center;
}

.initial-state {
  padding: 60px 20px;
}

.empty-icon {
  width: 100px;
  height: 100px;
  margin: 0 auto 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 30px rgba(16, 185, 129, 0.3);
}

.empty-icon .el-icon {
  font-size: 50px;
  color: #fff;
}

.initial-state h3 {
  font-size: 22px;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.initial-state p {
  color: #64748b;
  margin: 0 0 24px;
}

.results-info {
  margin-bottom: 20px;
}

.results-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid #eef2f6;
  transition: all 0.3s;
  opacity: 0;
  animation: resultFadeIn 0.4s ease-out forwards;
}

@keyframes resultFadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.result-card:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  border-color: #10b981;
  transform: translateY(-2px);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.result-text {
  margin: 0;
  font-size: 15px;
  color: #334155;
  line-height: 1.8;
  white-space: pre-wrap;
}

.metadata-content {
  max-height: 300px;
  overflow-y: auto;
}

.metadata-content pre {
  margin: 0;
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 13px;
  background: #f8fafc;
  padding: 16px;
  border-radius: 8px;
}

/* 对话框 */
.index-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

.index-dialog :deep(.el-dialog__header) {
  padding: 0;
}

.dialog-header {
  padding: 24px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #fff;
}

.dialog-header h2 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 8px;
  font-size: 22px;
}

.dialog-desc {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
}

.index-dialog :deep(.el-dialog__body) {
  padding: 28px;
}

.content-input :deep(.el-textarea__inner) {
  border-radius: 12px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #eef2f6;
}

/* 列表动画 */
.list-fade-enter-active {
  transition: all 0.4s ease-out;
}

.list-fade-enter-from {
  opacity: 0;
  transform: translateY(20px);
}
</style>
