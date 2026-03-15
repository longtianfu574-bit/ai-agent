import { createRouter, createWebHistory } from 'vue-router'
import ChatView from '../views/ChatView.vue'
import SkillsView from '../views/SkillsView.vue'
import DocumentsView from '../views/DocumentsView.vue'
import MemoryView from '../views/MemoryView.vue'
import SettingsView from '../views/SettingsView.vue'

const routes = [
  {
    path: '/',
    redirect: '/chat',
  },
  {
    path: '/chat',
    name: 'Chat',
    component: ChatView,
    meta: { title: '智能对话' },
  },
  {
    path: '/skills',
    name: 'Skills',
    component: SkillsView,
    meta: { title: '技能管理' },
  },
  {
    path: '/documents',
    name: 'Documents',
    component: DocumentsView,
    meta: { title: '文档管理' },
  },
  {
    path: '/memory',
    name: 'Memory',
    component: MemoryView,
    meta: { title: '记忆管理' },
  },
  {
    path: '/settings',
    name: 'Settings',
    component: SettingsView,
    meta: { title: '系统设置' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  if (to.meta.title) {
    document.title = `${to.meta.title} - AI Agent`
  }
  next()
})

export default router
