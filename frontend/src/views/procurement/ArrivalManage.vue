<template>
  <div class="page-container">
    <div class="page-header">
      <h2>到货登记</h2>
      <el-button type="primary" v-if="has('arrival:register')" @click="openRegister">登记到货</el-button>
    </div>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item>
          <el-input v-model="query.keyword" placeholder="到货单号 / 供应商批次号" clearable
                    style="width:240px" @keyup.enter="search"/>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card" style="margin-top:16px">
      <el-table :data="data" v-loading="loading" stripe>
        <el-table-column prop="arrivalNo" label="到货单号" width="170"/>
        <el-table-column prop="orderNo" label="采购订单" width="170"/>
        <el-table-column prop="materialName" label="原材料" min-width="130" show-overflow-tooltip/>
        <el-table-column prop="supplierName" label="供应商" min-width="140" show-overflow-tooltip/>
        <el-table-column label="到货数量" width="110" align="right">
          <template #default="{ row }">{{ row.quantity }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column prop="arrivalDate" label="到货日期" width="110"/>
        <el-table-column prop="supplierBatchNo" label="供应商批次号" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.supplierBatchNo || '—' }}</template>
        </el-table-column>
        <el-table-column prop="batchNo" label="生成批次号" width="170"/>
        <el-table-column prop="createdAt" label="登记时间" width="165"/>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
                       :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="fetch"/>
      </div>
    </div>

    <el-dialog v-model="formVisible" title="登记到货" width="580px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="采购订单" prop="orderId">
          <el-select v-model="form.orderId" filterable placeholder="选择已提交的采购订单"
                     style="width:100%" @change="onOrderChange">
            <el-option v-for="order in orders" :key="order.id"
                       :label="`${order.orderNo} · ${order.supplierName || ''}`" :value="order.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="采购明细" prop="orderItemId">
          <el-select v-model="form.orderItemId" placeholder="选择原材料明细" style="width:100%"
                     :disabled="!form.orderId" @change="onItemChange">
            <el-option v-for="item in availableItems" :key="item.id"
                       :label="`${item.materialCode || ''} ${item.materialName || ''}（剩余 ${item.remainingQuantity} ${item.unit}）`"
                       :value="item.id"/>
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedItem" label="明细信息">
          <div class="item-info">
            <span>采购 {{ selectedItem.quantity }} {{ selectedItem.unit }}</span>
            <span>已到货 {{ selectedItem.receivedQuantity }} {{ selectedItem.unit }}</span>
            <span>剩余 {{ selectedItem.remainingQuantity }} {{ selectedItem.unit }}</span>
          </div>
        </el-form-item>
        <el-form-item label="到货数量" prop="quantity">
          <el-input-number v-model="form.quantity" :min="0.01" :max="maxQuantity"
                           :precision="2" :controls="false" style="width:180px"/>
          <span v-if="selectedItem" class="unit-text">{{ selectedItem.unit }}</span>
        </el-form-item>
        <el-form-item label="到货日期" prop="arrivalDate">
          <el-date-picker v-model="form.arrivalDate" type="date" value-format="YYYY-MM-DD"
                          placeholder="请选择" style="width:180px"/>
        </el-form-item>
        <el-form-item label="供应商批次号">
          <el-input v-model="form.supplierBatchNo" placeholder="可选，填写供应商提供的批次号"
                    maxlength="100"/>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">确认登记</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="到货详情" size="500px">
      <div v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="到货单号">{{ detail.arrivalNo }}</el-descriptions-item>
          <el-descriptions-item label="采购订单">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="原材料">
            {{ detail.materialCode }} {{ detail.materialName }}
          </el-descriptions-item>
          <el-descriptions-item label="供应商">{{ detail.supplierName }}</el-descriptions-item>
          <el-descriptions-item label="到货数量">{{ detail.quantity }} {{ detail.unit }}</el-descriptions-item>
          <el-descriptions-item label="到货日期">{{ detail.arrivalDate }}</el-descriptions-item>
          <el-descriptions-item label="供应商批次号">{{ detail.supplierBatchNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="生成批次号">{{ detail.batchNo }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ detail.remark || '—' }}</el-descriptions-item>
          <el-descriptions-item label="登记时间">{{ detail.createdAt }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { listArrivals, registerArrival } from '@/api/arrival'
import { listPurchaseOrders, getPurchaseOrder } from '@/api/purchase'

const userStore = useUserStore()
const has = permission => userStore.hasPermission(permission)

const loading = ref(false)
const saving = ref(false)
const data = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })

