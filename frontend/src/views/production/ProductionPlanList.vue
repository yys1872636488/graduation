<template>
  <div class="page-container">
    <div class="page-header">
      <h2>生产计划</h2>
      <el-button type="primary" v-if="has('production:plan:create')" @click="openCreate">新增计划</el-button>
    </div>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item><el-input v-model="query.keyword" placeholder="计划号" clearable style="width:180px" @keyup.enter="search"/></el-form-item>
        <el-form-item>
          <el-select v-model="query.status" placeholder="状态" clearable style="width:130px">
            <el-option v-for="(meta, key) in PRODUCTION_PLAN_STATUS" :key="key" :label="meta.label" :value="key"/>
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
      </el-form>
    </div>

    <div class="table-card" style="margin-top:16px">
      <el-table :data="data" v-loading="loading" stripe>
        <el-table-column prop="planNo" label="计划号" width="170"/>
        <el-table-column prop="productName" label="产品" min-width="130" show-overflow-tooltip/>
        <el-table-column prop="skuCode" label="SKU" width="150"/>
        <el-table-column prop="plannedQuantity" label="计划数量" width="90" align="right"/>
        <el-table-column label="已生成/剩余" width="130" align="right">
          <template #default="{row}">
            {{ row.workOrderQuantity ?? 0 }} / {{ row.remainingQuantity ?? row.plannedQuantity }}
          </template>
        </el-table-column>
        <el-table-column prop="workOrderCount" label="工单数" width="80" align="center"/>
        <el-table-column prop="startDate" label="开始日期" width="110"/>
        <el-table-column label="结束日期" width="110"><template #default="{row}">{{ row.endDate || '—' }}</template></el-table-column>
        <el-table-column prop="priority" label="优先级" width="90">
          <template #default="{row}">{{ priorityLabel(row.priority) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{row}"><el-tag :type="statusMeta(row.status).type" size="small">{{ statusMeta(row.status).label }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{row}">
            <el-button v-if="row.status==='DRAFT' && has('production:plan:edit')" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="canRelease(row) && has('production:plan:release')" link type="success" @click="openRelease(row)">
              {{ row.status === 'DRAFT' ? '下达' : '追加工单' }}
            </el-button>
            <el-button v-if="row.workOrderCount > 0" link type="primary" @click="goWorkOrders(row)">查看工单</el-button>
            <el-button link type="info" @click="openLogs(row)">轨迹</el-button>
            <el-button v-if="['DRAFT','RELEASED'].includes(row.status) && has('production:plan:edit')" link type="danger" @click="cancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
                       :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="fetch"/>
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="editId ? '编辑生产计划' : '新增生产计划'" width="560px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="产品" prop="productId">
          <el-select v-model="form.productId" filterable placeholder="请选择产品" style="width:100%" @change="onProductChange">
            <el-option v-for="p in products" :key="p.id" :label="`${p.productCode} ${p.name}`" :value="p.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="SKU" prop="skuId">
          <el-select v-model="form.skuId" filterable placeholder="请选择SKU" style="width:100%" :disabled="!form.productId">
            <el-option v-for="s in filteredSkus" :key="s.id" :label="`${s.skuCode} ${s.color || ''} ${s.sizeLabel || ''}`" :value="s.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="计划数量" prop="plannedQuantity">
          <el-input-number v-model="form.plannedQuantity" :min="1" :precision="0" style="width:180px"/>
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width:180px"/>
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width:180px"/>
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="form.priority">
            <el-radio value="LOW">低</el-radio>
            <el-radio value="NORMAL">普通</el-radio>
            <el-radio value="HIGH">高</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="formVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="releaseVisible" title="下达 / 追加工单" width="480px">
      <el-descriptions v-if="releasePlan" :column="1" border>
        <el-descriptions-item label="计划号">{{ releasePlan.planNo }}</el-descriptions-item>
        <el-descriptions-item label="计划数量">{{ releasePlan.plannedQuantity }}</el-descriptions-item>
        <el-descriptions-item label="已生成工单">{{ releasePlan.workOrderQuantity ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="剩余可生成">{{ releasePlan.remainingQuantity ?? 0 }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="100px" style="margin-top:16px">
        <el-form-item label="本次工单数量">
          <el-input-number v-model="releaseQuantity" :min="1"
                           :max="Number(releasePlan?.remainingQuantity || 1)"
                           :precision="0" style="width:180px"/>
        </el-form-item>
      </el-form>
      <p class="muted">下达后生成“待生产”工单，可在生产工单页面开始生产。</p>
      <template #footer>
        <el-button @click="releaseVisible=false">取消</el-button>
        <el-button type="primary" :loading="releasing" @click="confirmRelease">确认下达</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="logVisible" title="计划状态轨迹" size="520px">
      <el-table :data="logs" size="small" border>
        <el-table-column label="时间" width="165"><template #default="{row}">{{ row.createdAt }}</template></el-table-column>
        <el-table-column label="状态变化" min-width="180">
          <template #default="{row}">
            {{ row.fromStatus ? planLabel(row.fromStatus) + ' → ' : '' }}{{ planLabel(row.toStatus) }}
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="100"/>
        <el-table-column prop="remark" label="说明" min-width="160" show-overflow-tooltip/>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { PRODUCTION_PLAN_STATUS } from '@/utils/constants'
import {
  listProductionPlans, createProductionPlan, updateProductionPlan,
  releaseProductionPlan, cancelProductionPlan, getProductionPlanStatusLogs,
} from '@/api/production'
import { listProducts } from '@/api/product'
import { listSkus } from '@/api/sku'

const router = useRouter()
const userStore = useUserStore()
const has = permission => userStore.hasPermission(permission)

const loading = ref(false), saving = ref(false), releasing = ref(false)
const data = ref([]), total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', status: '' })
const products = ref([]), skus = ref([])
const formVisible = ref(false), formRef = ref(null), editId = ref(null)
const form = reactive({ productId: null, skuId: null, plannedQuantity: 100, startDate: '', endDate: '', priority: 'NORMAL', remark: '' })
const rules = {
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  skuId: [{ required: true, message: '请选择SKU', trigger: 'change' }],
  plannedQuantity: [{ required: true, message: '请输入计划数量', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
}

const releaseVisible = ref(false), releasePlan = ref(null), releaseQuantity = ref(1)
const logVisible = ref(false), logs = ref([])

const filteredSkus = computed(() => skus.value.filter(s => !form.productId || s.productId === form.productId))

function statusMeta(status) { return PRODUCTION_PLAN_STATUS[status] || { label: status || '未知', type: 'info' } }
function planLabel(status) { return PRODUCTION_PLAN_STATUS[status]?.label || status || '—' }
function priorityLabel(value) { return { LOW: '低', NORMAL: '普通', HIGH: '高' }[value] || value || '—' }
function canRelease(row) {
  if (row.status === 'DRAFT') return true
  return ['RELEASED', 'IN_PROGRESS'].includes(row.status)
    && Number(row.remainingQuantity || 0) > 0
}

async function fetch() {
  loading.value = true
  try {
    const r = await listProductionPlans({ page: query.page, size: query.size, keyword: query.keyword || undefined, status: query.status || undefined })
    data.value = r.data.records
    total.value = r.data.total
  } finally { loading.value = false }
}
function search() { query.page = 1; fetch() }
function resetQuery() { query.keyword = ''; query.status = ''; query.page = 1; fetch() }

async function fetchOptions() {
  const [p, s] = await Promise.all([
    listProducts({ page: 1, size: 1000, status: 1 }),
    listSkus({ page: 1, size: 1000, status: 1 }),
  ])
  products.value = p.data.records || []
  skus.value = s.data.records || []
}
function resetForm() {
  editId.value = null
  form.productId = null; form.skuId = null; form.plannedQuantity = 100
  form.startDate = ''; form.endDate = ''; form.priority = 'NORMAL'; form.remark = ''
  formRef.value?.clearValidate()
}
function onProductChange() { form.skuId = null }
async function openCreate() { resetForm(); await fetchOptions(); formVisible.value = true }
async function openEdit(row) {
  await fetchOptions()
  editId.value = row.id
  form.productId = row.productId; form.skuId = row.skuId
  form.plannedQuantity = Number(row.plannedQuantity)
  form.startDate = row.startDate; form.endDate = row.endDate || ''
  form.priority = row.priority; form.remark = row.remark || ''
  formVisible.value = true
}
async function save() {
  if (!await formRef.value.validate().catch(() => false)) return
  saving.value = true
  try {
    const payload = { ...form, endDate: form.endDate || null }
    if (editId.value) { await updateProductionPlan(editId.value, payload); ElMessage.success('计划已更新') }
    else { await createProductionPlan(payload); ElMessage.success('计划已创建') }
    formVisible.value = false; fetch()
  } finally { saving.value = false }
}

function openRelease(row) {
  releasePlan.value = row
  releaseQuantity.value = Number(row.remainingQuantity || row.plannedQuantity || 0)
  releaseVisible.value = true
}
async function confirmRelease() {
  if (!releaseQuantity.value || releaseQuantity.value <= 0) { ElMessage.warning('请输入工单数量'); return }
  releasing.value = true
  try {
    const r = await releaseProductionPlan(releasePlan.value.id, releaseQuantity.value)
    ElMessage.success(`已生成工单 ${r.data.workOrderNo}，状态为待生产`)
    releaseVisible.value = false
    fetch()
  } finally { releasing.value = false }
}

function goWorkOrders(row) {
  router.push({ path: '/production/orders', query: { planId: row.id } })
}
async function openLogs(row) {
  const r = await getProductionPlanStatusLogs(row.id)
  logs.value = r.data || []
  logVisible.value = true
}
async function cancel(row) {
  const ok = await ElMessageBox.confirm(`确定取消计划「${row.planNo}」？未开工工单会同步取消。`, '取消确认', { type: 'warning' }).catch(() => false)
  if (!ok) return
  await cancelProductionPlan(row.id); ElMessage.success('计划已取消'); fetch()
}
onMounted(fetch)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--spacing-lg); }
.page-header h2 { margin: 0; }
.table-footer { display: flex; justify-content: flex-end; margin-top: var(--spacing-md); }
.muted { font-size: 12px; color: var(--color-text-secondary); }
</style>