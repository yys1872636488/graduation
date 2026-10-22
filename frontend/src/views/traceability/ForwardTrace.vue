<template>
  <div class="page-container">
    <h2>正向追溯</h2>

    <div class="search-card">
      <el-form inline @submit.prevent>
        <el-form-item label="原材料批次号">
          <el-input v-model="batchNo" placeholder="如 MB-20261022-001" clearable style="width:260px" @keyup.enter="search"/>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="search">追溯</el-button>
        </el-form-item>
      </el-form>
    </div>

    <template v-if="data">
      <div class="table-card" style="margin-top:16px">
        <h3 class="section-title">批次与采购来源</h3>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="批次号">{{ data.batch?.batchNo }}</el-descriptions-item>
          <el-descriptions-item label="原材料">{{ data.batch?.materialCode }} {{ data.batch?.materialName }}</el-descriptions-item>
          <el-descriptions-item label="供应商">{{ data.batch?.supplierName }}</el-descriptions-item>
          <el-descriptions-item label="采购订单">{{ data.batch?.orderNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="到货单">{{ data.batch?.arrivalNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="到货数量">{{ data.batch?.quantity }} {{ data.batch?.unit }}</el-descriptions-item>
          <el-descriptions-item label="供应商批次号">{{ data.batch?.supplierBatchNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="到货日期">{{ data.batch?.arrivalDate || '—' }}</el-descriptions-item>
          <el-descriptions-item label="检验状态">
            <el-tag :type="batchMeta(data.batch?.inspectionStatus).type" size="small">
              {{ batchMeta(data.batch?.inspectionStatus).label }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="table-card" style="margin-top:16px">
        <h3 class="section-title">来料质检</h3>
        <p class="muted">
          检测图片 {{ data.inspection?.images?.length || 0 }} 张，
          检测记录 {{ data.inspection?.detections?.length || 0 }} 条，
          复核记录 {{ data.inspection?.records?.length || 0 }} 条。
        </p>
        <el-table :data="data.inspection?.records || []" size="small" border>
          <el-table-column prop="reviewTime" label="复核时间" width="165"/>
          <el-table-column prop="reviewerName" label="复核人" width="100"/>
          <el-table-column label="结论" width="100">
            <template #default="{row}"><el-tag :type="batchMeta(row.conclusion).type" size="small">{{ batchMeta(row.conclusion).label }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="opinion" label="复核意见" min-width="200" show-overflow-tooltip/>
        </el-table>
      </div>

      <div class="table-card" style="margin-top:16px">
        <h3 class="section-title">生产使用（领料与工单）</h3>
        <el-table :data="data.workOrders || []" size="small" border>
          <el-table-column prop="workOrderNo" label="生产工单" width="180"/>
          <el-table-column label="产品/SKU" min-width="180">
            <template #default="{row}">{{ row.productName }} / {{ row.skuCode }}</template>
          </el-table-column>
          <el-table-column prop="plannedQuantity" label="计划数量" width="100"/>
          <el-table-column prop="completedQuantity" label="完成数量" width="100"/>
          <el-table-column prop="qualifiedQuantity" label="合格数量" width="100"/>
          <el-table-column label="状态" width="100">
            <template #default="{row}"><el-tag :type="woMeta(row.status).type" size="small">{{ woMeta(row.status).label }}</el-tag></template>
          </el-table-column>
        </el-table>
      </div>

      <div class="table-card" style="margin-top:16px">
        <h3 class="section-title">成品批次与销售</h3>
        <el-table :data="data.finishedBatches || []" size="small" border>
          <el-table-column prop="batchNo" label="成品批次号" width="180"/>
          <el-table-column label="产品/SKU" min-width="180">
            <template #default="{row}">{{ row.productName }} / {{ row.skuCode }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="90"/>
          <el-table-column label="状态" width="110">
            <template #default="{row}"><el-tag :type="fbMeta(row.status).type" size="small">{{ fbMeta(row.status).label }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="productionDate" label="生产日期" width="110"/>
        </el-table>
        <el-table :data="data.shipments || []" size="small" border style="margin-top:12px">
          <el-table-column prop="shipmentNo" label="发货单号" width="180"/>
          <el-table-column prop="shipmentDate" label="发货日期" width="110"/>
          <el-table-column prop="logisticsNo" label="物流单号" min-width="140"/>
          <el-table-column prop="operator" label="经办人" width="100"/>
        </el-table>
        <el-table :data="data.salesOrders || []" size="small" border style="margin-top:12px">
          <el-table-column prop="orderNo" label="销售订单号" width="180"/>
          <el-table-column prop="customerName" label="客户" min-width="140"/>
          <el-table-column prop="orderDate" label="下单日期" width="110"/>
          <el-table-column prop="status" label="状态" width="140"/>
        </el-table>
      </div>

      <div class="table-card" style="margin-top:16px">
        <h3 class="section-title">售后记录</h3>
        <el-table :data="data.afterSales || []" size="small" border>
          <el-table-column prop="recordNo" label="售后单号" width="180"/>
          <el-table-column prop="type" label="类型" width="90"/>
          <el-table-column prop="applyDate" label="申请日期" width="110"/>
          <el-table-column label="状态" width="110">
            <template #default="{row}"><el-tag :type="asMeta(row.status).type" size="small">{{ asMeta(row.status).label }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="description" label="问题描述" min-width="200" show-overflow-tooltip/>
        </el-table>
      </div>
    </template>

    <el-empty v-if="!data && !loading" description="输入原材料批次号开始追溯"/>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMaterialTrace } from '@/api/traceability'
import { BATCH_INSPECTION_STATUS, WORK_ORDER_STATUS, FINISHED_BATCH_STATUS, AFTER_SALES_STATUS } from '@/utils/constants'

const batchNo = ref('MB-20261022-001')
const data = ref(null)
const loading = ref(false)

function batchMeta(status) { return BATCH_INSPECTION_STATUS[status] || { label: status || '—', type: 'info' } }
function woMeta(status) { return WORK_ORDER_STATUS[status] || { label: status || '—', type: 'info' } }
function fbMeta(status) { return FINISHED_BATCH_STATUS[status] || { label: status || '—', type: 'info' } }
function asMeta(status) { return AFTER_SALES_STATUS[status] || { label: status || '—', type: 'info' } }

async function search() {
  if (!batchNo.value) { ElMessage.warning('请输入原材料批次号'); return }
  loading.value = true
  try {
    const r = await getMaterialTrace(batchNo.value.trim())
    data.value = r.data
  } catch {
    data.value = null
  } finally { loading.value = false }
}
</script>

<style scoped>
.section-title { margin: 0 0 12px; font-size: 14px; font-weight: 600; }
.muted { font-size: 12px; color: var(--color-text-secondary); margin-bottom: 8px; }
</style>