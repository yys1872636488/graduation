<template>
  <div class="page-container">
    <h2>质检记录</h2>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item>
          <el-input v-model="query.keyword" placeholder="批次号" clearable style="width:180px"
                    @keyup.enter="search"/>
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.conclusion" placeholder="质检结论" clearable style="width:130px">
            <el-option label="合格" value="QUALIFIED"/>
            <el-option label="不合格" value="UNQUALIFIED"/>
            <el-option label="暂停使用" value="ON_HOLD"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
                          start-placeholder="开始日期" end-placeholder="结束日期" style="width:250px"/>
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
        <el-table-column label="质检结论" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.conclusion).type" size="small">
              {{ statusMeta(row.conclusion).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理方式" width="100">
          <template #default="{ row }">{{ dispositionLabel(row.disposition) }}</template>
        </el-table-column>
        <el-table-column prop="reviewerName" label="复核人" width="110">
          <template #default="{ row }">{{ row.reviewerName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="reviewTime" label="复核时间" width="165"/>
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

    <el-drawer v-model="detailVisible" title="质检记录详情" size="640px">
      <div v-if="record" class="drawer-body">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="批次号">{{ record.batchNo }}</el-descriptions-item>
          <el-descriptions-item label="原材料">
            {{ record.materialCode }} {{ record.materialName }}
          </el-descriptions-item>
          <el-descriptions-item label="供应商">{{ record.supplierName }}</el-descriptions-item>
          <el-descriptions-item label="质检结论">
            <el-tag :type="statusMeta(record.conclusion).type" size="small">
              {{ statusMeta(record.conclusion).label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="处理方式">{{ dispositionLabel(record.disposition) }}</el-descriptions-item>
          <el-descriptions-item label="复核人">{{ record.reviewerName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="复核时间" :span="2">{{ record.reviewTime }}</el-descriptions-item>
          <el-descriptions-item label="复核意见" :span="2">{{ record.opinion || '—' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ record.remark || '—' }}</el-descriptions-item>
        </el-descriptions>

        <template v-if="inspection">
          <h4 class="section-title">检测图片</h4>
          <div v-if="inspection.images.length" class="image-list">
            <div v-for="image in inspection.images" :key="image.id" class="image-item">
              <el-image :src="image.url" fit="cover" :preview-src-list="[image.url]" preview-teleported/>
              <div class="image-meta" :title="image.originalName">{{ image.originalName }}</div>
            </div>
          </div>
          <p v-else class="muted">暂无检测图片</p>

          <h4 class="section-title">机器视觉结果</h4>
          <template v-if="latestDetection">
            <div class="result-head">
              <el-tag :type="detectionMeta(latestDetection.status).type" size="small">
                {{ detectionMeta(latestDetection.status).label }}
              </el-tag>
              <span class="muted">{{ latestDetection.createdAt }}</span>
              <span v-if="latestDetection.modelName" class="muted">
                模型：{{ latestDetection.modelName }} {{ latestDetection.modelVersion || '' }}
              </span>
            </div>
            <el-alert v-if="latestDetection.status === 'MODEL_NOT_CONFIGURED'"
                      type="warning" :closable="false" show-icon
                      title="检测服务未配置模型权重"
                      description="该批次没有真实检测框，复核结论由人工给出。"/>
            <el-alert v-else-if="latestDetection.status === 'SERVICE_UNAVAILABLE'"
                      type="error" :closable="false" show-icon
                      title="视觉服务不可用"
                      :description="latestDetection.errorMessage || '检测时视觉服务不可用'"/>
            <template v-else-if="latestDetection.status === 'SUCCESS'">
              <p v-if="latestDetection.detectionCount === 0" class="muted">
                未检测到缺陷框，不代表质检合格。
              </p>
              <el-table v-else :data="latestDetection.detections" size="small" border max-height="220">
                <el-table-column prop="class_name" label="缺陷类别" width="120"/>
                <el-table-column label="置信度" width="100">
                  <template #default="{ row }">{{ formatConfidence(row.confidence) }}</template>
                </el-table-column>
                <el-table-column label="位置框">
                  <template #default="{ row }">{{ (row.bbox || []).join(', ') }}</template>
                </el-table-column>
              </el-table>
            </template>
          </template>
          <p v-else class="muted">暂无检测结果</p>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { BATCH_INSPECTION_STATUS, DETECTION_STATUS, DISPOSITIONS } from '@/utils/constants'
import { listInspectionRecords, getInspectionDetail } from '@/api/inspection'

const loading = ref(false)
const data = ref([])
const total = ref(0)
const dateRange = ref([])
const query = reactive({ page: 1, size: 10, keyword: '', conclusion: '' })

const detailVisible = ref(false)
const record = ref(null)
const inspection = ref(null)

const latestDetection = computed(() => inspection.value?.detections?.[0] || null)

function statusMeta(status) {
  return BATCH_INSPECTION_STATUS[status] || { label: status || '未知', type: 'info' }
}

function detectionMeta(status) {
  return DETECTION_STATUS[status] || { label: status || '未知', type: 'info' }
}

function dispositionLabel(value) {
  return DISPOSITIONS.find(item => item.value === value)?.label || value || '—'
}

function formatConfidence(value) {
  const number = Number(value)
  return Number.isFinite(number) ? (number * 100).toFixed(1) + '%' : '—'
}

async function fetch() {
  loading.value = true
  try {
    const r = await listInspectionRecords({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      conclusion: query.conclusion || undefined,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined,
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
  query.conclusion = ''
  dateRange.value = []
  query.page = 1
  fetch()
}

async function openDetail(row) {
  record.value = row
  inspection.value = null
  detailVisible.value = true
  try {
    const r = await getInspectionDetail(row.batchId)
    inspection.value = r.data
  } catch {
    // 详情加载失败时仍展示记录基本信息
  }
}

onMounted(fetch)
</script>

<style scoped>
.table-footer { display: flex; justify-content: flex-end; margin-top: var(--spacing-md); }
.drawer-body { padding: 0 4px; }
.section-title { margin: 22px 0 10px; font-size: 13px; font-weight: 600; color: var(--color-text-secondary); }
.image-list { display: flex; flex-wrap: wrap; gap: 12px; }
.image-item { width: 150px; padding: 4px; border: 1px solid var(--color-border); border-radius: var(--radius-md); }
.image-item :deep(.el-image) { width: 140px; height: 100px; display: block; }
.image-meta { margin-top: 4px; font-size: 12px; color: var(--color-text-secondary);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.result-head { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.muted { font-size: 12px; color: var(--color-text-secondary); }
</style>