<template>
  <div class="memory-view">
    <!-- 页面头部 -->
    <header class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          <el-icon class="title-icon"><Collection /></el-icon>
          记忆管理
        </h1>
        <p class="page-desc">管理 AI 助手的长期记忆和上下文信息</p>
      </div>
    </header>

    <!-- 主体内容 -->
    <main class="memory-content">
      <!-- 功能开发中状态 -->
      <div class="coming-soon">
        <div class="coming-soon-icon">
          <el-icon><Collection /></el-icon>
        </div>
        <h2>功能开发中</h2>
        <p>记忆管理功能即将上线，敬请期待...</p>

        <div class="feature-preview">
          <h3>即将支持的功能</h3>
          <div class="feature-list">
            <div class="feature-item">
              <div class="feature-icon">
                <el-icon><User /></el-icon>
              </div>
              <div class="feature-info">
                <h4>用户偏好记忆</h4>
                <p>记住您的使用习惯和偏好设置</p>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon">
                <el-icon><Document /></el-icon>
              </div>
              <div class="feature-info">
                <h4>项目知识库</h4>
                <p>存储项目相关的知识和上下文</p>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon">
                <el-icon><ChatDotRound /></el-icon>
              </div>
              <div class="feature-info">
                <h4>对话摘要</h4>
                <p>自动总结重要对话内容</p>
              </div>
            </div>
          </div>
        </div>

        <el-button type="primary" @click="$router.push('/chat')">
          <el-icon><ChatDotRound /></el-icon>
          返回对话
        </el-button>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { User, Document, ChatDotRound, Collection } from '@element-plus/icons-vue'
</script>

<style scoped>
.memory-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
}

/* 页面头部 */
.page-header {
  padding: 32px 40px;
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
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
.memory-content {
  flex: 1;
  overflow-y: auto;
  padding: 40px 20px;
}

/* Coming Soon 状态 */
.coming-soon {
  max-width: 800px;
  margin: 0 auto;
  text-align: center;
  background: #fff;
  border-radius: 24px;
  padding: 60px 40px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

.coming-soon-icon {
  width: 120px;
  height: 120px;
  margin: 0 auto 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 40px rgba(139, 92, 246, 0.3);
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.coming-soon-icon .el-icon {
  font-size: 60px;
  color: #fff;
}

.coming-soon h2 {
  font-size: 32px;
  color: #1a1a2e;
  margin: 0 0 12px;
}

.coming-soon > p {
  font-size: 16px;
  color: #64748b;
  margin: 0 0 48px;
}

/* 功能预览 */
.feature-preview {
  margin-bottom: 40px;
  text-align: left;
}

.feature-preview h3 {
  font-size: 18px;
  color: #475569;
  margin: 0 0 24px;
  text-align: center;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 16px;
  border: 2px solid #e2e8f0;
  transition: all 0.3s;
}

.feature-item:hover {
  border-color: #8b5cf6;
  transform: translateX(8px);
  box-shadow: 0 4px 20px rgba(139, 92, 246, 0.15);
}

.feature-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 15px rgba(139, 92, 246, 0.3);
}

.feature-icon .el-icon {
  font-size: 26px;
  color: #fff;
}

.feature-info {
  flex: 1;
  text-align: left;
}

.feature-info h4 {
  font-size: 16px;
  color: #1a1a2e;
  margin: 0 0 6px;
  font-weight: 600;
}

.feature-info p {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.coming-soon .el-button {
  height: 48px;
  padding: 0 32px;
  border-radius: 14px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(139, 92, 246, 0.3);
  transition: all 0.3s;
}

.coming-soon .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 25px rgba(139, 92, 246, 0.4);
}
</style>