const orders = ref([])
const orderDetail = ref(null)
const formVisible = ref(false)
const formRef = ref(null)
const detailVisible = ref(false)
const detail = ref(null)

const form = reactive({
  orderId: null,
  orderItemId: null,
  quantity: null,
  arrivalDate: '',
  supplierBatchNo: '',
  remark: '',
})

const availableItems = computed(() => {
  if (!orderDetail.value?.items) return []
  return orderDetail.value.items.filter(item => Number(item.remainingQuantity) > 0)
})

const selectedItem = computed(() =>
  availableItems.value.find(item => item.id === form.orderItemId) || null)

const maxQuantity = computed(() =>
  selectedItem.value ? Number(selectedItem.value.remainingQuantity) : undefined)

const rules = {
  orderId: [{ required: true, message: '请选择采购订单', trigger: 'change' }],
  orderItemId: [{ required: true, message: '请选择采购明细', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入到货数量', trigger: 'blur' }],
  arrivalDate: [{ required: true, message: '请选择到货日期', trigger: 'change' }],
}

function today() {
  const d = new Date()
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

async function fetch() {
  loading.value = true
  try {
    const r = await listArrivals({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
    })
    data.value = r.data.records
    total.value = r.data.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  fetch()
}

function resetQuery() {
  query.keyword = ''
  query.page = 1
  fetch()
}

async function openRegister() {
  const r = await listPurchaseOrders({ page: 1, size: 1000 })
  orders.value = (r.data.records || []).filter(order =>
    ['SUBMITTED', 'PARTIAL_RECEIVED'].includes(order.status))
  form.arrivalDate = today()
  formVisible.value = true
}

function resetForm() {
  form.orderId = null
  form.orderItemId = null
  form.quantity = null
  form.arrivalDate = ''
  form.supplierBatchNo = ''
  form.remark = ''
  orderDetail.value = null
  formRef.value?.clearValidate()
}

async function onOrderChange(orderId) {
  form.orderItemId = null
  form.quantity = null
  orderDetail.value = null
  if (!orderId) return
  const r = await getPurchaseOrder(orderId)
  orderDetail.value = r.data
}

function onItemChange(itemId) {
  const item = availableItems.value.find(value => value.id === itemId)
  form.quantity = item ? Number(item.remainingQuantity) : null
}

async function save() {
  if (!await formRef.value.validate().catch(() => false)) return
  if (!form.quantity || Number(form.quantity) <= 0) {
    ElMessage.warning('到货数量必须大于0')
    return
  }
  if (selectedItem.value && Number(form.quantity) > Number(selectedItem.value.remainingQuantity)) {
    ElMessage.warning('到货数量不能超过剩余未到货数量')
    return
  }
  saving.value = true
  try {
    const r = await registerArrival(form.orderId, {
      orderItemId: form.orderItemId,
      quantity: form.quantity,
      arrivalDate: form.arrivalDate,
      supplierBatchNo: form.supplierBatchNo,
      remark: form.remark,
    })
    ElMessage.success(`到货登记成功，已生成批次 ${r.data.batchNo}`)
    formVisible.value = false
    fetch()
  } finally {
    saving.value = false
  }
}

function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}

onMounted(fetch)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--spacing-lg); }
.page-header h2 { margin: 0; }
.item-info { display: flex; gap: 18px; font-size: 13px; color: var(--color-text-secondary); }
.unit-text { margin-left: 8px; font-size: 13px; color: var(--color-text-secondary); }
.table-footer { display: flex; justify-content: flex-end; margin-top: var(--spacing-md); }
</style>