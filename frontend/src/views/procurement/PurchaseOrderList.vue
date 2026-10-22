<template>
  <div class="page-container">
    <div class="page-header">
      <h2>采购订单</h2>
      <el-button type="primary" v-if="has('purchase:create')" @click="openCreate">新增采购订单</el-button>
    </div>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item>
          <el-input v-model="query.keyword" placeholder="采购订单号" clearable style="width:180px" @keyup.enter="search"/>
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.status" placeholder="订单状态" clearable style="width:130px">
            <el-option v-for="(meta, key) in PURCHASE_ORDER_STATUS" :key="key" :label="meta.label" :value="key"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card" style="margin-top:16px">
      <el-table :data="data" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="采购订单号" width="170"/>
        <el-table-column prop="supplierName" label="供应商" min-width="150" show-overflow-tooltip/>
        <el-table-column prop="orderDate" label="下单日期" width="110"/>
        <el-table-column label="预计到货" width="110">
          <template #default="{ row }">{{ row.expectedDate || '—' }}</template>
        </el-table-column>
        <el-table-column prop="itemCount" label="明细" width="70" align="center"/>
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row }">{{ formatAmount(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" size="small">{{ statusMeta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="165"/>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            <el-button v-if="row.status === 'DRAFT' && has('purchase:edit')" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'DRAFT' && has('purchase:submit')" link type="success" @click="submitOrder(row)">提交</el-button>
            <el-button v-if="['DRAFT', 'SUBMITTED'].includes(row.status) && has('purchase:edit')"
                       link type="danger" @click="cancelOrder(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
                       :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="fetch"/>
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="editId ? '编辑采购订单' : '新增采购订单'"
               width="900px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="供应商" prop="supplierId">
              <el-select v-model="form.supplierId" filterable placeholder="请选择供应商" style="width:100%">
                <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="下单日期" prop="orderDate">
              <el-date-picker v-model="form.orderDate" type="date" value-format="YYYY-MM-DD"
                              placeholder="请选择" style="width:100%"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="预计到货">
              <el-date-picker v-model="form.expectedDate" type="date" value-format="YYYY-MM-DD"
                              placeholder="可选" style="width:100%"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="订单备注"/>
        </el-form-item>
        <el-form-item label="采购明细">
          <el-table :data="form.items" size="small" border>
            <el-table-column label="原材料" min-width="210">
              <template #default="{ row }">
                <el-select v-model="row.materialId" filterable placeholder="选择原材料"
                           style="width:100%" @change="onMaterialChange(row)">
                  <el-option v-for="m in materials" :key="m.id"
                             :label="`${m.materialCode} ${m.name}`" :value="m.id"/>
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="120">
              <template #default="{ row }">
                <el-input-number v-model="row.quantity" :min="0.01" :precision="2"
                                 :controls="false" style="width:100%"/>
              </template>
            </el-table-column>
            <el-table-column label="单位" width="80">
              <template #default="{ row }"><el-input v-model="row.unit" disabled/></template>
            </el-table-column>
            <el-table-column label="单价" width="110">
              <template #default="{ row }">
                <el-input-number v-model="row.unitPrice" :min="0" :precision="2"
                                 :controls="false" style="width:100%"/>
              </template>
            </el-table-column>
            <el-table-column label="备注" width="130">
              <template #default="{ row }"><el-input v-model="row.remark"/></template>
            </el-table-column>
            <el-table-column width="55">
              <template #default="{ $index }">
                <el-button link type="danger" @click="removeItem($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button class="add-item" @click="addItem">+ 添加明细</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存草稿</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="采购订单详情" size="640px">
      <div v-if="detail" class="detail-body">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号" :span="2">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="供应商">{{ detail.supplierName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusMeta(detail.status).type" size="small">{{ statusMeta(detail.status).label }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="下单日期">{{ detail.orderDate }}</el-descriptions-item>
          <el-descriptions-item label="预计到货">{{ detail.expectedDate || '—' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ detail.submittedAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="section-title">采购明细</h4>
        <el-table :data="detail.items || []" size="small" border>
          <el-table-column prop="materialCode" label="原材料编号" width="140"/>
          <el-table-column prop="materialName" label="原材料" min-width="130"/>
          <el-table-column prop="quantity" label="采购数量" width="100"/>
          <el-table-column prop="receivedQuantity" label="已到货" width="90"/>
          <el-table-column prop="remainingQuantity" label="未到货" width="90"/>
          <el-table-column prop="unit" label="单位" width="70"/>
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
import { PURCHASE_ORDER_STATUS } from '@/utils/constants'
import { listPurchaseOrders, getPurchaseOrder, createPurchaseOrder, updatePurchaseOrder, submitPurchaseOrder, cancelPurchaseOrder } from '@/api/purchase'
import { listSuppliers } from '@/api/supplier'
import { listMaterials } from '@/api/material'

const userStore = useUserStore()
const has = permission => userStore.hasPermission(permission)

const loading = ref(false)
const saving = ref(false)
const data = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', status: '' })

const suppliers = ref([])
const materials = ref([])
const formVisible = ref(false)
const formRef = ref(null)
const editId = ref(null)
const detailVisible = ref(false)
const detail = ref(null)

const form = reactive({
  supplierId: null,
  orderDate: '',
  expectedDate: '',
  remark: '',
  items: [],
})

const rules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  orderDate: [{ required: true, message: '请选择下单日期', trigger: 'change' }],
}

function statusMeta(status) {
  return PURCHASE_ORDER_STATUS[status] || { label: status || '未知', type: 'info' }
}

function formatAmount(value) {
  if (value === null || value === undefined || value === '') return '—'
  return '¥' + Number(value).toFixed(2)
}

async function fetch() {
  loading.value = true
  try {
    const r = await listPurchaseOrders({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
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
  query.status = ''
  query.page = 1
  fetch()
}

async function fetchOptions() {
  const [supplierRes, materialRes] = await Promise.all([
    listSuppliers({ page: 1, size: 1000, status: 1 }),
    listMaterials({ page: 1, size: 1000, status: 1 }),
  ])
  suppliers.value = supplierRes.data.records || []
  materials.value = materialRes.data.records || []
}

function resetForm() {
  editId.value = null
  form.supplierId = null
  form.orderDate = ''
  form.expectedDate = ''
  form.remark = ''
  form.items = []
  formRef.value?.clearValidate()
}

function addItem() {
  form.items.push({ materialId: null, quantity: 1, unit: '', unitPrice: null, remark: '' })
}

function removeItem(index) {
  form.items.splice(index, 1)
}

function onMaterialChange(row) {
  const material = materials.value.find(item => item.id === row.materialId)
  row.unit = material?.unit || ''
}

async function openCreate() {
  resetForm()
  await fetchOptions()
  addItem()
  formVisible.value = true
}

async function openEdit(row) {
  await fetchOptions()
  const r = await getPurchaseOrder(row.id)
  const order = r.data
  editId.value = order.id
  form.supplierId = order.supplierId
  form.orderDate = order.orderDate
  form.expectedDate = order.expectedDate || ''
  form.remark = order.remark || ''
  form.items = (order.items || []).map(item => ({
    materialId: item.materialId,
    quantity: Number(item.quantity),
    unit: item.unit,
    unitPrice: item.unitPrice === null || item.unitPrice === undefined ? null : Number(item.unitPrice),
    remark: item.remark || '',
  }))
  formVisible.value = true
}

async function save() {
  if (!await formRef.value.validate().catch(() => false)) return
  if (form.items.length === 0) {
    ElMessage.warning('请至少添加一条采购明细')
    return
  }
  if (form.items.some(item => !item.materialId || !item.quantity || Number(item.quantity) <= 0)) {
    ElMessage.warning('请完善每条明细的原材料和数量')
    return
  }
  saving.value = true
  try {
    const payload = {
      supplierId: form.supplierId,
      orderDate: form.orderDate,
      expectedDate: form.expectedDate || null,
      remark: form.remark,
      items: form.items.map(item => ({
        materialId: item.materialId,
        quantity: item.quantity,
        unit: item.unit,
        unitPrice: item.unitPrice,
        remark: item.remark,
      })),
    }
    if (editId.value) {
      await updatePurchaseOrder(editId.value, payload)
      ElMessage.success('采购订单已更新')
    } else {
      await createPurchaseOrder(payload)
      ElMessage.success('采购订单已创建')
    }
    formVisible.value = false
    fetch()
  } finally {
    saving.value = false
  }
}

async function openDetail(row) {
  const r = await getPurchaseOrder(row.id)
  detail.value = r.data
  detailVisible.value = true
}

async function submitOrder(row) {
  const ok = await ElMessageBox.confirm(`确定提交采购订单「${row.orderNo}」？提交后明细不可编辑。`, '提交确认', { type: 'warning' }).catch(() => false)
  if (!ok) return
  await submitPurchaseOrder(row.id)
  ElMessage.success('采购订单已提交')
  fetch()
}

async function cancelOrder(row) {
  const ok = await ElMessageBox.confirm(`确定取消采购订单「${row.orderNo}」？`, '取消确认', { type: 'warning' }).catch(() => false)
  if (!ok) return
  await cancelPurchaseOrder(row.id)
  ElMessage.success('采购订单已取消')
  fetch()
}

onMounted(fetch)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--spacing-lg); }
.page-header h2 { margin: 0; }
.add-item { margin-top: 10px; }
.section-title { margin: 24px 0 10px; font-size: 13px; font-weight: 600; color: var(--color-text-secondary); }
.table-footer { display: flex; justify-content: flex-end; margin-top: var(--spacing-md); }
</style>