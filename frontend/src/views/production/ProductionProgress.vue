<template>
  <div class="page-container">
    <h2>生产进度</h2>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item><el-input v-model="query.keyword" placeholder="工单号" clearable style="width:200px" @keyup.enter="search"/></el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
      </el-form>
    </div>

    <div class="table-card" style="margin-top:16px">
      <el-table :data="data" v-loading="loading" stripe>
        <el-table-column prop="workOrderNo" label="工单号" width="180"/>
        <el-table-column prop="productName" label="产品" min-width="140" show-overflow-tooltip/>
        <el-table-column prop="skuCode" label="SKU" width="150"/>
        <el-table-column prop="plannedQuantity" label="计划数量" width="100" align="right"/>
        <el-table-column prop="completedQuantity" label="已完成" width="100" align="right"/>
        <el-table-column label="当前工序" width="100">
          <template #default="{row}">{{ processLabel(row.currentProcess) }}</template>
        </el-table-column>
        <el-table-column label="完成率" width="160">
          <template #default="{row}">
            <el-progress :percentage="percent(row)" :stroke-width="8"/>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="statusMeta(row.status).type" size="small">{{ statusMeta(row.status).label }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{row}">
            <el-button link type="primary" v-if="has('production:order:report')" @click="openForm(row)">登记进度</el-button>
            <el-button link type="primary" @click="openHistory(row)">进度记录</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
                       :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="fetch"/>
      </div>
    </div>

    <el-dialog v-model="formVisible" title="登记生产进度" width="520px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="工单">{{ currentOrder?.workOrderNo }}</el-form-item>
        <el-form-item label="进度日期" prop="progressDate">
          <el-date-picker v-model="form.progressDate" type="date" value-format="YYYY-MM-DD" style="width:180px"/>
        </el-form-item>
        <el-form-item label="当前工序" prop="processName">
          <el-select v-model="form.processName" placeholder="请选择当前工序" style="width:180px">
            <el-option v-for="item in PRODUCTION_PROCESSES" :key="item.value"
                       :label="item.label" :value="item.value"/>
          </el-select>
        </el-form-item>
        <el-form-item label="累计完成" prop="completedQuantity">
          <el-input-number v-model="form.completedQuantity" :min="0" :max="Number(currentOrder?.plannedQuantity || 0)"
                           :precision="0" style="width:180px"/>
          <span class="muted"> / {{ currentOrder?.plannedQuantity }}</span>
        </el-form-item>
        <el-form-item label="进度百分比">
          <el-input-number v-model="form.progressPercent" :min="0" :max="100" :precision="2" style="width:180px"/>
          <span class="muted">留空自动计算</span>
        </el-form-item>
        <el-form-item label="登记人"><el-input v-model="form.recorder" style="width:180px"/></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <p class="muted">最后一批报工时请选择实际最后一道工序（通常是包装），报工完成后工单会自动转入待质检。</p>
      <template #footer><el-button @click="formVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>

    <el-drawer v-model="historyVisible" title="生产进度记录" size="560px">
      <el-table :data="history" size="small" border>
        <el-table-column prop="progressDate" label="日期" width="110"/>
        <el-table-column label="当前工序" min-width="120">
          <template #default="{row}">{{ processLabel(row.processName) }}</template>
        </el-table-column>
        <el-table-column prop="completedQuantity" label="累计完成" width="100"/>
        <el-table-column prop="progressPercent" label="进度%" width="90"/>
        <el-table-column prop="recorder" label="登记人" width="100"/>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip/>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { WORK_ORDER_STATUS, PRODUCTION_PROCESSES } from '@/utils/constants'
import { listWorkOrders, getWorkOrder, addProductionProgress } from '@/api/production'

const route = useRoute()
const userStore = useUserStore()
const has = permission => userStore.hasPermission(permission)

const loading = ref(false), saving = ref(false)
const data = ref([]), total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })
const formVisible = ref(false), formRef = ref(null), currentOrder = ref(null)
const historyVisible = ref(false), history = ref([])
const form = reactive({ progressDate: '', processName: '', completedQuantity: 0, progressPercent: null, recorder: '', remark: '' })
const rules = {
  progressDate: [{ required: true, message: '请选择进度日期', trigger: 'change' }],
  processName: [{ required: true, message: '请选择当前工序', trigger: 'change' }],
  completedQuantity: [{ required: true, message: '请输入累计完成数量', trigger: 'blur' }],
}

function processLabel(value) {
  return PRODUCTION_PROCESSES.find(item => item.value === value)?.label || value || '—'
}

function statusMeta(status) { return WORK_ORDER_STATUS[status] || { label: status || '未知', type: 'info' } }
function percent(row) {
  const planned = Number(row.plannedQuantity || 0)
  const completed = Number(row.completedQuantity || 0)
  if (!planned) return 0
  return Math.min(100, Math.round(completed / planned * 100))
}
function today() {
  const d = new Date(); const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

async function fetch() {
  loading.value = true
  try {
    const r = await listWorkOrders({ page: query.page, size: query.size, keyword: query.keyword || undefined, status: 'IN_PROGRESS' })
    data.value = r.data.records; total.value = r.data.total
  } finally { loading.value = false }
}
function search() { query.page = 1; fetch() }
function resetQuery() { query.keyword = ''; query.page = 1; fetch() }

function openForm(row) {
  currentOrder.value = row
  form.progressDate = today()
  form.processName = ''
  form.completedQuantity = Number(row.completedQuantity || 0)
  form.progressPercent = null
  form.recorder = ''
  form.remark = ''
  formVisible.value = true
}
function resetForm() { formRef.value?.clearValidate() }
async function save() {
  if (!await formRef.value.validate().catch(() => false)) return
  saving.value = true
  try {
    await addProductionProgress(currentOrder.value.id, {
      progressDate: form.progressDate,
      processName: form.processName,
      completedQuantity: form.completedQuantity,
      progressPercent: form.progressPercent,
      recorder: form.recorder,
      remark: form.remark,
    })
    const reached = Number(form.completedQuantity) >= Number(currentOrder.value.plannedQuantity)
    ElMessage.success(reached
      ? '报工完成，工单已自动转入待质检'
      : '进度已登记')
    formVisible.value = false
    fetch()
  } finally { saving.value = false }
}
async function openHistory(row) {
  const r = await getWorkOrder(row.id)
  history.value = r.data.progressRecords || []
  historyVisible.value = true
}
onMounted(async () => {
  await fetch()
  if (route.query.workOrderId) {
    const id = Number(route.query.workOrderId)
    const row = data.value.find(item => item.id === id)
    if (row) openForm(row)
  }
})
</script>

<style scoped>
.table-footer { display: flex; justify-content: flex-end; margin-top: var(--spacing-md); }
.muted { margin-left: 8px; font-size: 12px; color: var(--color-text-secondary); }
</style>