<template>
  <div class="documents-view">
    <div class="view-header">
      <h2>文档管理</h2>
      <el-button type="primary" @click="showIndexDialog = true">
        <el-icon><Upload /></el-icon>
        索引文档
      </el-button>
    </div>

    <!-- 搜索区域 -->
    <div class="search-area">
      <el-input
        v-model="searchQuery"
        placeholder="输入搜索关键词..."
        :disabled="documentsStore.isLoading"
        @keydown.enter="handleSearch"
        style="max-width: 600px"
      >
        <template #append>
          <el-button @click="handleSearch" :loading="documentsStore.isLoading">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </template>
      </el-input>
    </div>

    <!-- 搜索结果 -->
    <el-empty v-if="documentsStore.documents.length === 0 && !documentsStore.isLoading" description="暂无搜索结果" />

    <el-table v-else :data="documentsStore.documents" stripe>
      <el-table-column prop="content" label="内容" min-width="400" show-overflow-tooltip />
      <el-table-column prop="source" label="来源" width="150" />
      <el-table-column label="元数据" width="150">
        <template #default="{ row }">
          <el-popover placement="left" trigger="click" width="300">
            <template #reference>
              <el-tag size="small" effect="plain">查看详情</el-tag>
            </template>
            <pre>{{ JSON.stringify(row.metadata, null, 2) }}</pre>
          </el-popover>
        </template>
      </el-table-column>
    </el-table>

    <!-- 索引对话框 -->
    <el-dialog v-model="showIndexDialog" title="索引文档" width="600px">
      <el-form :model="indexForm" label-width="80px">
        <el-form-item label="文档内容">
          <el-input
            v-model="indexForm.content"
            type="textarea"
            :rows="6"
            placeholder="输入文档内容..."
          />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="indexForm.source" placeholder="例如：manual, upload" />
        </el-form-item>
        <el-form-item label="元数据">
          <el-input
            v-model="indexForm.metadataStr"
            type="textarea"
            :rows="3"
            placeholder='JSON 格式，例如：{"category": "guide"}'
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showIndexDialog = false">取消</el-button>
        <el-button type="primary" :loading="documentsStore.isIndexing" @click="handleIndex">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useDocumentsStore } from '@/stores/documents'

const documentsStore = useDocumentsStore()
const searchQuery = ref('')
const showIndexDialog = ref(false)
const indexForm = ref({
  content: '',
  source: '',
  metadataStr: '',
})

const handleSearch = async () => {
  if (!searchQuery.value.trim()) return
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
        source: indexForm.value.source || 'upload',
        metadata,
      },
    ])

    showIndexDialog.value = false
    indexForm.value = { content: '', source: '', metadataStr: '' }
  } catch (error: any) {
    console.error('索引失败:', error)
  }
}
</script>

<style scoped>
.documents-view {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.view-header h2 {
  margin: 0;
  color: #333;
}

.search-area {
  margin-bottom: 20px;
}

:deep(.el-table__empty-text) {
  color: #909399;
}
</style>
