<template>
  <div class="skills-view">
    <div class="view-header">
      <h2>技能管理</h2>
      <el-tag type="info">共 {{ skillsStore.skills.length }} 个技能</el-tag>
    </div>

    <el-loading v-if="skillsStore.isLoading" text="加载中..." />

    <el-table v-else :data="skillsStore.skills" stripe>
      <el-table-column prop="name" label="技能名称" width="200" />
      <el-table-column prop="description" label="描述" min-width="300" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.type === 'mcp' ? 'success' : 'primary'">
            {{ row.type.toUpperCase() }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="参数" width="150">
        <template #default="{ row }">
          <el-popover placement="top" trigger="click">
            <template #reference>
              <el-tag size="small" effect="plain">
                {{ row.parameters?.length || 0 }} 个参数
              </el-tag>
            </template>
            <div v-if="row.parameters?.length">
              <div v-for="param in row.parameters" :key="param.name" class="param-item">
                <strong>{{ param.name }}</strong> ({{ param.type }})
                <span v-if="param.required" class="required">必填</span>
                <div class="param-desc">{{ param.description }}</div>
              </div>
            </div>
            <div v-else>无参数</div>
          </el-popover>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch
            v-model="row.enabled"
            @change="skillsStore.toggleSkill(row.id)"
            active-color="#13ce66"
            inactive-color="#ff4949"
          />
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useSkillsStore } from '@/stores/skills'

const skillsStore = useSkillsStore()

onMounted(() => {
  skillsStore.fetchSkills()
})
</script>

<style scoped>
.skills-view {
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

.param-item {
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #eee;
}

.param-item:last-child {
  border-bottom: none;
}

.param-item strong {
  color: #409EFF;
}

.param-item .required {
  color: #f56c6c;
  font-size: 12px;
  margin-left: 8px;
}

.param-desc {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}
</style>
