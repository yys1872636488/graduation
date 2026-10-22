<template>
  <div class="page-container">
    <div class="page-header">
      <h2>成品质检</h2>
      <div>
        <el-button v-if="has('production:qc-write')" type="primary" @click="openInspection">新增成品质检</el-button>
        <el-button v-if="has('production:qc-write')" type="success" @click="openBatch">生成成品批次</el-button>
      </div>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="待质检任务" name="tasks">
        <div class="table-card" style="margin-top:8px">
          <el-table :data="taskOrders" v-loading="taskLoading" stripe>
            <el-table-column prop="workOrderNo" label="工单号" width="180"/>
            <el-table-column label="产品/SKU" min-width="180">
              <template #default="{row}">{{ row.productName }} / {{ row.skuCode }}</template>
            </el-table-column>
            <el-table-column prop="plannedQuantity" label="计划" width="80" align="right"/>
            <el-table-column prop="completedQuantity" label="完成" width="80" align="right"/>
            <el-table-column label="已检/待检" width="110" align="right">
              <template #default="{row}">{{ row.inspectedQuantity ?? 0 }} / {{ row.uninspectedQuantity ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{row}"><el-tag :type="woMeta(row.status).type" size="small">{{ woMeta(row.status).label }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="expectedEndDate" label="预计完成" width="110"/>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{row}">
                <el-button v-if="has('production:qc-write')" link type="primary" @click="openInspectionFor(row)">
                  {{ row.status === 'REWORKING' ? '复检' : '质检' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="待生成批次" name="pendingBatches">
        <div class="table-card" style="margin-top:8px">
          <el-table :data="pendingBatchOrders" stripe>
            <el-table-column prop="workOrderNo" label="工单号" width="180"/>
            <el-table-column label="产品/SKU" min-width="180">
              <template #default="{row}">{{ row.productName }} / {{ row.skuCode }}</template>
            </el-table-column>
            <el-table-column prop="qualifiedQuantity" label="合格数量" width="100" align="right"/>
            <el-table-column label="已生成" width="90" align="right">
              <template #default="{row}">{{ row.batchedQuantity ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="待生成" width="90" align="right">
              <template #default="{row}">{{ row.pendingBatchQuantity ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{row}"><el-tag :type="woMeta(row.status).type" size="small">{{ woMeta(row.status).label }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{row}">
                <el-button v-if="has('production:qc-write')" link type="primary" @click="openBatchFor(row)">生成批次</el-button>
              </template>
            </el-table-column>
          </el-table>
          <p v-if="!pendingBatchOrders.length" class="muted" style="margin-top:10px">
            暂无可生成批次的合格成品。
          </p>
        </div>
      </el-tab-pane>

      <el-tab-pane label="质检记录" name="inspections">
        <div class="search-card">
          <el-form :model="query" inline>
            <el-form-item><el-input v-model="query.keyword" placeholder="质检单号" clearable style="width:200px" @keyup.enter="search"/></el-form-item>
            <el-form-item><el-button type="primary" @click="search">查询</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
          </el-form>
        </div>
        <div class="table-card" style="margin-top:16px">
          <el-table :data="inspections" v-loading="loading" stripe>
            <el-table-column prop="inspectionNo" label="质检单号" width="180"/>
            <el-table-column prop="workOrderNo" label="生产工单" width="180"/>
            <el-table-column label="产品/SKU" min-width="180">
              <template #default="{row}">{{ row.productName }} / {{ row.skuCode }}</template>
            </el-table-column>
            <el-table-column prop="inspectedQuantity" label="送检" width="80" align="right"/>
            <el-table-column prop="qualifiedQuantity" label="合格" width="80" align="right"/>
            <el-table-column prop="unqualifiedQuantity" label="不合格" width="90" align="right"/>
            <el-table-column label="结果" width="100">
              <template #default="{row}"><el-tag :type="resultMeta(row.result).type" size="small">{{ resultMeta(row.result).label }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="inspector" label="检验人" width="100"/>
            <el-table-column prop="inspectionTime" label="检验时间" width="165"/>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{row}">
                <el-button v-if="Number(row.unqualifiedQuantity) > 0 && has('production:qc-review')"
                           link type="danger" @click="openDisposition(row)">不合格处理</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="table-footer">
            <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
                           :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="fetch"/>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="成品批次" name="batches">
        <div class="search-card">
          <el-form :model="batchQuery" inline>
            <el-form-item><el-input v-model="batchQuery.keyword" placeholder="成品批次号" clearable style="width:200px" @keyup.enter="fetchBatches"/></el-form-item>
            <el-form-item>
              <el-select v-model="batchQuery.status" placeholder="状态" clearable style="width:130px">
                <el-option v-for="(meta, key) in FINISHED_BATCH_STATUS" :key="key" :label="meta.label" :value="key"/>
              </el-select>
            </el-form-item>
            <el-form-item><el-button type="primary" @click="fetchBatches">查询</el-button><el-button @click="resetBatchQuery">重置</el-button></el-form-item>
          </el-form>
        </div>
        <div class="table-card" style="margin-top:16px">
          <el-table :data="batches" v-loading="batchLoading" stripe>
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="expand-box">
                  <el-table :data="row.materials || []" size="small">
                    <el-table-column prop="batchNo" label="原材料批次" width="180"/>
                    <el-table-column prop="materialName" label="原材料" min-width="140"/>
                    <el-table-column prop="quantity" label="使用数量" width="100"/>
                    <el-table-column prop="unit" label="单位" width="70"/>
                  </el-table>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="batchNo" label="成品批次号" width="180"/>
            <el-table-column prop="workOrderNo" label="生产工单" width="180"/>
            <el-table-column label="产品/SKU" min-width="180">
              <template #default="{row}">{{ row.productName }} / {{ row.skuCode }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="90" align="right"/>
            <el-table-column prop="productionDate" label="生产日期" width="110"/>
            <el-table-column label="状态" width="110">
              <template #default="{row}"><el-tag :type="batchMeta(row.status).type" size="small">{{ batchMeta(row.status).label }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="165"/>
          </el-table>
          <div class="table-footer">
            <el-pagination v-model:current-page="batchQuery.page" v-model:page-size="batchQuery.size" :total="batchTotal"
                           :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="fetchBatches"/>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="inspectionVisible" title="新增成品质检" width="600px" @closed="resetInspectionForm">
      <el-form ref="inspectionFormRef" :model="inspectionForm" :rules="inspectionRules" label-width="100px">
        <el-form-item label="生产工单" prop="workOrderId">
          <el-select v-model="inspectionForm.workOrderId" filterable placeholder="选择待质检/返工工单" style="width:100%" @change="onInspectionOrderChange">
            <el-option v-for="order in inspectableOrders" :key="order.id"
                       :label="`${order.workOrderNo} · ${order.productName} · 待检 ${order.uninspectedQuantity}`" :value="order.id"/>
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedInspectionOrder" label="数量信息">
          <div class="qty-info">
            <span>已完成 {{ selectedInspectionOrder.completedQuantity }}</span>
            <span>已送检 {{ selectedInspectionOrder.inspectedQuantity ?? 0 }}</span>
            <span>待送检 {{ selectedInspectionOrder.uninspectedQuantity ?? 0 }}</span>
          </div>
        </el-form-item>
        <el-form-item label="送检数量" prop="inspectedQuantity">
          <el-input-number v-model="inspectionForm.inspectedQuantity" :min="1"
                           :max="Number(selectedInspectionOrder?.uninspectedQuantity || undefined)"
                           :precision="0" style="width:180px"/>
        </el-form-item>
        <el-form-item label="合格数量" prop="qualifiedQuantity">
          <el-input-number v-model="inspectionForm.qualifiedQuantity" :min="0" :precision="0" style="width:180px"/>
        </el-form-item>
        <el-form-item label="不合格数量" prop="unqualifiedQuantity">
          <el-input-number v-model="inspectionForm.unqualifiedQuantity" :min="0" :precision="0" style="width:180px"/>
          <span class="muted">合格 + 不合格 = 送检</span>
        </el-form-item>
        <el-form-item label="检验人"><el-input v-model="inspectionForm.inspector" style="width:180px"/></el-form-item>
        <el-form-item label="检验时间">
          <el-date-picker v-model="inspectionForm.inspectionTime" type="datetime"
                          value-format="YYYY-MM-DD HH:mm:ss" style="width:220px"/>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="inspectionForm.remark" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="inspectionVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveInspection">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="dispositionVisible" title="不合格品处理" width="520px" @closed="resetDispositionForm">
      <el-form ref="dispositionFormRef" :model="dispositionForm" :rules="dispositionRules" label-width="100px">
        <el-form-item label="质检单号">{{ currentInspection?.inspectionNo }}</el-form-item>
        <el-form-item label="不合格数量">{{ currentInspection?.unqualifiedQuantity }}</el-form-item>
        <el-form-item label="处理类型" prop="type">
          <el-radio-group v-model="dispositionForm.type">
            <el-radio v-for="item in QUALITY_DISPOSITION_TYPES" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理数量" prop="quantity">
          <el-input-number v-model="dispositionForm.quantity" :min="0.01"
                           :max="Number(currentInspection?.unqualifiedQuantity || 0)" :precision="2" style="width:180px"/>
        </el-form-item>
        <el-form-item label="原因"><el-input v-model="dispositionForm.reason" type="textarea" :rows="2"/></el-form-item>
        <el-form-item label="处理人"><el-input v-model="dispositionForm.handler" style="width:180px"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="dispositionVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveDisposition">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="batchVisible" title="生成成品批次" width="540px">
      <el-form label-width="100px">
        <el-form-item label="生产工单">
          <el-select v-model="batchWorkOrderId" filterable placeholder="选择有合格数量的工单"
                     style="width:100%" @change="onBatchOrderChange">
            <el-option v-for="order in batchableOrders" :key="order.id"
                       :label="`${order.workOrderNo} · ${order.productName} · 待生成 ${order.pendingBatchQuantity}`" :value="order.id"/>
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedBatchOrder" label="数量信息">
          <div class="qty-info">
            <span>合格 {{ selectedBatchOrder.qualifiedQuantity }}</span>
            <span>已生成 {{ selectedBatchOrder.batchedQuantity ?? 0 }}</span>
            <span>待生成 {{ selectedBatchOrder.pendingBatchQuantity ?? 0 }}</span>
          </div>
        </el-form-item>
        <el-form-item label="本次数量">
          <el-input-number v-model="batchQuantity" :min="1"
                           :max="Number(selectedBatchOrder?.pendingBatchQuantity || 1)"
                           :precision="0" style="width:180px"/>
        </el-form-item>
        <el-form-item label="生产日期">
          <el-date-picker v-model="batchProductionDate" type="date" value-format="YYYY-MM-DD" style="width:180px"/>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="batchRemark" type="textarea" :rows="2" maxlength="500"/></el-form-item>
      </el-form>
      <p class="muted">一个工单可以分多次生成成品批次，每次都会关联该工单使用的原材料批次。</p>
      <template #footer><el-button @click="batchVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveBatch">生成批次</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  FINISHED_INSPECTION_RESULT, FINISHED_BATCH_STATUS,
  QUALITY_DISPOSITION_TYPES, WORK_ORDER_STATUS,
} from '@/utils/constants'
import {
  listFinishedInspections, listFinishedBatches, listWorkOrders,
  addFinishedInspection, addQualityDisposition, createFinishedBatch,
} from '@/api/production'

const route = useRoute()
const userStore = useUserStore()
const has = permission => userStore.hasPermission(permission)

const tab = ref('inspections')
const loading = ref(false), saving = ref(false)
const inspections = ref([]), total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })
const batches = ref([]), batchTotal = ref(0), batchLoading = ref(false)
const batchQuery = reactive({ page: 1, size: 10, keyword: '', status: '' })

