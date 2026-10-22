<template>
  <div class="page-container">
    <div class="page-header">
      <h2>销售订单</h2>
      <el-button type="primary" v-if="has('sales:create')" @click="openCreate">新增销售订单</el-button>
    </div>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item><el-input v-model="query.keyword" placeholder="销售订单号" clearable style="width:180px" @keyup.enter="search"/></el-form-item>
        <el-form-item>
          <el-select v-model="query.status" placeholder="状态" clearable style="width:130px">
            <el-option v-for="(meta, key) in SALES_ORDER_STATUS" :key="key" :label="meta.label" :value="key"/>
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
      </el-form>
    </div>

    <div class="table-card" style="margin-top:16px">
      <el-table :data="data" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" width="180"/>
        <el-table-column prop="customerName" label="客户" min-width="140" show-overflow-tooltip/>
        <el-table-column prop="orderDate" label="下单日期" width="110"/>
        <el-table-column prop="itemCount" label="明细" width="70" align="center"/>
        <el-table-column label="金额" width="110" align="right"><template #default="{row}">{{ money(row.totalAmount) }}</template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="statusMeta(row.status).type" size="small">{{ statusMeta(row.status).label }}</el-tag></template></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="165"/>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{row}">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            <el-button v-if="row.status==='PENDING_SHIPMENT' && has('sales:edit')" link type="danger" @click="cancelOrder(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
                       :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="fetch"/>
      </div>
    </div>

    <el-dialog v-model="formVisible" title="新增销售订单" width="780px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户名称" prop="customerName"><el-input v-model="form.customerName"/></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="下单日期" prop="orderDate">
              <el-date-picker v-model="form.orderDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2"/></el-form-item>
        <el-form-item label="订单明细">
          <el-table :data="form.items" size="small" border>
            <el-table-column label="SKU" min-width="220">
              <template #default="{row}">
                <el-select v-model="row.skuId" filterable placeholder="选择SKU" style="width:100%">
                  <el-option v-for="sku in skus" :key="sku.id"
                             :label="`${sku.skuCode} ${sku.color || ''} ${sku.sizeLabel || ''}`" :value="sku.id"/>
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="120">
              <template #default="{row}"><el-input-number v-model="row.quantity" :min="1" :precision="0" :controls="false" style="width:100%"/></template>
            </el-table-column>
            <el-table-column label="单价" width="120">
              <template #default="{row}"><el-input-number v-model="row.unitPrice" :min="0" :precision="2" :controls="false" style="width:100%"/></template>
            </el-table-column>
            <el-table-column label="备注" width="130"><template #default="{row}"><el-input v-model="row.remark"/></template></el-table-column>
            <el-table-column width="60"><template #default="{ $index }"><el-button link type="danger" @click="removeItem($index)">删除</el-button></template></el-table-column>
          </el-table>
          <el-button class="add-item" @click="addItem">+ 添加明细</el-button>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="formVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="销售订单详情" size="620px">
      <div v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号" :span="2">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ detail.customerName }}</el-descriptions-item>
          <el-descriptions-item label="下单日期">{{ detail.orderDate }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="statusMeta(detail.status).type" size="small">{{ statusMeta(detail.status).label }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="金额">{{ money(detail.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
        </el-descriptions>
        <h4 class="section-title">订单明细</h4>
        <el-table :data="detail.items || []" size="small" border>
          <el-table-column label="产品/SKU" min-width="180"><template #default="{row}">{{ row.productName }} / {{ row.skuCode }}</template></el-table-column>
          <el-table-column prop="quantity" label="数量" width="80"/>
          <el-table-column prop="shippedQuantity" label="已发货" width="90"/>
          <el-table-column prop="remainingQuantity" label="待发货" width="90"/>
          <el-table-column prop="unitPrice" label="单价" width="90"/>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { SALES_ORDER_STATUS } from '@/utils/constants'
import { listSalesOrders, getSalesOrder, createSalesOrder, cancelSalesOrder } from '@/api/sales'
import { listSkus } from '@/api/sku'

const userStore = useUserStore()
const has = permission => userStore.hasPermission(permission)

const loading = ref(false), saving = ref(false)
const data = ref([]), total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', status: '' })
const skus = ref([])
const formVisible = ref(false), formRef = ref(null)
const detailVisible = ref(false), detail = ref(null)
const form = reactive({ customerName: '', orderDate: '', remark: '', items: [] })
const rules = {
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  orderDate: [{ required: true, message: '请选择下单日期', trigger: 'change' }],
}

function statusMeta(status) { return SALES_ORDER_STATUS[status] || { label: status || '未知', type: 'info' } }
function money(value) { return value == null ? '—' : '¥' + Number(value).toFixed(2) }
function today() { const d = new Date(); const p = n => String(n).padStart(2, '0'); return `${d.getFullYear()}-${p(d.getMonth()+1)}-${p(d.getDate())}` }

async function fetch() {
  loading.value = true
  try {
    const r = await listSalesOrders({ page: query.page, size: query.size, keyword: query.keyword || undefined, status: query.status || undefined })
    data.value = r.data.records; total.value = r.data.total
  } finally { loading.value = false }
}
function search() { query.page = 1; fetch() }
function resetQuery() { query.keyword = ''; query.status = ''; query.page = 1; fetch() }

async function fetchSkus() {
  const r = await listSkus({ page: 1, size: 1000, status: 1 })
  skus.value = r.data.records || []
}
function resetForm() {
  form.customerName = ''; form.orderDate = today(); form.remark = ''; form.items = []
  formRef.value?.clearValidate()
}
function addItem() { form.items.push({ skuId: null, quantity: 1, unitPrice: null, remark: '' }) }
function removeItem(index) { form.items.splice(index, 1) }
async function openCreate() { await fetchSkus(); resetForm(); addItem(); formVisible.value = true }
async function save() {
  if (!await formRef.value.validate().catch(() => false)) return
  if (!form.items.length) { ElMessage.warning('请至少添加一条订单明细'); return }
  if (form.items.some(item => !item.skuId || !item.quantity)) { ElMessage.warning('请完善订单明细'); return }
  saving.value = true
  try {
    await createSalesOrder({ ...form, items: form.items.map(item => ({ ...item, unitPrice: item.unitPrice })) })
    ElMessage.success('销售订单已创建'); formVisible.value = false; fetch()
  } finally { saving.value = false }
}
async function openDetail(row) {
  const r = await getSalesOrder(row.id); detail.value = r.data; detailVisible.value = true
}
async function cancelOrder(row) {
  const ok = await ElMessageBox.confirm(`确定取消销售订单「${row.orderNo}」？`, '取消确认', { type: 'warning' }).catch(() => false)
  if (!ok) return
  await cancelSalesOrder(row.id); ElMessage.success('订单已取消'); fetch()
}
onMounted(fetch)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--spacing-lg); }
.page-header h2 { margin: 0; }
.table-footer { display: flex; justify-content: flex-end; margin-top: var(--spacing-md); }
.add-item { margin-top: 10px; }
.section-title { margin: 20px 0 10px; font-size: 13px; font-weight: 600; color: var(--color-text-secondary); }
</style>