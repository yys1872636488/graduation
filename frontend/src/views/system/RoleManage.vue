<template>
  <div class="page-container">
    <h2>角色管理</h2>

    <div class="role-grid">
      <div v-for="role in roles" :key="role.id" class="role-card">
        <div class="role-card-header">
          <div class="role-avatar">{{ role.roleName.charAt(0) }}</div>
          <div class="role-info">
            <div class="role-name">{{ role.roleName }}</div>
            <div class="role-code">{{ role.roleCode }}</div>
          </div>
          <el-button type="primary" plain size="small" @click="openPermDialog(role)" v-if="has('role:edit')">
            配置权限
          </el-button>
        </div>
        <div class="role-desc">{{ role.description }}</div>
        <div class="role-stats">
          <div class="stat">
            <span class="stat-num">{{ getPermCount(role.id) }}</span>
            <span class="stat-label">已授权限</span>
          </div>
          <div class="stat">
            <span class="stat-num">{{ totalPerms - 1 }}</span>
            <span class="stat-label">可配权限</span>
          </div>
        </div>
        <div class="role-progress">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: getPermPercent(role.id) + '%' }"></div>
          </div>
          <span class="progress-text">{{ getPermPercent(role.id) }}%</span>
        </div>
      </div>
    </div>

    <el-drawer
      v-model="drawerVisible"
      :title="'权限配置 — ' + currentRole?.roleName"
      size="720px"
      direction="rtl"
      @open="loadPermData"
    >
      <template #header>
        <div class="drawer-header">
          <span class="drawer-title">权限配置 — {{ currentRole?.roleName }}</span>
          <div class="drawer-actions">
            <el-button size="small" @click="selectAll">全选所有</el-button>
            <el-button size="small" @click="clearAll">清空所有</el-button>
          </div>
        </div>
      </template>

      <div v-loading="permLoading" class="perm-container">
        <div v-for="group in permGroups" :key="group.id" class="perm-module">
          <div class="module-header" @click="group.collapsed = !group.collapsed">
            <div class="module-info">
              <el-icon :size="18"><component :is="group.icon || 'FolderOpened'" /></el-icon>
              <span class="module-name">{{ group.name }}</span>
              <el-tag size="small" type="info">{{ group.selectedCount }} / {{ group.totalCount }}</el-tag>
            </div>
            <div class="module-actions" @click.stop>
              <el-button link size="small" @click="selectModule(group)">全选</el-button>
              <el-button link size="small" @click="clearModule(group)">清空</el-button>
              <el-icon :size="16" class="collapse-icon" :class="{ rotated: !group.collapsed }"><ArrowDown /></el-icon>
            </div>
          </div>

          <el-collapse-transition>
            <div v-show="!group.collapsed" class="module-body">
              <div v-for="sub in group.subGroups" :key="sub.id" class="perm-subgroup">
                <div class="subgroup-name">{{ sub.name }}</div>
                <div class="perm-grid">
                  <el-checkbox
                    v-for="perm in sub.items" :key="perm.id"
                    v-model="perm.checked"
                    :label="perm.permName"
                    size="small"
                  />
                </div>
              </div>
              <div v-if="group.buttons.length" class="perm-subgroup">
                <div class="subgroup-name">操作权限</div>
                <div class="perm-grid">
                  <el-checkbox
                    v-for="perm in group.buttons" :key="perm.id"
                    v-model="perm.checked"
                    :label="perm.permName"
                    size="small"
                  />
                </div>
              </div>
            </div>
          </el-collapse-transition>
        </div>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <span class="footer-summary">已选择 {{ selectedTotal }} / {{ totalPermsForRole }} 项权限</span>
          <div>
            <el-button @click="drawerVisible = false">取消</el-button>
            <el-button type="primary" :loading="saving" @click="savePerms">保存配置</el-button>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { listRoles, listPermissions, getRolePermissions, assignPermissions } from '@/api/role'

const userStore = useUserStore()
const has = p => userStore.hasPermission(p)

const roles = ref([])
const saving = ref(false), permLoading = ref(false), drawerVisible = ref(false)
const currentRole = ref(null)
const allPerms = ref([]), rolePermIds = ref([]), permGroups = ref([])
const totalPerms = ref(0), rolePermCounts = ref({})

const MODULE_ORDER = ['dashboard', 'system', 'basic', 'procurement', 'inspection', 'production', 'warehouse', 'sales', 'traceability']

const selectedTotal = computed(() => permGroups.value.reduce((s, g) => s + g.selectedCount, 0))
const totalPermsForRole = computed(() => permGroups.value.reduce((s, g) => s + g.totalCount, 0))

function getPermCount(roleId) { return rolePermCounts.value[roleId] || 0 }
function getPermPercent(roleId) { return totalPerms.value > 1 ? Math.round(getPermCount(roleId) / (totalPerms.value - 1) * 100) : 0 }

async function fetchRoles() {
  const r = await listRoles()
  roles.value = r.data || []
  try { const p = await listPermissions(); totalPerms.value = (p.data || []).length } catch {}
  for (const role of roles.value) {
    try { const pr = await getRolePermissions(role.id); rolePermCounts.value[role.id] = (pr.data || []).length } catch {}
  }
}

async function loadPermData() {
  permLoading.value = true
  try {
    const [allRes, roleRes] = await Promise.all([listPermissions(), getRolePermissions(currentRole.value.id)])
    allPerms.value = allRes.data || []
    rolePermIds.value = roleRes.data || []
    buildPermGroups()
  } finally { permLoading.value = false }
}

