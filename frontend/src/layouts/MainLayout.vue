<template>
  <div class="app-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: isCollapse }">
      <div class="sidebar-brand">
        <div class="brand-icon">
          <svg width="28" height="28" viewBox="0 0 48 48" fill="none">
            <rect x="4" y="4" width="40" height="40" rx="4" stroke="rgba(255,255,255,0.6)" stroke-width="1.5"/>
            <path d="M16 18h4v12h-4zM22 14h4v16h-4zM28 18h4v12h-4z" fill="rgba(255,255,255,0.7)" opacity="0.6"/>
          </svg>
        </div>
        <transition name="fade">
          <span v-if="!isCollapse" class="brand-text">服装追溯系统</span>
        </transition>
      </div>

      <nav class="sidebar-nav">
        <template v-for="menu in userStore.menus" :key="menu.id">
          <!-- 有子菜单 -->
          <div v-if="menu.children && menu.children.length" class="nav-group">
            <div class="nav-group-title" v-if="!isCollapse">{{ menu.name }}</div>
            <template v-for="child in menu.children" :key="child.id">
              <router-link
                :to="child.path"
                class="nav-item"
                :class="{ active: isActive(child.path) }"
              >
                <span class="nav-icon" v-if="child.icon">
                  <el-icon :size="18"><component :is="child.icon" /></el-icon>
                </span>
                <span class="nav-label">{{ child.name }}</span>
              </router-link>
            </template>
          </div>
          <!-- 无子菜单的顶级菜单 -->
          <router-link
            v-else
            :to="menu.path"
            class="nav-item top-level"
            :class="{ active: isActive(menu.path) }"
          >
            <span class="nav-icon" v-if="menu.icon">
              <el-icon :size="18"><component :is="menu.icon" /></el-icon>
            </span>
            <span class="nav-label">{{ menu.name }}</span>
          </router-link>
        </template>
      </nav>

      <div class="sidebar-toggle" @click="isCollapse = !isCollapse">
        <el-icon :size="16"><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
      </div>
    </aside>

    <!-- 主区域 -->
    <div class="main-area">
      <!-- 顶部 -->
      <header class="topbar">
        <div class="topbar-left">
          <span class="page-title">{{ route.meta.title || '' }}</span>
        </div>
        <div class="topbar-right">
          <span class="user-name">{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</span>
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-avatar">
              {{ (userStore.userInfo?.realName || userStore.userInfo?.username || '?').charAt(0) }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区 -->
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)

function isActive(path) {
  return route.path === path || route.path.startsWith(path + '/')
}

function handleCommand(cmd) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确定退出当前账号？', '退出确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(() => {
      userStore.logout()
      router.push('/login')
    })
  }
}
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* === 侧边栏 === */
.sidebar {
  width: 220px;
  min-width: 220px;
  background: var(--color-sidebar);
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease, min-width 0.2s ease;
  position: relative;
  z-index: 10;
}
.sidebar.collapsed {
  width: 64px;
  min-width: 64px;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.brand-icon {
  flex-shrink: 0;
}
.brand-text {
  font-size: 15px;
  font-weight: 600;
  color: rgba(255,255,255,0.9);
  white-space: nowrap;
  overflow: hidden;
}

/* 导航 */
.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}
.sidebar-nav::-webkit-scrollbar { width: 0; }

.nav-group {
  margin-bottom: 4px;
}
.nav-group-title {
  padding: 12px 16px 4px;
  font-size: 11px;
  color: rgba(255,255,255,0.3);
  letter-spacing: 0.06em;
  white-space: nowrap;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  margin: 1px 8px;
  border-radius: var(--radius-md);
  color: rgba(255,255,255,0.55);
  text-decoration: none;
  font-size: 13px;
  transition: all 0.15s ease;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
}
.nav-item:hover {
  color: rgba(255,255,255,0.85);
  background: var(--color-sidebar-hover);
}
.nav-item.active {
  color: #fff;
  background: var(--color-primary);
}
.nav-item.top-level {
  margin-top: 2px;
}

.nav-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  width: 18px;
  justify-content: center;
}
.nav-label {
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar.collapsed .nav-group-title,
.sidebar.collapsed .nav-label,
.sidebar.collapsed .brand-text { display: none; }
.sidebar.collapsed .nav-item {
  justify-content: center;
  padding: 12px;
  margin: 2px 12px;
}

/* 折叠按钮 */
.sidebar-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 14px;
  color: rgba(255,255,255,0.3);
  cursor: pointer;
  border-top: 1px solid rgba(255,255,255,0.06);
  transition: color 0.15s;
}
.sidebar-toggle:hover { color: rgba(255,255,255,0.6); }

/* === 主区域 === */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

/* 顶部栏 */
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 var(--spacing-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.page-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s;
}
.user-avatar:hover { opacity: 0.85; }

/* 内容区 */
.content {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-lg);
  background: var(--color-bg);
}
</style>