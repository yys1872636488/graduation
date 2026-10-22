<template>
  <div class="page-container">
    <h2>质检复核</h2>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item>
          <el-input v-model="query.keyword" placeholder="批次号 / 供应商批次号" clearable
                    style="width:230px" @keyup.enter="search"/>
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.inspectionStatus" placeholder="检验状态" style="width:140px">
            <el-option label="待复核" value="PENDING_REVIEW"/>
            <el-option label="暂停使用" value="ON_HOLD"/>
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
        <el-table-column label="到货数量" width="110" align="right">
          <template #default="{ row }">{{ row.quantity }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column prop="arrivalDate" label="到货日期" width="110"/>
        <el-table-column label="检验状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.inspectionStatus).type" size="small">
              {{ statusMeta(row.inspectionStatus).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="165"/>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openReview(row)">复核</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
                       :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="fetch"/>
      </div>
    </div>

    <el-drawer v-model="drawerVisible" title="质检复核" size="720px" @closed="resetForm">
      <div v-if="detail" class="drawer-body">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="批次号">{{ detail.batch.batchNo }}</el-descriptions-item>
          <el-descriptions-item label="原材料">
            {{ detail.batch.materialCode }} {{ detail.batch.materialName }}
          </el-descriptions-item>
          <el-descriptions-item label="供应商">{{ detail.batch.supplierName }}</el-descriptions-item>
          <el-descriptions-item label="到货数量">
            {{ detail.batch.quantity }} {{ detail.batch.unit }}
          </el-descriptions-item>
          <el-descriptions-item label="来源订单">{{ detail.batch.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="检验状态">
            <el-tag :type="statusMeta(detail.batch.inspectionStatus).type" size="small">
              {{ statusMeta(detail.batch.inspectionStatus).label }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <h4 class="section-title">检测图片</h4>
        <div v-if="detail.images.length" class="image-list">
          <div v-for="image in detail.images" :key="image.id" class="image-item">
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
                    description="本次没有真实检测结果，请基于图片进行人工复核。"/>
          <el-alert v-else-if="latestDetection.status === 'SERVICE_UNAVAILABLE'"
                    type="error" :closable="false" show-icon
                    title="视觉服务不可用"
                    :description="latestDetection.errorMessage || '请确认 Python 检测服务已启动'"/>
          <el-alert v-else-if="latestDetection.status === 'FAILED'"
                    type="error" :closable="false" show-icon
                    title="检测失败"
                    :description="latestDetection.errorMessage || '请稍后重试'"/>
          <template v-else>
            <p v-if="latestDetection.detectionCount === 0" class="muted">
              未检测到缺陷框。这不代表质检合格，请结合图片人工复核。
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

        <h4 class="section-title">复核结论</h4>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
          <el-form-item label="质检结论" prop="conclusion">
            <el-radio-group v-model="form.conclusion">
              <el-radio value="QUALIFIED">合格</el-radio>
              <el-radio value="UNQUALIFIED">不合格</el-radio>
              <el-radio value="ON_HOLD">暂停使用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="form.conclusion === 'UNQUALIFIED'" label="处理方式" prop="disposition">
            <el-select v-model="form.disposition" placeholder="请选择处理方式" style="width:220px">
              <el-option v-for="item in DISPOSITIONS" :key="item.value"
                         :label="item.label" :value="item.value"/>
            </el-select>
          </el-form-item>
          <el-form-item label="复核意见" prop="opinion">
            <el-input v-model="form.opinion" type="textarea" :rows="3" maxlength="1000"
                      show-word-limit placeholder="请填写复核依据和结论说明"/>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500"/>
          </el-form-item>
        </el-form>

        <h4 class="section-title">历史复核记录</h4>
        <el-table :data="detail.records" size="small" border>
          <el-table-column prop="reviewTime" label="复核时间" width="165"/>
          <el-table-column prop="reviewerName" label="复核人" width="100"/>
          <el-table-column label="结论" width="100">
            <template #default="{ row }">
              <el-tag :type="statusMeta(row.conclusion).type" size="small">
                {{ statusMeta(row.conclusion).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="处理方式" width="100">
            <template #default="{ row }">{{ dispositionLabel(row.disposition) }}</template>
          </el-table-column>
          <el-table-column prop="opinion" label="复核意见" min-width="180" show-overflow-tooltip/>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交复核</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { BATCH_INSPECTION_STATUS, DETECTION_STATUS, DISPOSITIONS } from '@/utils/constants'
import { listMaterialBatches } from '@/api/batch'
import { getInspectionDetail, reviewBatch } from '@/api/inspection'

const route = useRoute()

const loading = ref(false)
const saving = ref(false)
const data = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', inspectionStatus: 'PENDING_REVIEW' })

const drawerVisible = ref(false)
const detail = ref(null)
const currentBatchId = ref(null)
const formRef = ref(null)
const form = reactive({
  conclusion: '',
  opinion: '',
  disposition: '',
  remark: '',
})

const rules = {
  conclusion: [{ required: true, message: '请选择质检结论', trigger: 'change' }],
  opinion: [{ required: true, message: '请填写复核意见', trigger: 'blur' }],
}

const latestDetection = computed(() => detail.value?.detections?.[0] || null)

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
  query.inspectionStatus = 'PENDING_REVIEW'
  query.page = 1
  fetch()
}

function resetForm() {
  form.conclusion = ''
  form.opinion = ''
  form.disposition = ''
  form.remark = ''
  formRef.value?.clearValidate()
}

async function openReview(row) {
  currentBatchId.value = row.id
  const r = await getInspectionDetail(row.id)
  detail.value = r.data
  resetForm()
  drawerVisible.value = true
}

async function submit() {
  if (!await formRef.value.validate().catch(() => false)) return
  if (form.conclusion === 'UNQUALIFIED' && !form.disposition) {
    ElMessage.warning('不合格批次请选择处理方式')
    return
  }
  saving.value = true
  try {
    await reviewBatch(currentBatchId.value, {
      conclusion: form.conclusion,
      opinion: form.opinion,
      disposition: form.conclusion === 'UNQUALIFIED' ? form.disposition : '',
      remark: form.remark,
      imageId: latestDetection.value?.imageId || null,
      detectionId: latestDetection.value?.id || null,
    })
    ElMessage.success('复核结论已保存')
    drawerVisible.value = false
    fetch()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await fetch()
  if (route.query.batchId) {
    const batchId = Number(route.query.batchId)
    try {
      const r = await getInspectionDetail(batchId)
      currentBatchId.value = batchId
      detail.value = r.data
      resetForm()
      drawerVisible.value = true
    } catch {
      // 批次不存在时保持列表页
    }
  }
})
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