const allOrders = ref([])
const taskOrders = ref([]), taskLoading = ref(false)
const inspectionVisible = ref(false), inspectionFormRef = ref(null)
const inspectionForm = reactive({ workOrderId: null, inspectedQuantity: 1, qualifiedQuantity: 1, unqualifiedQuantity: 0, inspector: '', inspectionTime: '', remark: '' })
const inspectionRules = {
  workOrderId: [{ required: true, message: '请选择生产工单', trigger: 'change' }],
  inspectedQuantity: [{ required: true, message: '请输入送检数量', trigger: 'blur' }],
  qualifiedQuantity: [{ required: true, message: '请输入合格数量', trigger: 'blur' }],
  unqualifiedQuantity: [{ required: true, message: '请输入不合格数量', trigger: 'blur' }],
}

const dispositionVisible = ref(false), dispositionFormRef = ref(null), currentInspection = ref(null)
const dispositionForm = reactive({ type: 'REWORK', quantity: 1, reason: '', handler: '' })
const dispositionRules = {
  type: [{ required: true, message: '请选择处理类型', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入处理数量', trigger: 'blur' }],
}

const batchVisible = ref(false), batchWorkOrderId = ref(null)
const batchQuantity = ref(1), batchProductionDate = ref(''), batchRemark = ref('')

const inspectableOrders = computed(() => allOrders.value.filter(o => ['PENDING_QC', 'REWORKING'].includes(o.status)))
const pendingBatchOrders = computed(() => allOrders.value.filter(o =>
  Number(o.pendingBatchQuantity) > 0 && ['COMPLETED', 'REWORKING'].includes(o.status)))
const batchableOrders = computed(() => pendingBatchOrders.value)
const selectedInspectionOrder = computed(() =>
  allOrders.value.find(o => o.id === inspectionForm.workOrderId) || null)
const selectedBatchOrder = computed(() =>
  allOrders.value.find(o => o.id === batchWorkOrderId.value) || null)

function resultMeta(result) { return FINISHED_INSPECTION_RESULT[result] || { label: result || '—', type: 'info' } }
function batchMeta(status) { return FINISHED_BATCH_STATUS[status] || { label: status || '未知', type: 'info' } }
function woMeta(status) { return WORK_ORDER_STATUS[status] || { label: status || '未知', type: 'info' } }

async function fetch() {
  loading.value = true
  try {
    const r = await listFinishedInspections({ page: query.page, size: query.size, keyword: query.keyword || undefined })
    inspections.value = r.data.records; total.value = r.data.total
  } finally { loading.value = false }
}
function search() { query.page = 1; fetch() }
function resetQuery() { query.keyword = ''; query.page = 1; fetch() }

async function fetchBatches() {
  batchLoading.value = true
  try {
    const r = await listFinishedBatches({ page: batchQuery.page, size: batchQuery.size, keyword: batchQuery.keyword || undefined, status: batchQuery.status || undefined })
    batches.value = r.data.records; batchTotal.value = r.data.total
  } finally { batchLoading.value = false }
}
function resetBatchQuery() { batchQuery.keyword = ''; batchQuery.status = ''; batchQuery.page = 1; fetchBatches() }

async function loadOrders() {
  taskLoading.value = true
  try {
    const r = await listWorkOrders({ page: 1, size: 1000 })
    allOrders.value = r.data.records || []
    taskOrders.value = allOrders.value.filter(o => ['PENDING_QC', 'REWORKING'].includes(o.status))
  } finally { taskLoading.value = false }
}

function resetInspectionFields() {
  inspectionForm.workOrderId = null
  inspectionForm.inspectedQuantity = 1
  inspectionForm.qualifiedQuantity = 1
  inspectionForm.unqualifiedQuantity = 0
  inspectionForm.inspector = ''
  inspectionForm.inspectionTime = ''
  inspectionForm.remark = ''
}

async function openInspection() {
  await loadOrders()
  resetInspectionFields()
  inspectionVisible.value = true
}

async function openInspectionFor(order) {
  if (!allOrders.value.length) await loadOrders()
  resetInspectionFields()
  inspectionForm.workOrderId = order.id
  onInspectionOrderChange(order.id)
  inspectionVisible.value = true
}

function onInspectionOrderChange(orderId) {
  const order = allOrders.value.find(o => o.id === orderId)
  if (!order) return
  const pending = Number(order.uninspectedQuantity || 0)
  inspectionForm.inspectedQuantity = pending > 0 ? pending : 1
  inspectionForm.qualifiedQuantity = pending > 0 ? pending : 1
  inspectionForm.unqualifiedQuantity = 0
}
function resetInspectionForm() { inspectionFormRef.value?.clearValidate() }
async function saveInspection() {
  if (!await inspectionFormRef.value.validate().catch(() => false)) return
  if (Number(inspectionForm.qualifiedQuantity) + Number(inspectionForm.unqualifiedQuantity) !== Number(inspectionForm.inspectedQuantity)) {
    ElMessage.warning('合格数量与不合格数量之和必须等于送检数量'); return
  }
  saving.value = true
  try {
    const orderId = inspectionForm.workOrderId
    await addFinishedInspection(orderId, {
      ...inspectionForm,
      inspectionTime: inspectionForm.inspectionTime || null,
    })
    inspectionVisible.value = false
    fetch()
    await loadOrders()
    const updated = allOrders.value.find(o => o.id === orderId)
    const pending = Number(updated?.pendingBatchQuantity || 0)
    if (pending > 0) {
      tab.value = 'pendingBatches'
      ElMessage.success(`质检完成，还有 ${pending} 件合格品待生成成品批次`)
    } else {
      ElMessage.success('成品质检已登记')
    }
  } finally { saving.value = false }
}

function openDisposition(row) {
  currentInspection.value = row
  dispositionForm.type = 'REWORK'
  dispositionForm.quantity = Number(row.unqualifiedQuantity)
  dispositionForm.reason = ''
  dispositionForm.handler = ''
  dispositionVisible.value = true
}
function resetDispositionForm() { dispositionFormRef.value?.clearValidate() }
async function saveDisposition() {
  if (!await dispositionFormRef.value.validate().catch(() => false)) return
  saving.value = true
  try {
    await addQualityDisposition({
      inspectionId: currentInspection.value.id,
      type: dispositionForm.type,
      quantity: dispositionForm.quantity,
      reason: dispositionForm.reason,
      handler: dispositionForm.handler,
    })
    ElMessage.success('处理记录已保存'); dispositionVisible.value = false; fetch(); loadOrders()
  } finally { saving.value = false }
}

function today() {
  const d = new Date(); const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}
async function openBatch() {
  await loadOrders()
  batchWorkOrderId.value = null
  batchQuantity.value = 1
  batchProductionDate.value = today()
  batchRemark.value = ''
  batchVisible.value = true
}
async function openBatchFor(order) {
  if (!allOrders.value.length) await loadOrders()
  batchWorkOrderId.value = order.id
  onBatchOrderChange(order.id)
  batchProductionDate.value = today()
  batchRemark.value = ''
  batchVisible.value = true
}
function onBatchOrderChange(orderId) {
  const order = allOrders.value.find(o => o.id === orderId)
  batchQuantity.value = Number(order?.pendingBatchQuantity || 1)
}
async function saveBatch() {
  if (!batchWorkOrderId.value) { ElMessage.warning('请选择生产工单'); return }
  if (!batchQuantity.value || Number(batchQuantity.value) <= 0) {
    ElMessage.warning('请输入本次批次数量'); return
  }
  saving.value = true
  try {
    const r = await createFinishedBatch(batchWorkOrderId.value, {
      quantity: batchQuantity.value,
      productionDate: batchProductionDate.value || null,
      remark: batchRemark.value,
    })
    ElMessage.success(`成品批次已生成：${r.data.batchNo}`)
    batchVisible.value = false
    fetchBatches()
    await loadOrders()
  } finally { saving.value = false }
}

onMounted(async () => {
  fetch()
  fetchBatches()
  await loadOrders()
  if (route.query.workOrderId) {
    tab.value = 'tasks'
    const order = allOrders.value.find(o => o.id === Number(route.query.workOrderId))
    if (order) openInspectionFor(order)
  }
})
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--spacing-lg); }
.page-header h2 { margin: 0; }
.table-footer { display: flex; justify-content: flex-end; margin-top: var(--spacing-md); }
.expand-box { padding: 8px 24px; background: #fafbfc; }
.muted { margin-left: 8px; font-size: 12px; color: var(--color-text-secondary); }
.qty-info { display: flex; gap: 16px; font-size: 12px; color: var(--color-text-secondary); }
</style>