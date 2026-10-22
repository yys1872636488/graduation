<template>
  <div class="page-container">
    <h2>入库管理</h2>

    <el-tabs v-model="tab">
      <el-tab-pane label="原材料入库" name="material">
        <div class="table-card" style="margin-top:8px">
          <el-table :data="materialBatches" v-loading="loading" stripe>
            <el-table-column prop="batchNo" label="批次号" width="180"/>
            <el-table-column label="原材料" min-width="150">
              <template #default="{row}">{{ row.materialCode }} {{ row.materialName }}</template>
            </el-table-column>
            <el-table-column prop="supplierName" label="供应商" min-width="140" show-overflow-tooltip/>
            <el-table-column label="到货数量" width="110" align="right">
              <template #default="{row}">{{ row.quantity }} {{ row.unit }}</template>
            </el-table-column>
            <el-table-column prop="arrivalDate" label="到货日期" width="110"/>
            <el-table-column label="操作" width="100">
              <template #default="{row}">
                <el-button v-if="has('inventory:inbound-material')" link type="primary" @click="openInbound('MATERIAL', row)">入库</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="成品入库" name="finished">
        <div class="table-card" style="margin-top:8px">
          <el-table :data="finishedBatches" v-loading="loading" stripe>
            <el-table-column prop="batchNo" label="成品批次号" width="180"/>
            <el-table-column prop="workOrderNo" label="生产工单" width="180"/>
            <el-table-column label="产品/SKU" min-width="180">
              <template #default="{row}">{{ row.productName }} / {{ row.skuCode }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="90" align="right"/>
            <el-table-column prop="productionDate" label="生产日期" width="110"/>
            <el-table-column label="操作" width="100">
              <template #default="{row}">
                <el-button v-if="has('inventory:inbound-finished')" link type="primary" @click="openInbound('FINISHED', row)">入库</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" title="选择入库仓库" width="420px">
      <el-form label-width="80px">
        <el-form-item label="仓库">
          <el-select v-model="warehouseId" filterable placeholder="请选择仓库" style="width:100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="批次">
          <span>{{ currentRow?.batchNo }}</span>
        </el-form-item>
        <el-form-item label="数量">
          <span>{{ currentRow?.quantity }} {{ currentRow?.unit || '' }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="confirmInbound">确认入库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { listMaterialBatches } from '@/api/batch'
import { listFinishedBatches } from '@/api/production'
import { listWarehouses } from '@/api/warehouse'
import { materialInbound, finishedInbound } from '@/api/inventory'

const userStore = useUserStore()
const has = permission => userStore.hasPermission(permission)

const tab = ref('material')
const loading = ref(false), saving = ref(false)
const materialBatches = ref([]), finishedBatches = ref([]), warehouses = ref([])
const dialogVisible = ref(false), warehouseId = ref(null), currentType = ref('MATERIAL'), currentRow = ref(null)

async function fetch() {
  loading.value = true
  try {
    const [m, f] = await Promise.all([
      listMaterialBatches({ page: 1, size: 1000, inspectionStatus: 'QUALIFIED' }),
      listFinishedBatches({ page: 1, size: 1000, status: 'PENDING_INBOUND' }),
    ])
    materialBatches.value = m.data.records || []
    finishedBatches.value = f.data.records || []
  } finally { loading.value = false }
}

async function loadWarehouses() {
  const r = await listWarehouses({ page: 1, size: 1000, status: 1 })
  warehouses.value = r.data.records || []
}

function openInbound(type, row) {
  currentType.value = type
  currentRow.value = row
  warehouseId.value = null
  dialogVisible.value = true
}

async function confirmInbound() {
  if (!warehouseId.value) { ElMessage.warning('请选择仓库'); return }
  saving.value = true
  try {
    if (currentType.value === 'MATERIAL') {
      await materialInbound({ batchId: currentRow.value.id, warehouseId: warehouseId.value })
    } else {
      await finishedInbound({ finishedBatchId: currentRow.value.id, warehouseId: warehouseId.value })
    }
    ElMessage.success('入库成功'); dialogVisible.value = false; fetch()
  } finally { saving.value = false }
}

onMounted(() => { loadWarehouses(); fetch() })
</script>