function buildPermGroups() {
  const perms = allPerms.value
  const checked = new Set(rolePermIds.value)

  const topLevels = perms
    .filter(p => p.parentId === 0)
    .filter(mod => mod.permCode !== 'dashboard')
    .sort((a, b) => MODULE_ORDER.indexOf(a.permCode) - MODULE_ORDER.indexOf(b.permCode))

  permGroups.value = topLevels.map(mod => {
    const children = perms.filter(p => p.parentId === mod.id).sort((a, b) => a.sortOrder - b.sortOrder)
    const menus = children.filter(c => c.type === 'menu')
    const buttons = children.filter(c => c.type === 'button')

    const subGroups = menus.map(menu => {
      const subItems = perms.filter(p => p.parentId === menu.id)
      const allItems = [menu, ...subItems].map(p => ({ ...p, checked: checked.has(p.id) }))
      return { id: menu.id, name: menu.permName, items: allItems }
    })

    const buttonItems = buttons.map(p => ({ ...p, checked: checked.has(p.id) }))

    return reactive({
      id: mod.id, name: mod.permName, code: mod.permCode, icon: mod.icon, collapsed: false,
      subGroups, buttons: buttonItems,
      get selectedCount() {
        return this.subGroups.reduce((s, g) => s + g.items.filter(i => i.checked).length, 0)
             + this.buttons.filter(b => b.checked).length
      },
      get totalCount() {
        return this.subGroups.reduce((s, g) => s + g.items.length, 0)
             + this.buttons.length
      },
    })
  })
}

function openPermDialog(role) { currentRole.value = role; drawerVisible.value = true }

function selectAll() {
  permGroups.value.forEach(g => {
    g.subGroups.forEach(sg => sg.items.forEach(i => i.checked = true))
    g.buttons.forEach(b => b.checked = true)
  })
}
function clearAll() {
  permGroups.value.forEach(g => {
    g.subGroups.forEach(sg => sg.items.forEach(i => i.checked = false))
    g.buttons.forEach(b => b.checked = false)
  })
}
function selectModule(g) {
  g.subGroups.forEach(sg => sg.items.forEach(i => i.checked = true))
  g.buttons.forEach(b => b.checked = true)
}
function clearModule(g) {
  g.subGroups.forEach(sg => sg.items.forEach(i => i.checked = false))
  g.buttons.forEach(b => b.checked = false)
}

async function savePerms() {
  saving.value = true
  try {
    const ids = []
    permGroups.value.forEach(g => {
      g.subGroups.forEach(sg => sg.items.forEach(i => { if (i.checked) ids.push(i.id) }))
      g.buttons.forEach(b => { if (b.checked) ids.push(b.id) })
    })
    await assignPermissions(currentRole.value.id, ids)
    ElMessage.success('权限配置已保存，用户重新登录后生效')
    drawerVisible.value = false
    // 刷新权限数
    rolePermCounts.value[currentRole.value.id] = ids.length
  } finally { saving.value = false }
}

onMounted(fetchRoles)
</script>

<style scoped>
.role-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: var(--spacing-md) }
.role-card { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: var(--spacing-lg) }
.role-card-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px }
.role-avatar { width: 40px; height: 40px; border-radius: var(--radius-md); background: var(--color-primary-light); color: var(--color-primary); display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: 600; flex-shrink: 0 }
.role-info { flex: 1; min-width: 0 }
.role-name { font-size: var(--font-size-base); font-weight: 600; color: var(--color-text) }
.role-code { font-size: var(--font-size-xs); color: var(--color-text-secondary); margin-top: 2px }
.role-desc { font-size: var(--font-size-sm); color: var(--color-text-secondary); line-height: 1.5; margin-bottom: 16px; min-height: 36px }
.role-stats { display: flex; gap: 24px; margin-bottom: 12px }
.stat { display: flex; align-items: baseline; gap: 6px }
.stat-num { font-size: var(--font-size-xl); font-weight: 600; color: var(--color-text) }
.stat-label { font-size: var(--font-size-xs); color: var(--color-text-secondary) }
.role-progress { display: flex; align-items: center; gap: 10px }
.progress-bar { flex: 1; height: 4px; background: var(--color-bg); border-radius: 2px; overflow: hidden }
.progress-fill { height: 100%; background: var(--color-primary); border-radius: 2px; transition: width 0.3s }
.progress-text { font-size: var(--font-size-xs); color: var(--color-text-secondary); width: 36px; text-align: right }
.drawer-header { display: flex; align-items: center; justify-content: space-between; width: 100% }
.drawer-title { font-size: var(--font-size-lg); font-weight: 600 }
.drawer-actions { display: flex; gap: 8px }
.perm-container { display: flex; flex-direction: column; gap: 12px }
.perm-module { border: 1px solid var(--color-border); border-radius: var(--radius-md); overflow: hidden }
.module-header { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; background: #f8f9fb; cursor: pointer; user-select: none }
.module-info { display: flex; align-items: center; gap: 8px }
.module-name { font-size: var(--font-size-base); font-weight: 600; color: var(--color-text) }
.module-actions { display: flex; align-items: center; gap: 4px }
.collapse-icon { transition: transform 0.2s; margin-left: 4px }
.collapse-icon.rotated { transform: rotate(180deg) }
.module-body { padding: 12px 16px 16px; border-top: 1px solid var(--color-border) }
.perm-subgroup { margin-bottom: 12px }
.perm-subgroup:last-child { margin-bottom: 0 }
.subgroup-name { font-size: var(--font-size-xs); color: var(--color-text-secondary); font-weight: 500; margin-bottom: 8px }
.perm-grid { display: flex; flex-wrap: wrap; gap: 4px 16px }
.drawer-footer { display: flex; align-items: center; justify-content: space-between; width: 100% }
.footer-summary { font-size: var(--font-size-sm); color: var(--color-text-secondary) }
</style>