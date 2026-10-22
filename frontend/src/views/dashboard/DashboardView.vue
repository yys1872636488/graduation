<template>
  <div class="dashboard">
    <div class="welcome">
      <h2>工作台</h2>
      <p>{{ greeting }}</p>
    </div>

    <div class="stat-grid">
      <div class="stat-item">
        <span class="stat-value">—</span>
        <span class="stat-label">采购订单</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">—</span>
        <span class="stat-label">待检批次</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">—</span>
        <span class="stat-label">生产中工单</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">—</span>
        <span class="stat-label">成品库存</span>
      </div>
    </div>

    <div class="quick-section">
      <h3>快捷入口</h3>
      <div class="quick-grid">
        <div class="quick-card" v-for="item in quickLinks" :key="item.path" @click="$router.push(item.path)">
          <el-icon :size="20"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </div>

    <div class="info-section">
      <div class="info-card">
        <h4>系统说明</h4>
        <p>本系统用于服装产品从原材料采购到销售售后的全生命周期追溯管理。各模块功能可通过左侧菜单进入，系统正在持续完善中。</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const greeting = computed(() => {
  const hour = new Date().getHours()
  const name = userStore.userInfo?.realName || userStore.userInfo?.username || ''
  const time = hour < 12 ? '上午好' : hour < 18 ? '下午好' : '晚上好'
  const roles = userStore.roles.join(' / ')
  return `${time}，${name}。当前角色：${roles}。`
})

const quickLinks = [
  { path: '/basic/suppliers', label: '供应商管理', icon: 'OfficeBuilding' },
  { path: '/basic/materials', label: '原材料管理', icon: 'Goods' },
  { path: '/basic/products', label: '产品管理', icon: 'ShoppingBag' },
  { path: '/procurement/orders', label: '采购订单', icon: 'Document' },
  { path: '/inspection/detect', label: '布料检测', icon: 'Picture' },
  { path: '/warehouse/inventory', label: '库存查询', icon: 'Box' },
]
</script>

<style scoped>
.dashboard {
  max-width: 1000px;
}

/* 欢迎区 */
.welcome {
  margin-bottom: var(--spacing-lg);
}
.welcome h2 {
  margin: 0 0 8px;
  font-size: var(--font-size-2xl);
  font-weight: 600;
}
.welcome p {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* 统计网格 */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1px;
  background: var(--color-border);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: var(--spacing-lg);
}

.stat-item {
  background: var(--color-surface);
  padding: 28px 24px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--color-text);
  letter-spacing: -0.02em;
}

.stat-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  letter-spacing: 0.02em;
}

/* 快捷入口 */
.quick-section {
  margin-bottom: var(--spacing-lg);
}
.quick-section h3 {
  font-size: var(--font-size-base);
  font-weight: 600;
  margin: 0 0 12px;
  color: var(--color-text);
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 1px;
  background: var(--color-border);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.quick-card {
  background: var(--color-surface);
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: background 0.15s;
  color: var(--color-text-secondary);
}
.quick-card:hover {
  background: var(--color-primary-light);
  color: var(--color-primary);
}
.quick-card span {
  font-size: var(--font-size-xs);
}

/* 信息区 */
.info-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
}
.info-card h4 {
  margin: 0 0 8px;
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-text);
}
.info-card p {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: 1.6;
}
</style>