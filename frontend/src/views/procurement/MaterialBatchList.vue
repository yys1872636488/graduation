<template>
  <div class="page-container">
    <h2>原材料批次</h2>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item>
          <el-input v-model="query.keyword" placeholder="批次号 / 供应商批次号" clearable
                    style="width:230px" @keyup.enter="search"/>
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.inspectionStatus" placeholder="检验状态" clearable style="width:140px">
            <el-option v-for="(meta, key) in BATCH_INSPECTION_STATUS" :key="key"
                       :label="meta.label" :value="key"/>
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
        <el-table-column prop="batchNo" label="批次号" width="170"/>
        <el-table-column label="原材料" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.materialCode }} {{ row.materialName }}</template>
        </el-table-column>
        <el-table-column prop="supplierName" label="供应商" min-width="140" show-overflow-tooltip/>
        <el-table-column prop="orderNo" label="来源订单" width="170"/>
        <el-table-column label="到货数量" width="110" align="right">
          <template #default="{ row }">{{ row.quantity }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column prop="arrivalDate" label="到货日期" width="110"/>
        <el-table-column prop="supplierBatchNo" label="供应商批次号" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.supplierBatchNo || '—' }}</template>
        </el-table-column>
        <el-table-column label="检验状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.inspectionStatus).type" size="small">
              {{ statusMeta(row.inspectionStatus).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="165"/>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
                       :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="fetch"/>
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="原材料批次详情" size="520px">
      <div v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="批次号">{{ detail.batchNo }}</el-descriptions-item>
          <el-descriptions-item label="原材料">
            {{ detail.materialCode }} {{ detail.materialName }}
          </el-descriptions-item>
          <el-descriptions-item label="规格">{{ detail.specification || '—' }}</el-descriptions-item>
          <el-descriptions-item label="供应商">{{ detail.supplierName }}</el-descriptions-item>
          <el-descriptions-item label="来源采购订单">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="来源到货单">{{ detail.arrivalNo }}</el-descriptions-item>
          <el-descriptions-item label="供应商批次号">{{ detail.supplierBatchNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="到货数量">{{ detail.quantity }} {{ detail.unit }}</el-descriptions-item>
          <el-descriptions-item label="可用数量">{{ detail.availableQuantity }} {{ detail.unit }}</el-descriptions-item>
          <el-descriptions-item label="到货日期">{{ detail.arrivalDate }}</el-descriptions-item>
          <el-descriptions-item label="检验状态">
            <el-tag :type="statusMeta(detail.inspectionStatus).type" size="small">
              {{ statusMeta(detail.inspectionStatus).label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.createdAt }}</el-descriptions-item>
        </el-descriptions>
        <p v-if="detail.inspectionStatus === 'PENDING_INSPECTION'" class="hint">
          该批次等待来料质检，检测与复核功能将在下一模块接入。
        </p>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { BATCH_INSPECTION_STATUS } from '@/utils/constants'
import { listMaterialBatches, getMaterialBatch } from '@/api/batch'

const loading = ref(false)
const data = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', inspectionStatus: '' })

const detailVisible = ref(false)
const detail = ref(null)

function statusMeta(status) {
  return BATCH_INSPECTION_STATUS[status] || { label: status || '未知', type: 'info' }
}

async function fetch() {
  loading.value = true
  try {
    const r = await listMaterialBatches({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      inspectionStatus: query.inspectionStatus || undefined,
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
  query.inspectionStatus = ''
  query.page = 1
  fetch()
}

async function openDetail(row) {
  const r = await getMaterialBatch(row.id)
  detail.value = r.data
  detailVisible.value = true
}

onMounted(fetch)
</script>

<style scoped>
.table-footer { display: flex; justify-content: flex-end; margin-top: var(--spacing-md); }
.hint { margin-top: 16px; padding: 10px 12px; background: #f7f8fa; border-radius: var(--radius-md);
  font-size: 12px; color: var(--color-text-secondary); }
</style>