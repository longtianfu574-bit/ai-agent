<template>
  <div class="skills-view">
    <!-- 页面头部 -->
    <header class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          <el-icon class="title-icon"><Tools /></el-icon>
          技能管理
        </h1>
        <p class="page-desc">管理和配置 MCP 服务提供的各种技能</p>
      </div>
      <div class="header-stats">
        <div class="stat-card">
          <div class="stat-value">{{ skillsStore.skills.length }}</div>
          <div class="stat-label">总技能数</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ enabledCount }}</div>
          <div class="stat-label">已启用</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ mcpCount }}</div>
          <div class="stat-label">MCP 技能</div>
        </div>
      </div>
    </header>

    <!-- 技能卡片列表 -->
    <div class="skills-grid">
      <div
        v-for="skill in skillsStore.skills"
        :key="skill.id"
        :class="['skill-card', { disabled: !skill.enabled }]"
      >
        <div class="skill-header">
          <div class="skill-icon">
            <el-icon><Tools /></el-icon>
          </div>
          <div class="skill-meta">
            <h3 class="skill-name">{{ skill.name }}</h3>
            <el-tag :type="skill.type === 'mcp' ? 'success' : 'primary'" size="small">
              {{ skill.type.toUpperCase() }}
            </el-tag>
          </div>
          <el-switch
            v-model="skill.enabled"
            @change="skillsStore.toggleSkill(skill.id)"
            class="skill-switch"
          />
        </div>

        <p class="skill-description">{{ skill.description }}</p>

        <div v-if="skill.parameters?.length" class="skill-params">
          <div class="params-label">
            <el-icon><Setting /></el-icon>
            参数配置 ({{ skill.parameters.length }})
          </div>
          <div class="params-list">
            <div
              v-for="param in skill.parameters"
              :key="param.name"
              class="param-item"
            >
              <div class="param-info">
                <span class="param-name">{{ param.name }}</span>
                <span class="param-type">{{ param.type }}</span>
              </div>
              <el-tag
                v-if="param.required"
                type="danger"
                size="small"
                effect="plain"
              >
                必填
              </el-tag>
            </div>
          </div>
        </div>

        <div v-else class="skill-no-params">
          <el-text type="info" size="small">
            <el-icon><InfoFilled /></el-icon>
            无需参数
          </el-text>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty
      v-if="skillsStore.skills.length === 0 && !skillsStore.isLoading"
      description="暂无技能配置"
    >
      <el-button type="primary" @click="skillsStore.fetchSkills">刷新</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useSkillsStore } from '@/stores/skills'
import { Tools, Setting, InfoFilled } from '@element-plus/icons-vue'

const skillsStore = useSkillsStore()

const enabledCount = computed(() =>
  skillsStore.skills.filter(s => s.enabled).length
)

const mcpCount = computed(() =>
  skillsStore.skills.filter(s => s.type === 'mcp').length
)

onMounted(() => {
  skillsStore.fetchSkills()
})
</script>

<style scoped>
.skills-view {
  height: 100%;
  overflow-y: auto;
  background: #f8fafc;
  padding: 0;
}

/* 页面头部 */
.page-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 32px 40px;
  color: #fff;
}

.header-content {
  margin-bottom: 24px;
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

.header-stats {
  display: flex;
  gap: 16px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  padding: 16px 24px;
  min-width: 120px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
}

.stat-label {
  font-size: 13px;
  opacity: 0.8;
  margin-top: 4px;
}

/* 技能网格 */
.skills-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 20px;
  padding: 24px 40px;
  max-width: 1600px;
}

.skill-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  border: 2px solid transparent;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.skill-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1);
  border-color: #667eea;
}

.skill-card.disabled {
  opacity: 0.6;
  background: #f1f5f9;
}

.skill-card.disabled:hover {
  transform: none;
  box-shadow: none;
  border-color: transparent;
}

.skill-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.skill-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
  flex-shrink: 0;
}

.skill-icon .el-icon {
  font-size: 26px;
  color: #fff;
}

.skill-card.disabled .skill-icon {
  background: #cbd5e1;
  box-shadow: none;
}

.skill-meta {
  flex: 1;
  min-width: 0;
}

.skill-name {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
}

.skill-description {
  margin: 0 0 20px;
  font-size: 14px;
  color: #64748b;
  line-height: 1.7;
}

.skill-switch {
  margin-left: auto;
}

/* 技能参数 */
.skill-params {
  border-top: 1px solid #eef2f6;
  padding-top: 16px;
}

.params-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 12px;
}

.params-label .el-icon {
  font-size: 16px;
  color: #667eea;
}

.params-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.param-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #f8fafc;
  border-radius: 8px;
  transition: background 0.2s;
}

.param-item:hover {
  background: #f1f5f9;
}

.param-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.param-name {
  font-weight: 600;
  color: #334155;
  font-size: 14px;
}

.param-type {
  font-size: 12px;
  color: #94a3b8;
  background: #e2e8f0;
  padding: 2px 8px;
  border-radius: 4px;
}

.skill-no-params {
  padding: 12px 0;
}

.skill-no-params .el-text {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .skills-grid {
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  }
}

@media (max-width: 768px) {
  .skills-grid {
    grid-template-columns: 1fr;
  }

  .page-header {
    padding: 24px 20px;
  }

  .header-stats {
    flex-wrap: wrap;
  }

  .skills-grid {
    padding: 16px;
  }
}
</style>
