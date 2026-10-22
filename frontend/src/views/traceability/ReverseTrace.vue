<template>
  <div class="page-container">
    <h2>反向追溯</h2>

    <div class="search-card">
      <el-form inline @submit.prevent>
        <el-form-item label="成品批次号">
          <el-input v-model="batchNo" placeholder="如 FB-20261022-001" clearable style="width:260px" @keyup.enter="search"/>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="search">追溯</el-button>
        </el-form-item>
      </el-form>
    </div>

    <template v-if="data">
      <div class="table-card" style="margin-top:16px">
        <h3 class="section-title">成品批次</h3>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="成品批次号">{{ data.finishedBatch?.batchNo }}</el-descriptions-item>
          <el-descriptions-item label="生产工单">{{ data.workOrder?.workOrderNo }}</el-descriptions-item>
          <el-descriptions-item label="产品/SKU">
            {{ data.workOrder?.productName }} / {{ data.workOrder?.skuCode }}
          </el-descriptions-item>
          <el-descriptions-item label="数量">{{ data.finishedBatch?.quantity }}</el-descriptions-item>
          <el-descriptions-item label="生产日期">{{ data.finishedBatch?.productionDate }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="fbMeta(data.finishedBatch?.status).type" size="small">
              {{ fbMeta(data.finishedBatch?.status).label }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="table-card" style="margin-top:16px">
        <h3 class="section-title">生产工单与成品质检</h3>
        <p class="muted">
          工单状态：{{ woMeta(data.workOrder?.status).label }}；
          计划 {{ data.workOrder?.plannedQuantity }}，已完成 {{ data.workOrder?.completedQuantity }}，
          合格 {{ data.workOrder?.qualifiedQuantity }}。
        </p>
        <el-table :data="data.workOrder?.inspections || []" size="small" border>
          <el-table-column prop="inspectionNo" label="成品质检单号" width="180"/>
          <el-table-column prop="inspectedQuantity" label="送检" width="80"/>
          <el-table-column prop="qualifiedQuantity" label="合格" width="80"/>
          <el-table-column prop="unqualifiedQuantity" label="不合格" width="90"/>
          <el-table-column prop="inspector" label="检验人" width="100"/>
          <el-table-column prop="inspectionTime" label="检验时间" width="165"/>
        </el-table>
      </div>

      <div class="table-card" style="margin-top:16px">
        <h3 class="section-title">原材料来源</h3>
        <el-table :data="data.materialBatches || []" size="small" border>
          <el-table-column label="原材料批次" width="180">
            <template #default="{row}">{{ row.batch?.batchNo }}</template>
          </el-table-column>
          <el-table-column label="原材料" min-width="150">
            <template #default="{row}">{{ row.batch?.materialCode }} {{ row.batch?.materialName }}</template>
          </el-table-column>
          <el-table-column label="供应商" min-width="140">
            <template #default="{row}">{{ row.batch?.supplierName }}</template>
          </el-table-column>
          <el-table-column label="采购订单" width="170">
            <template #default="{row}">{{ row.batch?.orderNo || '—' }}</template>
          </el-table-column>
          <el-table-column label="使用数量" width="100">
            <template #default="{row}">{{ row.usedQuantity }} {{ row.batch?.unit }}</template>
          </el-table-column>
          <el-table-column label="来料结论" width="110">
            <template #default="{row}">
              <el-tag :type="batchMeta(row.inspection?.records?.[0]?.conclusion).type" size="small">
                {{ batchMeta(row.inspection?.records?.[0]?.conclusion).label }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="table-card" style="margin-top:16px">
        <h3 class="section-title">销售发货</h3>
        <el-table :data="data.shipments || []" size="small" border>
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
          <el-table-column prop="conclusion" label="处理结论" min-width="180" show-overflow-tooltip/>
        </el-table>
      </div>
    </template>

    <el-empty v-if="!data && !loading" description="输入成品批次号开始追溯"/>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getFinishedTrace } from '@/api/traceability'
import { BATCH_INSPECTION_STATUS, WORK_ORDER_STATUS, FINISHED_BATCH_STATUS, AFTER_SALES_STATUS } from '@/utils/constants'

const batchNo = ref('FB-20261022-001')
const data = ref(null)
const loading = ref(false)

function batchMeta(status) { return BATCH_INSPECTION_STATUS[status] || { label: status || '—', type: 'info' } }
function woMeta(status) { return WORK_ORDER_STATUS[status] || { label: status || '—', type: 'info' } }
function fbMeta(status) { return FINISHED_BATCH_STATUS[status] || { label: status || '—', type: 'info' } }
function asMeta(status) { return AFTER_SALES_STATUS[status] || { label: status || '—', type: 'info' } }

async function search() {
  if (!batchNo.value) { ElMessage.warning('请输入成品批次号'); return }
  loading.value = true
  try {
    const r = await getFinishedTrace(batchNo.value.trim())
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