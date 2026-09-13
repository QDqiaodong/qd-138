import { createRouter, createWebHistory } from 'vue-router'
import ThemeManagement from '@/views/ThemeManagement.vue'
import RoleManagement from '@/views/RoleManagement.vue'
import PropManagement from '@/views/PropManagement.vue'
import AssociationManagement from '@/views/AssociationManagement.vue'
import TraceQuery from '@/views/TraceQuery.vue'
import HistoryRecord from '@/views/HistoryRecord.vue'

const routes = [
  { path: '/', redirect: '/theme' },
  { path: '/theme', component: ThemeManagement },
  { path: '/role', component: RoleManagement },
  { path: '/prop', component: PropManagement },
  { path: '/association', component: AssociationManagement },
  { path: '/trace', component: TraceQuery },
  { path: '/history', component: HistoryRecord }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router