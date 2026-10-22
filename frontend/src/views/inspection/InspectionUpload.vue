<template>
  <div class="page-container">
    <h2>布料检测</h2>

    <div class="search-card">
      <el-form inline>
        <el-form-item label="原材料批次">
          <el-select v-model="batchId" filterable placeholder="选择待检批次" style="width:380px"
                     @change="loadDetail">
            <el-option v-for="batch in batches" :key="batch.id"
                       :label="`${batch.batchNo} · ${batch.materialName} · ${statusMeta(batch.inspectionStatus).label}`"
                       :value="batch.id"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="loadBatches">刷新批次</el-button>
        </el-form-item>
      </el-form>
    </div>

    <template v-if="detail">
      <div class="table-card" style="margin-top:16px">
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
      </div>

      <div class="grid" style="margin-top:16px">
        <div class="table-card">
          <div class="card-title">上传图片</div>
          <el-upload
            v-model:file-list="fileList"
            drag
            :auto-upload="false"
            :limit="1"
            accept=".jpg,.jpeg,.png,.bmp"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
            :on-exceed="onExceed">
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽图片到此处，或 <em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 JPG / PNG / BMP，单张不超过 20MB</div>
            </template>
          </el-upload>
          <div class="upload-actions">
            <el-button type="primary" :loading="uploading"
                       :disabled="!selectedFile || !has('inspection:upload')"
                       @click="upload">上传图片</el-button>
            <el-button type="success" :loading="detecting"
                       :disabled="!selectedImageId || !has('inspection:detect-btn')"
                       @click="detect">开始检测</el-button>
          </div>
          <p class="muted">先上传图片，再点击“开始检测”。检测结果不会自动判定批次合格。</p>
        </div>

        <div class="table-card">
          <div class="card-title">检测结果</div>
          <template v-if="latestDetection">
            <div class="result-head">
              <el-tag :type="detectionMeta(latestDetection.status).type" size="small">
                {{ detectionMeta(latestDetection.status).label }}
              </el-tag>
              <span class="muted">{{ latestDetection.createdAt }}</span>
            </div>
            <p v-if="latestDetection.modelName" class="muted">
              模型：{{ latestDetection.modelName }} {{ latestDetection.modelVersion || '' }}
            </p>
            <p v-if="latestDetection.inferenceTimeMs != null" class="muted">
              推理耗时：{{ latestDetection.inferenceTimeMs }} ms
            </p>

            <el-alert v-if="latestDetection.status === 'MODEL_NOT_CONFIGURED'"
                      type="warning" :closable="false" show-icon
                      title="检测服务未配置模型权重"
                      description="当前未产生真实检测结果，不会自动判定批次合格，请进行人工复核。"/>
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
              <el-table v-else :data="latestDetection.detections" size="small" border max-height="260">
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
          <el-empty v-else description="暂无检测结果" :image-size="60"/>
        </div>
      </div>

      <div class="table-card" style="margin-top:16px">
        <div class="card-title">
          批次图片
          <span class="muted">点击图片可选中用于检测</span>
        </div>
        <div v-if="detail.images.length" class="image-list">
          <div v-for="image in detail.images" :key="image.id"
               class="image-item" :class="{ selected: selectedImageId === image.id }"
               @click="selectedImageId = image.id">
            <el-image :src="image.url" fit="cover" :preview-src-list="[image.url]" preview-teleported/>
            <div class="image-meta" :title="image.originalName">{{ image.originalName }}</div>
            <el-button v-if="canDeleteImage(image)" link type="danger" size="small"
                       @click.stop="removeImage(image)">删除</el-button>
          </div>
        </div>
        <el-empty v-else description="暂无图片" :image-size="60"/>
      </div>

      <div class="table-card" style="margin-top:16px">
        <div class="card-title">复核记录</div>
        <el-table :data="detail.records" size="small" border>
          <el-table-column prop="reviewTime" label="复核时间" width="165"/>
          <el-table-column prop="reviewerName" label="复核人" width="110"/>
          <el-table-column label="结论" width="100">
            <template #default="{ row }">
              <el-tag :type="conclusionMeta(row.conclusion).type" size="small">
                {{ conclusionMeta(row.conclusion).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="处理方式" width="110">
            <template #default="{ row }">{{ dispositionLabel(row.disposition) }}</template>
          </el-table-column>
          <el-table-column prop="opinion" label="复核意见" min-width="220" show-overflow-tooltip/>
        </el-table>
        <div v-if="detail.batch.inspectionStatus === 'PENDING_REVIEW'" class="review-link">
          <el-button type="primary" @click="goReview">去复核</el-button>
        </div>
      </div>
    </template>

    <el-empty v-else description="请选择原材料批次" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { BATCH_INSPECTION_STATUS, DETECTION_STATUS, DISPOSITIONS } from '@/utils/constants'
import { listMaterialBatches } from '@/api/batch'
import { uploadInspectionImage, deleteInspectionImage, detectImage, getInspectionDetail } from '@/api/inspection'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const has = permission => userStore.hasPermission(permission)

const INSPECTABLE = ['PENDING_INSPECTION', 'INSPECTING', 'PENDING_REVIEW', 'ON_HOLD']

const batches = ref([])
const batchId = ref(null)
const detail = ref(null)
const fileList = ref([])
const selectedFile = ref(null)
const selectedImageId = ref(null)
const uploading = ref(false)
const detecting = ref(false)

const latestDetection = computed(() => detail.value?.detections?.[0] || null)

function statusMeta(status) {
  return BATCH_INSPECTION_STATUS[status] || { label: status || '未知', type: 'info' }
}

function detectionMeta(status) {
  return DETECTION_STATUS[status] || { label: status || '未知', type: 'info' }
}

function conclusionMeta(conclusion) {
  return BATCH_INSPECTION_STATUS[conclusion] || { label: conclusion || '—', type: 'info' }
}

function dispositionLabel(value) {
  return DISPOSITIONS.find(item => item.value === value)?.label || value || '—'
}

function formatConfidence(value) {
  const number = Number(value)
  return Number.isFinite(number) ? (number * 100).toFixed(1) + '%' : '—'
}

async function loadBatches() {
  const r = await listMaterialBatches({ page: 1, size: 1000 })
  batches.value = (r.data.records || []).filter(batch => INSPECTABLE.includes(batch.inspectionStatus))
  if (batchId.value && !batches.value.some(batch => batch.id === batchId.value)) {
    // 保留当前选择，可能是已完成批次，仅用于查看
  }
}

async function loadDetail() {
  if (!batchId.value) {
    detail.value = null
    return
  }
  const r = await getInspectionDetail(batchId.value)
  detail.value = r.data
  selectedImageId.value = detail.value.images?.[0]?.id || null
}

function onFileChange(file) {
  selectedFile.value = file.raw || null
}

function onFileRemove() {
  selectedFile.value = null
}

function onExceed() {
  ElMessage.warning('一次只能上传一张图片，请先移除已选择的图片')
}

function canDeleteImage(image) {
  if (!has('inspection:upload')) return false
  if (!INSPECTABLE.includes(detail.value?.batch?.inspectionStatus)) return false
  return !(detail.value?.detections || []).some(item => item.imageId === image.id)
}

async function removeImage(image) {
  const ok = await ElMessageBox.confirm(
    `确定删除图片「${image.originalName}」？`, '删除确认', { type: 'warning' }
  ).catch(() => false)
  if (!ok) return
  await deleteInspectionImage(batchId.value, image.id)
  ElMessage.success('图片已删除')
  if (selectedImageId.value === image.id) {
    selectedImageId.value = null
  }
  await loadDetail()
}

async function upload() {
  if (!selectedFile.value) {
    ElMessage.warning('请选择图片')
    return
  }
  uploading.value = true
  try {
    const r = await uploadInspectionImage(batchId.value, selectedFile.value)
    ElMessage.success('图片上传成功')
    fileList.value = []
    selectedFile.value = null
    await loadDetail()
    selectedImageId.value = r.data.id
  } finally {
    uploading.value = false
  }
}

async function detect() {
  if (!selectedImageId.value) {
    ElMessage.warning('请先选择要检测的图片')
    return
  }
  detecting.value = true
  try {
    const r = await detectImage(batchId.value, selectedImageId.value)
    const result = r.data
    if (result.status === 'SUCCESS') {
      ElMessage.success(`检测完成，发现 ${result.detectionCount} 个缺陷框`)
    } else if (result.status === 'MODEL_NOT_CONFIGURED') {
      ElMessage.warning('检测服务未配置模型权重，未产生真实检测结果')
    } else if (result.status === 'SERVICE_UNAVAILABLE') {
      ElMessage.error('视觉服务不可用，请确认 Python 检测服务已启动')
    } else {
      ElMessage.error('检测失败，请稍后重试')
    }
    await loadDetail()
  } finally {
    detecting.value = false
  }
}

function goReview() {
  router.push({ path: '/inspection/review', query: { batchId: batchId.value } })
}

onMounted(async () => {
  await loadBatches()
  if (route.query.batchId) {
    batchId.value = Number(route.query.batchId)
    await loadDetail()
  }
})
</script>

<style scoped>
.card-title { font-size: 13px; font-weight: 600; color: var(--color-text-secondary); margin-bottom: 12px; }
.card-title .muted { font-weight: 400; margin-left: 8px; font-size: 12px; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.upload-actions { display: flex; gap: 8px; margin-top: 12px; }
.muted { font-size: 12px; color: var(--color-text-secondary); }
.result-head { display: flex; align-items: center; gap: 10px; }
.image-list { display: flex; flex-wrap: wrap; gap: 12px; }
.image-item {
  width: 150px; padding: 4px; border: 1px solid var(--color-border);
  border-radius: var(--radius-md); cursor: pointer; transition: border-color .15s;
}
.image-item.selected { border-color: var(--color-primary); box-shadow: 0 0 0 1px var(--color-primary); }
.image-item :deep(.el-image) { width: 140px; height: 100px; display: block; }
.image-meta { margin-top: 4px; font-size: 12px; color: var(--color-text-secondary);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.review-link { margin-top: 12px; }
@media (max-width: 1000px) { .grid { grid-template-columns: 1fr; } }
</style>