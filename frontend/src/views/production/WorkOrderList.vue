<template>
  <div class="page-container">
    <div class="page-header">
      <h2>生产工单</h2>
      <el-button type="primary" v-if="has('production:order:create')" @click="openCreate">手工建单</el-button>
    </div>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item><el-input v-model="query.keyword" placeholder="工单号" clearable style="width:180px" @keyup.enter="search"/></el-form-item>
        <el-form-item>
          <el-tag v-if="query.planId" closable @close="clearPlanFilter">生产计划筛选</el-tag>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
      </el-form>
    </div>

    <el-tabs v-model="activeStatus" style="margin-top:12px" @tab-change="onStatusChange">
      <el-tab-pane label="全部" name=""/>
      <el-tab-pane label="待生产" name="PENDING"/>
      <el-tab-pane label="生产中" name="IN_PROGRESS"/>
      <el-tab-pane label="待质检" name="PENDING_QC"/>
      <el-tab-pane label="返工中" name="REWORKING"/>
      <el-tab-pane label="已完成" name="COMPLETED"/>
    </el-tabs>

    <div class="table-card">
      <el-table :data="data" v-loading="loading" stripe>
        <el-table-column prop="workOrderNo" label="工单号" width="170"/>
        <el-table-column prop="productName" label="产品" min-width="110" show-overflow-tooltip/>
        <el-table-column prop="skuCode" label="SKU" width="145"/>
        <el-table-column prop="planNo" label="生产计划" width="155"><template #default="{row}">{{ row.planNo || '—' }}</template></el-table-column>
        <el-table-column prop="plannedQuantity" label="计划" width="75" align="right"/>
        <el-table-column label="生产进度" width="170">
          <template #default="{row}">
            <el-progress :percentage="percent(row)" :stroke-width="8"/>
            <span class="tiny">{{ row.completedQuantity }} / {{ row.plannedQuantity }}</span>
          </template>
        </el-table-column>
        <el-table-column label="合格" width="75" align="right"><template #default="{row}">{{ row.qualifiedQuantity }}</template></el-table-column>
        <el-table-column label="已检/待检" width="100" align="right">
          <template #default="{row}">{{ row.inspectedQuantity ?? 0 }} / {{ row.uninspectedQuantity ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="manager" label="负责人" width="90"><template #default="{row}">{{ row.manager || '—' }}</template></el-table-column>
        <el-table-column label="领料状态" width="95">
          <template #default="{row}">
            <el-tag :type="Number(row.issueCount) > 0 ? 'success' : 'warning'" size="small">
              {{ Number(row.issueCount) > 0 ? '已领料' : '未领料' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前阶段" width="105">
          <template #default="{row}">{{ stageLabel(row) }}</template>
        </el-table-column>
        <el-table-column prop="expectedEndDate" label="预计完成" width="105"><template #default="{row}">{{ row.expectedEndDate || '—' }}</template></el-table-column>
        <el-table-column label="状态" width="95">
          <template #default="{row}"><el-tag :type="statusMeta(row.status).type" size="small">{{ statusMeta(row.status).label }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{row}">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'PENDING' && Number(row.issueCount) === 0 && has('production:issue-material')"
                       link type="warning" @click="goIssue(row)">去领料</el-button>
            <el-button v-if="['PENDING','REWORKING'].includes(row.status) && Number(row.issueCount) > 0 && has('production:order:start')"
                       link type="success" @click="start(row)">{{ row.status === 'REWORKING' ? '开始返工' : '开始生产' }}</el-button>
            <el-button v-if="row.status === 'IN_PROGRESS' && has('production:order:report')"
                       link type="warning" @click="goProgress(row)">报工</el-button>
            <el-button v-if="['PENDING_QC','REWORKING'].includes(row.status) && has('production:qc-write')"
                       link type="primary" @click="goQc(row)">{{ row.status === 'REWORKING' ? '去复检' : '去质检' }}</el-button>
            <el-button v-if="row.status === 'PENDING' && has('production:order:cancel')"
                       link type="danger" @click="openCancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
                       :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="fetch"/>
      </div>
    </div>

    <el-dialog v-model="formVisible" title="手工创建生产工单" width="600px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="生产计划">
          <el-select v-model="form.planId" filterable clearable placeholder="可选，仅已下达计划可追加工单" style="width:100%" @change="onPlanChange">
            <el-option v-for="plan in plans" :key="plan.id"
                       :label="`${plan.planNo} · ${plan.productName} · 剩余 ${plan.remainingQuantity}`" :value="plan.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="产品" prop="productId">
          <el-select v-model="form.productId" filterable placeholder="请选择产品" style="width:100%" @change="form.skuId = null">
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
        <el-form-item label="负责人"><el-input v-model="form.manager" style="width:180px"/></el-form-item>
        <el-form-item label="开始日期"><el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width:180px"/></el-form-item>
        <el-form-item label="预计完成"><el-date-picker v-model="form.expectedEndDate" type="date" value-format="YYYY-MM-DD" style="width:180px"/></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="formVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="cancelVisible" title="取消工单" width="420px">
      <p>确定取消工单「{{ cancelRow?.workOrderNo }}」？</p>
      <el-input v-model="cancelReason" type="textarea" :rows="2" placeholder="取消原因" maxlength="500"/>
      <template #footer>
        <el-button @click="cancelVisible=false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="confirmCancel">确认取消</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="生产工单详情" size="820px">
      <div v-if="detail">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="工单号">{{ detail.workOrderNo }}</el-descriptions-item>
          <el-descriptions-item label="生产计划">{{ detail.planNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="statusMeta(detail.status).type" size="small">{{ statusMeta(detail.status).label }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="产品">{{ detail.productName }}</el-descriptions-item>
          <el-descriptions-item label="SKU">{{ detail.skuCode }}</el-descriptions-item>
          <el-descriptions-item label="负责人">{{ detail.manager || '—' }}</el-descriptions-item>
          <el-descriptions-item label="计划数量">{{ detail.plannedQuantity }}</el-descriptions-item>
          <el-descriptions-item label="已完成">{{ detail.completedQuantity }}</el-descriptions-item>
          <el-descriptions-item label="合格数量">{{ detail.qualifiedQuantity }}</el-descriptions-item>
          <el-descriptions-item label="实际开工">{{ detail.actualStartTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="实际完工">{{ detail.actualEndTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="取消原因">{{ detail.cancelReason || '—' }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs style="margin-top:16px">
          <el-tab-pane :label="`领料记录 (${detail.issues?.length || 0})`">
            <el-table :data="detail.issues || []" size="small" border>
              <el-table-column type="expand">
                <template #default="{ row }">
                  <div class="expand-box">
                    <el-table :data="row.items || []" size="small">
                      <el-table-column prop="batchNo" label="原材料批次" width="170"/>
                      <el-table-column prop="materialName" label="原材料" min-width="120"/>
                      <el-table-column prop="quantity" label="数量" width="90"/>
                      <el-table-column prop="unit" label="单位" width="70"/>
                    </el-table>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="issueNo" label="领料单号" width="170"/>
              <el-table-column prop="issueDate" label="领料日期" width="110"/>
              <el-table-column prop="issuer" label="领料人" width="100"/>
              <el-table-column prop="createdAt" label="登记时间" width="165"/>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="`生产进度 (${detail.progressRecords?.length || 0})`">
            <el-table :data="detail.progressRecords || []" size="small" border>
              <el-table-column prop="progressDate" label="日期" width="110"/>
              <el-table-column label="当前工序" min-width="140">
                <template #default="{row}">{{ processLabel(row.processName) }}</template>
              </el-table-column>
              <el-table-column prop="completedQuantity" label="累计完成" width="100"/>
              <el-table-column prop="progressPercent" label="进度%" width="90"/>
              <el-table-column prop="recorder" label="登记人" width="100"/>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="`成品质检 (${detail.inspections?.length || 0})`">
            <el-table :data="detail.inspections || []" size="small" border>
              <el-table-column prop="inspectionNo" label="质检单号" width="170"/>
              <el-table-column prop="inspectedQuantity" label="送检" width="80"/>
              <el-table-column prop="qualifiedQuantity" label="合格" width="80"/>
              <el-table-column prop="unqualifiedQuantity" label="不合格" width="80"/>
              <el-table-column label="结果" width="100"><template #default="{row}"><el-tag :type="resultMeta(row.result).type" size="small">{{ resultMeta(row.result).label }}</el-tag></template></el-table-column>
              <el-table-column prop="inspector" label="检验人" width="100"/>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="`成品批次 (${detail.batches?.length || 0})`">
            <el-table :data="detail.batches || []" size="small" border>
              <el-table-column type="expand">
                <template #default="{ row }">
                  <div class="expand-box">
                    <el-table :data="row.materials || []" size="small">
                      <el-table-column prop="batchNo" label="原材料批次" width="170"/>
                      <el-table-column prop="materialName" label="原材料" min-width="120"/>
                      <el-table-column prop="quantity" label="使用数量" width="100"/>
                      <el-table-column prop="unit" label="单位" width="70"/>
                    </el-table>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="batchNo" label="成品批次号" width="170"/>
              <el-table-column prop="quantity" label="数量" width="90"/>
              <el-table-column prop="productionDate" label="生产日期" width="110"/>
              <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="batchMeta(row.status).type" size="small">{{ batchMeta(row.status).label }}</el-tag></template></el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="状态轨迹">
            <el-table :data="statusLogs" size="small" border>
              <el-table-column prop="createdAt" label="时间" width="165"/>
              <el-table-column label="状态变化" min-width="190">
                <template #default="{row}">{{ row.fromStatus ? woLabel(row.fromStatus) + ' → ' : '' }}{{ woLabel(row.toStatus) }}</template>
              </el-table-column>
              <el-table-column prop="operatorName" label="操作人" width="100"/>
              <el-table-column prop="remark" label="说明" min-width="180" show-overflow-tooltip/>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  WORK_ORDER_STATUS, FINISHED_INSPECTION_RESULT,
  FINISHED_BATCH_STATUS, PRODUCTION_PROCESSES,
} from '@/utils/constants'
import {
  listWorkOrders, getWorkOrder, createWorkOrder, startWorkOrder,
  cancelWorkOrder, getWorkOrderStatusLogs, listProductionPlans,
} from '@/api/production'
import { listProducts } from '@/api/product'
import { listSkus } from '@/api/sku'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const has = permission => userStore.hasPermission(permission)

const loading = ref(false), saving = ref(false)
const data = ref([]), total = ref(0)
const activeStatus = ref('')
const query = reactive({ page: 1, size: 10, keyword: '', status: '', planId: null })
const plans = ref([]), products = ref([]), skus = ref([])
const formVisible = ref(false), formRef = ref(null)
const detailVisible = ref(false), detail = ref(null), statusLogs = ref([])
const cancelVisible = ref(false), cancelRow = ref(null), cancelReason = ref('')
const form = reactive({ planId: null, productId: null, skuId: null, plannedQuantity: 100, manager: '', startDate: '', expectedEndDate: '', remark: '' })
const rules = {
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  skuId: [{ required: true, message: '请选择SKU', trigger: 'change' }],
  plannedQuantity: [{ required: true, message: '请输入计划数量', trigger: 'blur' }],
}
const filteredSkus = computed(() => skus.value.filter(s => !form.productId || s.productId === form.productId))

function statusMeta(status) { return WORK_ORDER_STATUS[status] || { label: status || '未知', type: 'info' } }
function woLabel(status) { return WORK_ORDER_STATUS[status]?.label || status || '—' }
function resultMeta(result) { return FINISHED_INSPECTION_RESULT[result] || { label: result || '—', type: 'info' } }
function batchMeta(status) { return FINISHED_BATCH_STATUS[status] || { label: status || '未知', type: 'info' } }
function processLabel(value) {
  return PRODUCTION_PROCESSES.find(item => item.value === value)?.label || value || '—'
}
function stageLabel(row) {
  switch (row.status) {
    case 'PENDING': return '未开工'
    case 'IN_PROGRESS': return row.currentProcess ? processLabel(row.currentProcess) : '生产中'
    case 'PENDING_QC': return '完工待检'
    case 'REWORKING': return '返工中'
    case 'COMPLETED': return '已完成'
    case 'CANCELLED': return '已取消'
    default: return processLabel(row.currentProcess)
  }
}
function percent(row) {
  const planned = Number(row.plannedQuantity || 0)
  const completed = Number(row.completedQuantity || 0)
  if (!planned) return 0
  return Math.min(100, Math.round(completed / planned * 100))
}

async function fetch() {
  loading.value = true
  try {
    const r = await listWorkOrders({
      page: query.page, size: query.size,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      planId: query.planId || undefined,
    })
    data.value = r.data.records; total.value = r.data.total
  } finally { loading.value = false }
}
function search() { query.page = 1; fetch() }
function resetQuery() {
  query.keyword = ''; query.status = ''; query.planId = null
  activeStatus.value = ''; query.page = 1; fetch()
}
function onStatusChange(name) { query.status = name; query.page = 1; fetch() }
function clearPlanFilter() { query.planId = null; query.page = 1; fetch() }

async function fetchOptions() {
  const [planRes, productRes, skuRes] = await Promise.all([
    listProductionPlans({ page: 1, size: 1000 }),
    listProducts({ page: 1, size: 1000, status: 1 }),
    listSkus({ page: 1, size: 1000, status: 1 }),
  ])
  plans.value = (planRes.data.records || []).filter(p =>
    ['RELEASED', 'IN_PROGRESS'].includes(p.status) && Number(p.remainingQuantity) > 0)
  products.value = productRes.data.records || []
  skus.value = skuRes.data.records || []
}
function resetForm() {
  form.planId = null; form.productId = null; form.skuId = null; form.plannedQuantity = 100
  form.manager = ''; form.startDate = ''; form.expectedEndDate = ''; form.remark = ''
  formRef.value?.clearValidate()
}
async function openCreate() { resetForm(); await fetchOptions(); formVisible.value = true }
function onPlanChange(planId) {
  const plan = plans.value.find(p => p.id === planId)
  if (!plan) return
  form.productId = plan.productId
  form.skuId = plan.skuId
  form.plannedQuantity = Number(plan.remainingQuantity)
}
async function save() {
  if (!await formRef.value.validate().catch(() => false)) return
  saving.value = true
  try {
    await createWorkOrder({ ...form, planId: form.planId || null, expectedEndDate: form.expectedEndDate || null, startDate: form.startDate || null })
    ElMessage.success('工单已创建，状态为待生产'); formVisible.value = false; fetch()
  } finally { saving.value = false }
}
async function openDetail(row) {
  const [detailRes, logRes] = await Promise.all([
    getWorkOrder(row.id),
    getWorkOrderStatusLogs(row.id),
  ])
  detail.value = detailRes.data
  statusLogs.value = logRes.data || []
  detailVisible.value = true
}
async function start(row) {
  await startWorkOrder(row.id)
  ElMessage.success(row.status === 'REWORKING' ? '已开始返工' : '已开始生产')
  fetch()
}
function goIssue(row) {
  router.push({ path: '/production/issues', query: { workOrderId: row.id } })
}
function goProgress(row) {
  router.push({ path: '/production/progress', query: { workOrderId: row.id } })
}
function goQc(row) {
  router.push({ path: '/production/qc', query: { workOrderId: row.id } })
}
function openCancel(row) {
  cancelRow.value = row
  cancelReason.value = ''
  cancelVisible.value = true
}
async function confirmCancel() {
  saving.value = true
  try {
    await cancelWorkOrder(cancelRow.value.id, { reason: cancelReason.value })
    ElMessage.success('工单已取消'); cancelVisible.value = false; fetch()
  } finally { saving.value = false }
}

onMounted(async () => {
  if (route.query.planId) {
    query.planId = Number(route.query.planId)
  }
  await fetch()
})
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--spacing-lg); }
.page-header h2 { margin: 0; }
.table-footer { display: flex; justify-content: flex-end; margin-top: var(--spacing-md); }
.expand-box { padding: 8px 24px; background: #fafbfc; }
.tiny { font-size: 11px; color: var(--color-text-secondary); }
</style>