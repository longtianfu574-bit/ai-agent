<template>
  <div class="settings-view">
    <div class="view-header">
      <h2>系统设置</h2>
    </div>

    <el-card>
      <template #header>
        <span>API 配置</span>
      </template>
      <el-form label-width="120px">
        <el-form-item label="API Token">
          <el-input
            v-model="settings.apiToken"
            type="password"
            placeholder="输入 API Token"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveSettings">保存配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 20px">
      <template #header>
        <span>系统信息</span>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="前端版本">1.0.0</el-descriptions-item>
        <el-descriptions-item label="框架">Vue 3 + TypeScript</el-descriptions-item>
        <el-descriptions-item label="UI 组件">Element Plus</el-descriptions-item>
        <el-descriptions-item label="状态管理">Pinia</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const settings = ref({
  apiToken: '',
})

onMounted(() => {
  settings.value.apiToken = localStorage.getItem('api_token') || ''
})

const saveSettings = () => {
  if (settings.value.apiToken) {
    localStorage.setItem('api_token', settings.value.apiToken)
  }
}
</script>

<style scoped>
.settings-view {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}

.view-header {
  margin-bottom: 20px;
}

.view-header h2 {
  margin: 0;
  color: #333;
}
</style>
