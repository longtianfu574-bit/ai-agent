<template>
  <div class="app-container">
    <el-container>
      <!-- 侧边栏导航 -->
      <el-aside width="240px">
        <div class="sidebar-header">
          <div class="logo">
            <div class="logo-icon">
              <el-icon><Monitor /></el-icon>
            </div>
            <span class="logo-text">AI Agent</span>
          </div>
        </div>

        <nav class="sidebar-nav">
          <router-link
            v-for="item in menuItems"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            active-class="active"
          >
            <el-icon class="nav-icon"><component :is="item.iconComponent" /></el-icon>
            <span class="nav-text">{{ item.name }}</span>
          </router-link>
        </nav>

        <div class="sidebar-footer">
          <div class="status-indicator">
            <span class="status-dot online"></span>
            <span class="status-text">系统运行中</span>
          </div>
        </div>
      </el-aside>

      <!-- 主内容区 -->
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  Monitor,
  ChatDotRound,
  Tools,
  Document,
  Collection,
  Setting,
} from '@element-plus/icons-vue'

const route = useRoute()

const menuItems = [
  { path: '/chat', name: '智能对话', iconComponent: ChatDotRound },
  { path: '/skills', name: '技能管理', iconComponent: Tools },
  { path: '/documents', name: '文档管理', iconComponent: Document },
  { path: '/memory', name: '记忆管理', iconComponent: Collection },
  { path: '/settings', name: '系统设置', iconComponent: Setting },
]
</script>

<style scoped>
.app-container {
  height: 100vh;
  width: 100vw;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.el-container {
  height: 100%;
}

.el-aside {
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.15);
  z-index: 10;
}

.sidebar-header {
  padding: 28px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo {
  display: flex;
  align-items: center;
  gap: 14px;
}

.logo-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.logo-icon .el-icon {
  font-size: 26px;
  color: #fff;
}

.logo-text {
  font-size: 22px;
  font-weight: 700;
  background: linear-gradient(135deg, #fff 0%, #e0e7ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 0.5px;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  color: #a0aec0;
  text-decoration: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.nav-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.15) 0%, transparent 100%);
  opacity: 0;
  transition: opacity 0.3s;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #fff;
}

.nav-item:hover::before {
  opacity: 1;
}

.nav-item.active {
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.25) 0%, rgba(118, 75, 162, 0.1) 100%);
  color: #fff;
}

.nav-item.active .nav-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 2px 10px rgba(102, 126, 234, 0.4);
}

.nav-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);
  font-size: 20px;
  transition: all 0.3s;
  flex-shrink: 0;
}

.nav-text {
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 0.3px;
}

.sidebar-footer {
  padding: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 10px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
  box-shadow: 0 0 10px rgba(245, 108, 108, 0.5);
}

.status-dot.online {
  background: #10b981;
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.5);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.status-text {
  font-size: 13px;
  color: #a0aec0;
  font-weight: 500;
}

.el-main {
  padding: 0;
  overflow: hidden;
  position: relative;
}
</style>
