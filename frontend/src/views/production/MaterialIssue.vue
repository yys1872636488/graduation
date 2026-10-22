<template>
  <div class="page-container">
    <div class="page-header">
      <h2>领料管理</h2>
      <el-button type="primary" v-if="has('production:issue-material')" @click="openCreate">登记领料</el-button>
    </div>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item><el-input v-model="query.keyword" placeholder="领料单号" clearable style="width:200px" @keyup.enter="search"/></el-form-item>
        <el-form-item v-if="query.workOrderId">
          <el-tag closable @close="clearWorkOrderFilter">仅看当前工单</el-tag>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
      </el-form>
    </div>

    <div class="table-card" style="margin-top:16px">
      <el-table :data="data" v-loading="loading" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-box">
              <el-table :data="row.items || []" size="small">
                <el-table-column prop="batchNo" label="原材料批次" width="180"/>
                <el-table-column label="原材料" min-width="150">
                  <template #default="{ row: item }">{{ item.materialCode }} {{ item.materialName }}</template>
                </el-table-column>
                <el-table-column prop="quantity" label="领料数量" width="110"/>
                <el-table-column prop="unit" label="单位" width="80"/>
                <el-table-column prop="remark" label="备注" min-width="120"/>
              </el-table>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="issueNo" label="领料单号" width="180"/>
        <el-table-column prop="workOrderNo" label="生产工单" width="180"/>
        <el-table-column prop="issueDate" label="领料日期" width="110"/>
        <el-table-column prop="issuer" label="领料人" width="110"/>
        <el-table-column label="明细数" width="80" align="center"><template #default="{row}">{{ row.items?.length || 0 }}</template></el-table-column>
        <el-table-column prop="createdAt" label="登记时间" width="165"/>
      </el-table>
      <div class="table-footer">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
                       :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="fetch"/>
      </div>
    </div>

    <el-dialog v-model="formVisible" title="登记领料" width="760px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="生产工单" prop="workOrderId">
              <el-select v-model="form.workOrderId" filterable placeholder="选择待执行/生产中工单" style="width:100%">
                <el-option v-for="order in orders" :key="order.id" :label="`${order.workOrderNo} · ${order.productName} · ${order.skuCode}`" :value="order.id"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="领料日期" prop="issueDate">
              <el-date-picker v-model="form.issueDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="领料人"><el-input v-model="form.issuer" style="width:200px"/></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2"/></el-form-item>
        <el-form-item label="领料明细">
          <el-table :data="form.items" size="small" border>
            <el-table-column label="原材料批次" min-width="260">
              <template #default="{ row }">
                <el-select v-model="row.materialBatchId" filterable placeholder="选择已入库批次" style="width:100%" @change="onBatchChange(row)">
                  <el-option v-for="batch in batches" :key="batch.id"
                             :label="`${batch.batchNo} · ${batch.materialName} · ${batch.quantity}${batch.unit}`" :value="batch.id"/>
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="130">
              <template #default="{ row }"><el-input-number v-model="row.quantity" :min="0.01" :precision="2" :controls="false" style="width:100%"/></template>
            </el-table-column>
            <el-table-column label="单位" width="80"><template #default="{ row }"><el-input v-model="row.unit" disabled/></template></el-table-column>
            <el-table-column label="备注" width="150"><template #default="{ row }"><el-input v-model="row.remark"/></template></el-table-column>
            <el-table-column width="60"><template #default="{ $index }"><el-button link type="danger" @click="removeItem($index)">删除</el-button></template></el-table-column>
          </el-table>
          <el-button class="add-item" @click="addItem">+ 添加明细</el-button>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="formVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">确认领料</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { listMaterialIssues, createMaterialIssue, listWorkOrders } from '@/api/production'
import { listMaterialBatches } from '@/api/batch'

const route = useRoute()
const userStore = useUserStore()
const has = permission => userStore.hasPermission(permission)

const loading = ref(false), saving = ref(false)
const data = ref([]), total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', workOrderId: null })
const orders = ref([]), batches = ref([])
const formVisible = ref(false), formRef = ref(null)
const form = reactive({ workOrderId: null, issueDate: '', issuer: '', remark: '', items: [] })
const rules = {
  workOrderId: [{ required: true, message: '请选择生产工单', trigger: 'change' }],
  issueDate: [{ required: true, message: '请选择领料日期', trigger: 'change' }],
}

function today() {
  const d = new Date(); const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

async function fetch() {
  loading.value = true
  try {
    const r = await listMaterialIssues({
      page: query.page, size: query.size,
      keyword: query.keyword || undefined,
      workOrderId: query.workOrderId || undefined,
    })
    data.value = r.data.records; total.value = r.data.total
  } finally { loading.value = false }
}
function search() { query.page = 1; fetch() }
function resetQuery() { query.keyword = ''; query.workOrderId = null; query.page = 1; fetch() }
function clearWorkOrderFilter() { query.workOrderId = null; query.page = 1; fetch() }

async function openCreate(presetWorkOrderId = null) {
  const [orderRes, batchRes] = await Promise.all([
    listWorkOrders({ page: 1, size: 1000 }),
    listMaterialBatches({ page: 1, size: 1000, inspectionStatus: 'STOCKED' }),
  ])
  orders.value = (orderRes.data.records || []).filter(o =>
    ['PENDING', 'IN_PROGRESS', 'REWORKING'].includes(o.status))
  batches.value = batchRes.data.records || []
  form.workOrderId = presetWorkOrderId
  form.issueDate = today()
  addItem()
  formVisible.value = true
}
function resetForm() {
  form.workOrderId = null; form.issueDate = ''; form.issuer = ''; form.remark = ''; form.items = []
  formRef.value?.clearValidate()
}
function addItem() { form.items.push({ materialBatchId: null, quantity: 1, unit: '', remark: '' }) }
function removeItem(index) { form.items.splice(index, 1) }
function onBatchChange(row) {
  const batch = batches.value.find(item => item.id === row.materialBatchId)
  row.unit = batch?.unit || ''
}
async function save() {
  if (!await formRef.value.validate().catch(() => false)) return
  if (!form.items.length) { ElMessage.warning('请至少添加一条领料明细'); return }
  if (form.items.some(item => !item.materialBatchId || !item.quantity || Number(item.quantity) <= 0)) {
    ElMessage.warning('请完善每条领料明细'); return
  }
  saving.value = true
  try {
    await createMaterialIssue(form.workOrderId, {
      issueDate: form.issueDate,
      issuer: form.issuer,
      remark: form.remark,
      items: form.items.map(item => ({ materialBatchId: item.materialBatchId, quantity: item.quantity, remark: item.remark })),
    })
    ElMessage.success('领料登记成功'); formVisible.value = false; fetch()
  } finally { saving.value = false }
}
onMounted(async () => {
  if (route.query.workOrderId) {
    query.workOrderId = Number(route.query.workOrderId)
  }
  await fetch()
  if (query.workOrderId) {
    await openCreate(query.workOrderId)
  }
})
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--spacing-lg); }
.page-header h2 { margin: 0; }
.table-footer { display: flex; justify-content: flex-end; margin-top: var(--spacing-md); }
.expand-box { padding: 8px 24px; background: #fafbfc; }
.add-item { margin-top: 10px; }
</style>