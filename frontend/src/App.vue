<template>
  <div class="app-container">
    <el-container>
      <!-- 侧边栏导航 -->
      <el-aside width="220px">
        <div class="logo">
          <el-icon><Monitor /></el-icon>
          <span>AI Agent</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
        >
          <el-menu-item index="/chat">
            <el-icon><ChatDotRound /></el-icon>
            <span>智能对话</span>
          </el-menu-item>
          <el-menu-item index="/skills">
            <el-icon><Tools /></el-icon>
            <span>技能管理</span>
          </el-menu-item>
          <el-menu-item index="/documents">
            <el-icon><Document /></el-icon>
            <span>文档管理</span>
          </el-menu-item>
          <el-menu-item index="/memory">
            <el-icon><Memory /></el-icon>
            <span>记忆管理</span>
          </el-menu-item>
          <el-menu-item index="/settings">
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </el-menu-item>
        </el-menu>
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

const route = useRoute()
const activeMenu = computed(() => route.path as string)
</script>

<style scoped>
.app-container {
  height: 100vh;
  width: 100vw;
}

.el-container {
  height: 100%;
}

.el-aside {
  background-color: #304156;
  color: #fff;
  overflow-x: hidden;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 24px 0;
  font-size: 20px;
  font-weight: bold;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo .el-icon {
  font-size: 28px;
  color: #409EFF;
}

.el-menu {
  border-right: none;
}

.el-menu-item {
  margin: 4px 8px;
  border-radius: 4px;
}

.el-main {
  background-color: #f5f7fa;
  padding: 0;
  overflow: hidden;
}
</style>
