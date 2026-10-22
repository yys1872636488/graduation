<template>
  <div class="page-container">
    <div class="page-header">
      <h2>用户管理</h2>
      <el-button type="primary" v-if="has('user:create')" @click="openDialog()">新增用户</el-button>
    </div>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item><el-input v-model="query.keyword" placeholder="搜索用户名或姓名" clearable @keyup.enter="fetchData" style="width:220px"/></el-form-item>
        <el-form-item><el-select v-model="query.status" clearable placeholder="状态筛选" style="width:120px"><el-option label="正常" :value="1"/><el-option label="禁用" :value="0"/></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="fetchData">查询</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
      </el-form>
    </div>

    <div class="table-card" style="margin-top:16px">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="username" label="用户名" width="120"/>
        <el-table-column prop="realName" label="姓名" width="120"/>
        <el-table-column label="角色" min-width="220">
          <template #default="{ row }">
            <el-tag v-for="r in row.roleNames" :key="r" size="small" style="margin-right:4px">{{ r }}</el-tag>
            <span v-if="!row.roleNames?.length" style="color:var(--color-text-secondary)">未分配</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140"/>
        <el-table-column prop="email" label="邮箱" width="180"/>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170"/>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)" v-if="has('user:edit')">编辑信息</el-button>
            <el-button type="primary" link @click="openRoleDialog(row)" v-if="has('user:edit')">修改角色</el-button>
            <el-button link :type="row.status===1?'warning':'success'" @click="toggleStatus(row)" v-if="has('user:disable')">{{ row.status===1?'禁用':'启用' }}</el-button>
            <el-button type="danger" link @click="handleResetPwd(row)" v-if="has('user:reset-password')">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="fetchData"/>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户信息' : '新增用户'" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="登录账号"/>
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="form.password" type="password" show-password placeholder="至少6位"/>
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" placeholder="真实姓名"/></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="手机号码"/></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" placeholder="电子邮箱"/></el-form-item>
        <el-form-item label="角色" v-if="!isEdit">
          <el-select v-model="form.roleIds" multiple placeholder="选择角色" style="width:100%">
            <el-option v-for="role in roleList" :key="role.id" :label="role.roleName" :value="role.id"/>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改角色弹窗 -->
    <el-dialog v-model="roleDialogVisible" :title="'修改角色 — ' + roleFormUser?.username" width="440px">
      <el-form label-width="80px">
        <el-form-item label="当前角色">
          <el-tag v-for="r in roleFormUser?.roleNames" :key="r" size="small" style="margin-right:4px">{{ r }}</el-tag>
          <span v-if="!roleFormUser?.roleNames?.length" style="color:var(--color-text-secondary)">未分配</span>
        </el-form-item>
        <el-form-item label="新角色">
          <el-select v-model="roleFormIds" multiple placeholder="选择角色" style="width:100%">
            <el-option v-for="role in roleList" :key="role.id" :label="role.roleName" :value="role.id"/>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSaving" @click="handleSaveRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { listUsers, createUser, updateUser, disableUser, enableUser, resetPassword, getUserRoleIds } from '@/api/user'
import { listRoles } from '@/api/role'

const userStore = useUserStore()
const has = p => userStore.hasPermission(p)
const loading = ref(false), saving = ref(false), dialogVisible = ref(false), isEdit = ref(false), editId = ref(null)
const roleDialogVisible = ref(false), roleSaving = ref(false), roleFormUser = ref(null), roleFormIds = ref([])
const tableData = ref([]), total = ref(0), roleList = ref([]), formRef = ref(null)
const query = reactive({ page: 1, size: 10, keyword: '', status: null })
const form = reactive({ username: '', password: '', realName: '', phone: '', email: '', roleIds: [] })
const rules = { username: [{ required: true, message: '请输入用户名' }], password: [{ required: true, min: 6, message: '密码至少6位' }] }

async function fetchData() { loading.value = true; try { const r = await listUsers(query); tableData.value = r.data.records; total.value = r.data.total } finally { loading.value = false } }
function resetQuery() { query.keyword = ''; query.status = null; query.page = 1; fetchData() }
async function fetchRoles() { const r = await listRoles(); roleList.value = r.data || [] }

// 编辑信息弹窗（编辑时不改角色）
async function openDialog(row) {
  if (row?.id) {
    isEdit.value = true; editId.value = row.id
    form.username = row.username
    form.realName = row.realName || ''
    form.phone = row.phone || ''
    form.email = row.email || ''
    form.password = ''
  } else {
    await fetchRoles()
    isEdit.value = false; editId.value = null; resetForm()
  }
  dialogVisible.value = true
}

// 修改角色弹窗
async function openRoleDialog(row) {
  await fetchRoles()
  try {
    const r = await getUserRoleIds(row.id)
    roleFormIds.value = r.data || []
  } catch { roleFormIds.value = [] }
  roleFormUser.value = row
  roleDialogVisible.value = true
}

function resetForm() { form.username = ''; form.password = ''; form.realName = ''; form.phone = ''; form.email = ''; form.roleIds = []; formRef.value?.resetFields() }

async function handleSave() {
  if (!await formRef.value.validate().catch(() => false)) return
  saving.value = true
  try {
    if (isEdit.value) { await updateUser(editId.value, { realName: form.realName, phone: form.phone, email: form.email }); ElMessage.success('用户信息已更新') }
    else { await createUser(form); ElMessage.success('用户已创建') }
    dialogVisible.value = false; fetchData()
  } finally { saving.value = false }
}

async function handleSaveRoles() {
  roleSaving.value = true
  try {
    await updateUser(roleFormUser.value.id, { realName: roleFormUser.value.realName, phone: roleFormUser.value.phone, email: roleFormUser.value.email, roleIds: roleFormIds.value })
    ElMessage.success('角色已更新')
    roleDialogVisible.value = false; fetchData()
  } finally { roleSaving.value = false }
}

async function toggleStatus(row) {
  const action = row.status === 1 ? '禁用' : '启用'
  await ElMessageBox.confirm(`确定${action}「${row.username}」？`, '操作确认', { type: 'warning' })
  row.status === 1 ? await disableUser(row.id) : await enableUser(row.id)
  ElMessage.success(`已${action}`); fetchData()
}
async function handleResetPwd(row) {
  const { value } = await ElMessageBox.prompt('请输入新密码（至少6位）', '重置密码', { confirmButtonText: '确定', inputType: 'password', inputValidator: v => v?.length >= 6 ? true : '密码至少6位' })
  await resetPassword(row.id, value); ElMessage.success('密码已重置')
}
onMounted(fetchData)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--spacing-lg) }
.page-header h2 { margin: 0 }
.table-footer { display: flex; justify-content: flex-end; margin-top: var(--spacing-md) }
</style>