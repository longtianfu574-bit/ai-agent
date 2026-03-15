<template>
  <div class="memory-view">
    <div class="view-header">
      <h2>记忆管理</h2>
      <el-tag type="warning">功能开发中</el-tag>
    </div>

    <el-empty description="记忆管理功能正在开发中">
      <el-button type="primary">了解详情</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
// 记忆管理视图 - 待实现
</script>

<style scoped>
.memory-view {
  padding: 20px;
  height: 100%;
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
</style>
