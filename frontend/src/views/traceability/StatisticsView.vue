<template>
  <div class="page-container">
    <h2>统计分析</h2>

    <div class="stat-grid">
      <div class="stat-card" v-for="item in cards" :key="item.label">
        <span class="stat-value">{{ item.value }}</span>
        <span class="stat-label">{{ item.label }}</span>
      </div>
    </div>

    <div class="table-card" style="margin-top:20px">
      <h3 class="section-title">来料质量</h3>
      <div class="quality-row">
        <div class="quality-item">
          <span class="quality-value">{{ quality?.passRate ?? 0 }}%</span>
          <span class="stat-label">复核合格率</span>
        </div>
        <div class="quality-item">
          <span class="quality-value">{{ quality?.inspectionTotal ?? 0 }}</span>
          <span class="stat-label">复核记录总数</span>
        </div>
        <div class="quality-item">
          <span class="quality-value">{{ quality?.qualifiedCount ?? 0 }}</span>
          <span class="stat-label">合格</span>
        </div>
        <div class="quality-item">
          <span class="quality-value">{{ quality?.unqualifiedCount ?? 0 }}</span>
          <span class="stat-label">不合格</span>
        </div>
      </div>

      <el-table :data="batchStatusRows" size="small" border style="margin-top:16px">
        <el-table-column prop="label" label="批次检验状态" width="160"/>
        <el-table-column prop="count" label="批次数" width="120"/>
      </el-table>

      <p class="hint">
        合格率口径：人工复核结论为“合格”的记录数 ÷（合格 + 不合格）记录数；待复核和暂停使用不计入。
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getOverview, getQuality } from '@/api/statistics'
import { BATCH_INSPECTION_STATUS } from '@/utils/constants'

const overview = ref({})
const quality = ref({})

const cards = computed(() => [
  { label: '采购订单', value: overview.value.purchaseOrderCount ?? 0 },
  { label: '待检批次', value: overview.value.pendingInspectionCount ?? 0 },
  { label: '生产中/待质检工单', value: overview.value.workOrderInProgress ?? 0 },
  { label: '待入库成品批次', value: overview.value.finishedBatchPendingInbound ?? 0 },
  { label: '原材料可用库存', value: Number(overview.value.materialStock ?? 0).toFixed(2) },
  { label: '成品可用库存', value: Number(overview.value.finishedStock ?? 0).toFixed(2) },
  { label: '销售订单', value: overview.value.salesOrderCount ?? 0 },
  { label: '待处理售后', value: overview.value.afterSalesPending ?? 0 },
])

const batchStatusRows = computed(() => {
  const counts = quality.value.batchStatusCount || {}
  return Object.entries(counts).map(([status, count]) => ({
    status,
    label: BATCH_INSPECTION_STATUS[status]?.label || status,
    count,
  }))
})

onMounted(async () => {
  const [o, q] = await Promise.all([getOverview(), getQuality()])
  overview.value = o.data || {}
  quality.value = q.data || {}
})
</script>

<style scoped>
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1px; background: var(--color-border);
  border: 1px solid var(--color-border); border-radius: var(--radius-lg); overflow: hidden; }
.stat-card { background: var(--color-surface); padding: 24px; display: flex; flex-direction: column; gap: 6px; }
.stat-value { font-size: 26px; font-weight: 600; color: var(--color-text); }
.stat-label { font-size: 12px; color: var(--color-text-secondary); }
.section-title { margin: 0 0 16px; font-size: 14px; font-weight: 600; }
.quality-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.quality-item { display: flex; flex-direction: column; gap: 6px; padding: 16px; background: #f7f8fa; border-radius: var(--radius-md); }
.quality-value { font-size: 22px; font-weight: 600; color: var(--color-primary); }
.hint { margin: 14px 0 0; font-size: 12px; color: var(--color-text-secondary); }
@media (max-width: 1000px) { .stat-grid, .quality-row { grid-template-columns: repeat(2, 1fr); } }
</style>