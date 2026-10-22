<template>
  <div class="page-container">
    <div class="page-header">
      <h2>售后管理</h2>
      <el-button type="primary" v-if="has('after-sales:handle')" @click="openCreate">新增售后</el-button>
    </div>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item><el-input v-model="query.keyword" placeholder="售后单号" clearable style="width:180px" @keyup.enter="search"/></el-form-item>
        <el-form-item>
          <el-select v-model="query.status" placeholder="状态" clearable style="width:130px">
            <el-option v-for="(meta, key) in AFTER_SALES_STATUS" :key="key" :label="meta.label" :value="key"/>
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
      </el-form>
    </div>

    <div class="table-card" style="margin-top:16px">
      <el-table :data="data" v-loading="loading" stripe>
        <el-table-column prop="recordNo" label="售后单号" width="180"/>
        <el-table-column prop="orderNo" label="销售订单" width="180"/>
        <el-table-column label="类型" width="90"><template #default="{row}">{{ typeLabel(row.type) }}</template></el-table-column>
        <el-table-column prop="applyDate" label="申请日期" width="110"/>
        <el-table-column label="状态" width="110"><template #default="{row}"><el-tag :type="statusMeta(row.status).type" size="small">{{ statusMeta(row.status).label }}</el-tag></template></el-table-column>
        <el-table-column prop="handler" label="经办人" width="100"><template #default="{row}">{{ row.handler || '—' }}</template></el-table-column>
        <el-table-column prop="conclusion" label="处理结论" min-width="160" show-overflow-tooltip/>
        <el-table-column prop="createdAt" label="创建时间" width="165"/>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{row}">
            <el-button v-if="has('after-sales:handle')" link type="primary" @click="openProcess(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
                       :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="fetch"/>
      </div>
    </div>

    <el-dialog v-model="formVisible" title="新增售后" width="620px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="销售订单" prop="salesOrderId">
          <el-select v-model="form.salesOrderId" filterable placeholder="请选择销售订单" style="width:100%" @change="form.shipmentId = null">
            <el-option v-for="order in orders" :key="order.id" :label="`${order.orderNo} · ${order.customerName}`" :value="order.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="发货单">
          <el-select v-model="form.shipmentId" filterable clearable placeholder="可选" style="width:100%">
            <el-option v-for="shipment in shipmentOptions" :key="shipment.id"
                       :label="`${shipment.shipmentNo} · ${shipment.shipmentDate}`" :value="shipment.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="成品批次">
          <el-select v-model="form.finishedBatchId" filterable clearable placeholder="可选" style="width:100%">
            <el-option v-for="batch in batches" :key="batch.id" :label="`${batch.batchNo} · ${batch.skuCode}`" :value="batch.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="售后类型" prop="type">
          <el-select v-model="form.type" style="width:180px">
            <el-option v-for="item in AFTER_SALES_TYPES" :key="item.value" :label="item.label" :value="item.value"/>
          </el-select>
        </el-form-item>
        <el-form-item label="申请日期" prop="applyDate">
          <el-date-picker v-model="form.applyDate" type="date" value-format="YYYY-MM-DD" style="width:180px"/>
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input v-model="form.description" type="textarea" :rows="3"/>
        </el-form-item>
        <el-form-item label="经办人"><el-input v-model="form.handler" style="width:180px"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="formVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="processVisible" title="处理售后" width="560px" @closed="resetProcessForm">
      <el-form ref="processFormRef" :model="processForm" :rules="processRules" label-width="100px">
        <el-form-item label="售后单号">{{ current?.recordNo }}</el-form-item>
        <el-form-item label="处理状态" prop="status">
          <el-select v-model="processForm.status" style="width:200px">
            <el-option v-for="(meta, key) in AFTER_SALES_STATUS" :key="key" :label="meta.label" :value="key"/>
          </el-select>
        </el-form-item>
        <el-form-item label="处理结论"><el-input v-model="processForm.conclusion" type="textarea" :rows="2"/></el-form-item>
        <el-form-item label="验收结果"><el-input v-model="processForm.acceptanceResult" placeholder="退货验收结果等"/></el-form-item>
        <el-form-item label="经办人"><el-input v-model="processForm.handler" style="width:180px"/></el-form-item>
        <el-form-item label="备注"><el-input v-model="processForm.remark" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <p class="muted">退货不会自动恢复可售库存，需要后续验收和入库流程处理。</p>
      <template #footer><el-button @click="processVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveProcess">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { AFTER_SALES_TYPES, AFTER_SALES_STATUS } from '@/utils/constants'
