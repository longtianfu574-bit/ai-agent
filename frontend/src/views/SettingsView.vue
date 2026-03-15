<template>
  <div class="settings-view">
    <!-- 页面头部 -->
    <header class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          <el-icon class="title-icon"><Setting /></el-icon>
          系统设置
        </h1>
        <p class="page-desc">配置 API 连接和系统参数</p>
      </div>
    </header>

    <!-- 主体内容 -->
    <main class="settings-content">
      <div class="settings-grid">
        <!-- API 配置卡片 -->
        <el-card class="settings-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon class="card-icon"><Link /></el-icon>
              <span>API 配置</span>
            </div>
          </template>

          <el-form :model="settings" label-position="top" size="large">
            <el-form-item label="后端服务地址">
              <el-input
                v-model="settings.backendUrl"
                placeholder="http://localhost:8080"
              >
                <template #append>
                  <el-button @click="testConnection" :loading="testing">
                    测试
                  </el-button>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="API Token">
              <el-input
                v-model="settings.apiToken"
                type="password"
                placeholder="请输入 API Token"
                show-password
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                @click="saveSettings"
                class="save-btn"
              >
                保存配置
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 系统信息卡片 -->
        <el-card class="settings-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon class="card-icon"><Monitor /></el-icon>
              <span>系统信息</span>
            </div>
          </template>

          <el-descriptions :column="1" border class="system-info">
            <el-descriptions-item label="前端版本">
              <el-tag type="primary" size="small">v1.0.0</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="Vue 版本">
              <el-tag type="success" size="small">{{ vueVersion }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="UI 组件">
              <el-tag type="warning" size="small">Element Plus 2.6</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态管理">
              <el-tag type="info" size="small">Pinia 2.2</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="构建工具">
              <el-tag type="danger" size="small">Vite 5.4</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 快捷操作卡片 -->
        <el-card class="settings-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon class="card-icon"><Operation /></el-icon>
              <span>快捷操作</span>
            </div>
          </template>

          <div class="quick-actions">
            <el-button class="action-btn" @click="clearAllData">
              清空本地数据
            </el-button>
            <el-button class="action-btn" @click="resetSettings">
              重置设置
            </el-button>
          </div>
        </el-card>

        <!-- 关于卡片 -->
        <el-card class="settings-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon class="card-icon"><InfoFilled /></el-icon>
              <span>关于</span>
            </div>
          </template>

          <div class="about-content">
            <div class="about-logo">
              <el-icon><Monitor /></el-icon>
            </div>
            <h3>AI Agent</h3>
            <p>智能对话助手系统</p>
            <div class="about-info">
              <p><strong>后端:</strong> Spring Boot 3.2 + Qwen-3.5</p>
              <p><strong>RAG:</strong> Qdrant 向量数据库</p>
              <p><strong>MCP:</strong> Model Context Protocol</p>
            </div>
          </div>
        </el-card>
      </div>
    </main>

    <!-- 连接成功提示 -->
    <el-dialog
      v-model="showSuccessDialog"
      title="连接测试"
      width="400px"
      :close-on-click-modal="false"
    >
      <div class="test-result">
        <el-icon class="success-icon"><CircleCheck /></el-icon>
        <h4>连接成功</h4>
        <p>后端服务响应正常</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="showSuccessDialog = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import * as vue from 'vue'
import { ElMessage } from 'element-plus'
import {
  Setting, Link, Monitor, Operation, InfoFilled, CircleCheck
} from '@element-plus/icons-vue'

const vueVersion = ref(vue.version)

const settings = reactive({
  backendUrl: localStorage.getItem('backend_url') || 'http://localhost:8080',
  apiToken: localStorage.getItem('api_token') || '',
})

const testing = ref(false)
const showSuccessDialog = ref(false)

const saveSettings = () => {
  localStorage.setItem('backend_url', settings.backendUrl)
  localStorage.setItem('api_token', settings.apiToken)
  ElMessage.success('配置已保存')
}

const testConnection = async () => {
  testing.value = true
  try {
    const response = await fetch(`${settings.backendUrl}/actuator/health`)
    if (response.ok) {
      showSuccessDialog.value = true
    } else {
      ElMessage.error('连接失败，请检查后端服务')
    }
  } catch {
    ElMessage.error('无法连接到后端服务')
  } finally {
    testing.value = false
  }
}

const clearAllData = () => {
  localStorage.clear()
  ElMessage.success('本地数据已清空')
}

const resetSettings = () => {
  settings.backendUrl = 'http://localhost:8080'
  settings.apiToken = ''
  saveSettings()
  ElMessage.success('设置已重置')
}
</script>

<style scoped>
.settings-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f0f4ff 0%, #e8ecff 100%);
}

/* 页面头部 */
.page-header {
  padding: 32px 40px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  color: #fff;
}

.header-content {
  max-width: 800px;
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

/* 主体内容 */
.settings-content {
  flex: 1;
  overflow-y: auto;
  padding: 32px 40px;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(450px, 1fr));
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.settings-card {
  border-radius: 16px;
  overflow: hidden;
  border: none;
  transition: all 0.3s;
}

.settings-card:hover {
  transform: translateY(-4px);
}

.settings-card :deep(.el-card__header) {
  padding: 20px 24px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 1px solid #e2e8f0;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
}

.card-icon {
  font-size: 24px;
  color: #4f46e5;
}

/* 表单样式 */
.settings-card :deep(.el-form-item) {
  margin-bottom: 20px;
}

.settings-card :deep(.el-form-item__label) {
  font-weight: 600;
  color: #475569;
  margin-bottom: 8px;
}

.settings-card :deep(.el-input__wrapper) {
  border-radius: 12px;
}

.save-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(79, 70, 229, 0.3);
  transition: all 0.3s;
}

.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 25px rgba(79, 70, 229, 0.4);
}

/* 系统信息 */
.system-info {
  margin-top: 8px;
}

.system-info :deep(.el-descriptions__label) {
  font-weight: 600;
  width: 120px;
}

/* 快捷操作 */
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-btn {
  height: 48px;
  border-radius: 12px;
  font-size: 15px;
  justify-content: flex-start;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  transition: all 0.3s;
}

.action-btn:hover {
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  color: #fff;
  border-color: transparent;
  transform: translateX(4px);
}

/* 关于内容 */
.about-content {
  text-align: center;
  padding: 20px 0;
}

.about-logo {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  border-radius: 20px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 25px rgba(79, 70, 229, 0.3);
}

.about-logo .el-icon {
  font-size: 40px;
  color: #fff;
}

.about-content h3 {
  font-size: 24px;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.about-content p {
  color: #64748b;
  margin: 0 0 24px;
}

.about-info {
  text-align: left;
  background: #f8fafc;
  padding: 20px;
  border-radius: 12px;
}

.about-info p {
  margin: 12px 0;
  font-size: 14px;
}

.about-info p:last-child {
  margin-bottom: 0;
}

/* 测试成功对话框 */
.test-result {
  text-align: center;
  padding: 20px;
}

.success-icon {
  font-size: 60px;
  color: #10b981;
  margin-bottom: 16px;
}

.test-result h4 {
  font-size: 20px;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.test-result p {
  color: #64748b;
  margin: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }

  .settings-content {
    padding: 20px 16px;
  }

  .page-header {
    padding: 24px 20px;
  }
}
</style>
