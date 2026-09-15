<template>
  <div class="app-container">
    <el-container>
      <el-aside width="200px" class="aside">
        <div class="logo">剧本杀道具溯源系统</div>
        <el-menu :default-active="activeMenu" class="menu">
          <el-menu-item
            v-for="item in menuItems"
            :key="item.path"
            :index="item.path"
            @click="navigateTo(item.path)"
          >
            <a class="menu-link" :href="item.path" @click.prevent="navigateTo(item.path)">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </a>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header class="header">
          <span class="header-title">{{ pageTitle }}</span>
        </el-header>
        <el-main class="main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed, markRaw } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Menu, User, Box, Link, Search, Clock, View } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const menuItems = [
  { path: '/theme', title: '剧本主题管理', icon: markRaw(Menu) },
  { path: '/role', title: '人物角色管理', icon: markRaw(User) },
  { path: '/prop', title: '道具档案管理', icon: markRaw(Box) },
  { path: '/association', title: '角色道具关联', icon: markRaw(Link) },
  { path: '/trace', title: '双向溯源查询', icon: markRaw(Search) },
  { path: '/history', title: '变更记录', icon: markRaw(Clock) },
  { path: '/review', title: '人物回看', icon: markRaw(View) }
]

const titles = Object.fromEntries(menuItems.map(item => [item.path, item.title]))
const activeMenu = computed(() => route.path)
const pageTitle = computed(() => titles[route.path] || '剧本杀道具溯源系统')

const navigateTo = (path: string) => {
  if (route.path !== path) {
    router.push(path)
  }
}
</script>

<style scoped>
.app-container {
  height: 100vh;
  display: flex;
}

.aside {
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
}

.logo {
  padding: 20px;
  font-size: 18px;
  font-weight: bold;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.menu {
  border-right: none;
}

.menu-link {
  color: inherit;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 100%;
  text-decoration: none;
  width: 100%;
}

.header {
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
}

.header-title {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.main {
  background: #f5f5f5;
  padding: 20px;
}
</style>