import { listAfterSales, createAfterSales, processAfterSales, listSalesOrders, listShipments } from '@/api/sales'
import { listFinishedBatches } from '@/api/production'

const userStore = useUserStore()
const has = permission => userStore.hasPermission(permission)

const loading = ref(false), saving = ref(false)
const data = ref([]), total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', status: '' })
const orders = ref([]), shipments = ref([]), batches = ref([])
const formVisible = ref(false), formRef = ref(null)
const processVisible = ref(false), processFormRef = ref(null), current = ref(null)
const form = reactive({ salesOrderId: null, shipmentId: null, finishedBatchId: null, type: 'RETURN', applyDate: '', description: '', handler: '' })
const processForm = reactive({ status: 'PROCESSING', conclusion: '', acceptanceResult: '', handler: '', remark: '' })
const rules = {
  salesOrderId: [{ required: true, message: '请选择销售订单', trigger: 'change' }],
  type: [{ required: true, message: '请选择售后类型', trigger: 'change' }],
  applyDate: [{ required: true, message: '请选择申请日期', trigger: 'change' }],
}
const processRules = {
  status: [{ required: true, message: '请选择处理状态', trigger: 'change' }],
}
const shipmentOptions = computed(() => shipments.value.filter(s => !form.salesOrderId || s.orderId === form.salesOrderId))

function typeLabel(type) { return AFTER_SALES_TYPES.find(item => item.value === type)?.label || type || '—' }
function statusMeta(status) { return AFTER_SALES_STATUS[status] || { label: status || '未知', type: 'info' } }
function today() { const d = new Date(); const p = n => String(n).padStart(2, '0'); return `${d.getFullYear()}-${p(d.getMonth()+1)}-${p(d.getDate())}` }

async function fetch() {
  loading.value = true
  try {
    const r = await listAfterSales({ page: query.page, size: query.size, keyword: query.keyword || undefined, status: query.status || undefined })
    data.value = r.data.records; total.value = r.data.total
  } finally { loading.value = false }
}
function search() { query.page = 1; fetch() }
function resetQuery() { query.keyword = ''; query.status = ''; query.page = 1; fetch() }

async function openCreate() {
  const [orderRes, shipmentRes, batchRes] = await Promise.all([
    listSalesOrders({ page: 1, size: 1000 }),
    listShipments({ page: 1, size: 1000 }),
    listFinishedBatches({ page: 1, size: 1000 }),
  ])
  orders.value = orderRes.data.records || []
  shipments.value = shipmentRes.data.records || []
  batches.value = batchRes.data.records || []
  form.salesOrderId = null; form.shipmentId = null; form.finishedBatchId = null
  form.type = 'RETURN'; form.applyDate = today(); form.description = ''; form.handler = ''
  formVisible.value = true
}
function resetForm() { formRef.value?.clearValidate() }
async function save() {
  if (!await formRef.value.validate().catch(() => false)) return
  saving.value = true
  try {
    await createAfterSales({ ...form })
    ElMessage.success('售后记录已创建'); formVisible.value = false; fetch()
  } finally { saving.value = false }
}
function openProcess(row) {
  current.value = row
  processForm.status = row.status
  processForm.conclusion = row.conclusion || ''
  processForm.acceptanceResult = row.acceptanceResult || ''
  processForm.handler = row.handler || ''
  processForm.remark = row.remark || ''
  processVisible.value = true
}
function resetProcessForm() { processFormRef.value?.clearValidate() }
async function saveProcess() {
  if (!await processFormRef.value.validate().catch(() => false)) return
  saving.value = true
  try {
    await processAfterSales(current.value.id, { ...processForm })
    ElMessage.success('售后处理已保存'); processVisible.value = false; fetch()
  } finally { saving.value = false }
}
onMounted(fetch)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--spacing-lg); }
.page-header h2 { margin: 0; }
.table-footer { display: flex; justify-content: flex-end; margin-top: var(--spacing-md); }
.muted { font-size: 12px; color: var(--color-text-secondary); }
</style>