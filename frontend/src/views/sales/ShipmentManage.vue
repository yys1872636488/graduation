<template>
  <div class="page-container">
    <div class="page-header">
      <h2>发货管理</h2>
      <el-button type="primary" v-if="has('sales:ship')" @click="openCreate">新增发货</el-button>
    </div>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item><el-input v-model="query.keyword" placeholder="发货单号" clearable style="width:200px" @keyup.enter="search"/></el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
      </el-form>
    </div>

    <div class="table-card" style="margin-top:16px">
      <el-table :data="data" v-loading="loading" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-box">
              <el-table :data="row.items || []" size="small">
                <el-table-column prop="batchNo" label="成品批次号" width="180"/>
                <el-table-column prop="skuCode" label="SKU" width="160"/>
                <el-table-column prop="quantity" label="发货数量" width="100"/>
              </el-table>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="shipmentNo" label="发货单号" width="180"/>
        <el-table-column prop="orderNo" label="销售订单" width="180"/>
        <el-table-column prop="customerName" label="客户" min-width="140" show-overflow-tooltip/>
        <el-table-column prop="shipmentDate" label="发货日期" width="110"/>
        <el-table-column prop="logisticsNo" label="物流单号" min-width="140"><template #default="{row}">{{ row.logisticsNo || '—' }}</template></el-table-column>
        <el-table-column prop="operator" label="经办人" width="100"><template #default="{row}">{{ row.operator || '—' }}</template></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="165"/>
      </el-table>
      <div class="table-footer">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
                       :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="fetch"/>
      </div>
    </div>

    <el-dialog v-model="formVisible" title="新增发货" width="800px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="销售订单" prop="orderId">
              <el-select v-model="form.orderId" filterable placeholder="选择待发货订单" style="width:100%" @change="onOrderChange">
                <el-option v-for="order in orders" :key="order.id"
                           :label="`${order.orderNo} · ${order.customerName}`" :value="order.id"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发货日期" prop="shipmentDate">
              <el-date-picker v-model="form.shipmentDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="物流单号"><el-input v-model="form.logisticsNo"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="经办人"><el-input v-model="form.operator"/></el-form-item></el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2"/></el-form-item>
        <el-form-item label="发货明细">
          <el-table :data="form.items" size="small" border>
            <el-table-column label="产品/SKU" min-width="170">
              <template #default="{row}">{{ row.productName }} / {{ row.skuCode }}</template>
            </el-table-column>
            <el-table-column label="待发数量" width="90"><template #default="{row}">{{ row.remaining }}</template></el-table-column>
            <el-table-column label="成品批次" min-width="220">
              <template #default="{row}">
                <el-select v-model="row.finishedBatchId" filterable clearable placeholder="选择可售批次" style="width:100%">
                  <el-option v-for="batch in batchOptions(row.skuId)" :key="batch.id"
                             :label="`${batch.batchNo} · 数量 ${batch.quantity}`" :value="batch.id"/>
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="发货数量" width="130">
              <template #default="{row}"><el-input-number v-model="row.quantity" :min="0" :max="Number(row.remaining)" :precision="0" :controls="false" style="width:100%"/></template>
            </el-table-column>
          </el-table>
          <p class="muted">只提交已选择成品批次且数量大于 0 的明细；发货会扣减成品库存并生成流水。</p>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="formVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">确认发货</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { listShipments, createShipment, listSalesOrders, getSalesOrder } from '@/api/sales'
import { listFinishedBatches } from '@/api/production'

const userStore = useUserStore()
const has = permission => userStore.hasPermission(permission)

const loading = ref(false), saving = ref(false)
const data = ref([]), total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })
const orders = ref([]), availableBatches = ref([])
const formVisible = ref(false), formRef = ref(null)
const form = reactive({ orderId: null, shipmentDate: '', logisticsNo: '', operator: '', remark: '', items: [] })
const rules = {
  orderId: [{ required: true, message: '请选择销售订单', trigger: 'change' }],
  shipmentDate: [{ required: true, message: '请选择发货日期', trigger: 'change' }],
}

function today() { const d = new Date(); const p = n => String(n).padStart(2, '0'); return `${d.getFullYear()}-${p(d.getMonth()+1)}-${p(d.getDate())}` }
function batchOptions(skuId) { return availableBatches.value.filter(b => b.skuId === skuId) }

async function fetch() {
  loading.value = true
  try {
    const r = await listShipments({ page: query.page, size: query.size, keyword: query.keyword || undefined })
    data.value = r.data.records; total.value = r.data.total
  } finally { loading.value = false }
}
function search() { query.page = 1; fetch() }
function resetQuery() { query.keyword = ''; query.page = 1; fetch() }

async function openCreate() {
  const [orderRes, batchRes] = await Promise.all([
    listSalesOrders({ page: 1, size: 1000 }),
    listFinishedBatches({ page: 1, size: 1000, status: 'AVAILABLE' }),
  ])
  orders.value = (orderRes.data.records || []).filter(o => ['PENDING_SHIPMENT', 'PARTIAL_SHIPPED'].includes(o.status))
  availableBatches.value = batchRes.data.records || []
  form.orderId = null; form.shipmentDate = today(); form.logisticsNo = ''; form.operator = ''; form.remark = ''; form.items = []
  formVisible.value = true
}
function resetForm() { formRef.value?.clearValidate() }
async function onOrderChange(orderId) {
  form.items = []
  if (!orderId) return
  const r = await getSalesOrder(orderId)
  form.items = (r.data.items || [])
    .filter(item => Number(item.remainingQuantity) > 0)
    .map(item => ({
      skuId: item.skuId,
      skuCode: item.skuCode,
      productName: item.productName,
      remaining: item.remainingQuantity,
      finishedBatchId: null,
      quantity: 0,
    }))
}
async function save() {
  if (!await formRef.value.validate().catch(() => false)) return
  const items = form.items.filter(item => item.finishedBatchId && Number(item.quantity) > 0)
  if (!items.length) { ElMessage.warning('请至少选择一条发货明细'); return }
  saving.value = true
  try {
    await createShipment(form.orderId, {
      shipmentDate: form.shipmentDate,
      logisticsNo: form.logisticsNo,
      operator: form.operator,
      remark: form.remark,
      items: items.map(item => ({ finishedBatchId: item.finishedBatchId, quantity: item.quantity })),
    })
    ElMessage.success('发货成功，库存已扣减'); formVisible.value = false; fetch()
  } finally { saving.value = false }
}
onMounted(fetch)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--spacing-lg); }
.page-header h2 { margin: 0; }
.table-footer { display: flex; justify-content: flex-end; margin-top: var(--spacing-md); }
.expand-box { padding: 8px 24px; background: #fafbfc; }
.muted { margin-top: 8px; font-size: 12px; color: var(--color-text-secondary); }
</style>