import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface Skill {
  id: string
  name: string
  description: string
  type: 'mcp' | 'rag' | 'system'
  enabled: boolean
  parameters?: SkillParameter[]
}

export interface SkillParameter {
  name: string
  type: string
  description: string
  required: boolean
  default?: any
}

export const useSkillsStore = defineStore('skills', () => {
  const skills = ref<Skill[]>([])
  const isLoading = ref(false)

  // 获取技能列表
  async function fetchSkills() {
    isLoading.value = true
    try {
      // TODO: 实现获取技能列表的 API
      // const response = await apiClient.get('/skills')
      // skills.value = response.data

      // 模拟数据
      skills.value = [
        {
          id: '1',
          name: 'get_weather',
          description: '查询城市实时天气信息',
          type: 'mcp',
          enabled: true,
          parameters: [
            { name: 'city', type: 'string', description: '要查询天气的城市名称', required: true },
            { name: 'lang', type: 'string', description: '语言代码 (zh/en)', required: false, default: 'zh' },
          ],
        },
        {
          id: '2',
          name: 'web_search',
          description: '互联网搜索信息',
          type: 'mcp',
          enabled: true,
          parameters: [
            { name: 'query', type: 'string', description: '搜索关键词', required: true },
          ],
        },
        {
          id: '3',
          name: 'file_read',
          description: '读取文件系统文件',
          type: 'mcp',
          enabled: true,
          parameters: [
            { name: 'path', type: 'string', description: '文件路径', required: true },
          ],
        },
        {
          id: '4',
          name: 'database_query',
          description: '执行数据库查询',
          type: 'mcp',
          enabled: true,
          parameters: [
            { name: 'query', type: 'string', description: 'SQL 查询语句', required: true },
          ],
        },
        {
          id: '5',
          name: 'rag_search',
          description: '知识库检索',
          type: 'rag',
          enabled: true,
          parameters: [
            { name: 'query', type: 'string', description: '检索查询', required: true },
            { name: 'topK', type: 'number', description: '返回结果数量', required: false, default: 5 },
          ],
        },
      ]
    } finally {
      isLoading.value = false
    }
  }

  // 切换技能状态
  async function toggleSkill(id: string) {
    const skill = skills.value.find((s) => s.id === id)
    if (skill) {
      skill.enabled = !skill.enabled
      // TODO: 调用 API 更新状态
      // await apiClient.post(`/skills/${id}/toggle`)
    }
  }

  return {
    skills,
    isLoading,
    fetchSkills,
    toggleSkill,
  }
})
