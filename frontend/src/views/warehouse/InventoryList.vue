<template>
  <div class="page-container">
    <h2>库存查询</h2>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="仓库">
          <el-select v-model="query.warehouseId" placeholder="全部" clearable filterable style="width:160px">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.itemType" placeholder="全部" clearable style="width:120px">
            <el-option label="原材料" value="MATERIAL"/>
            <el-option label="成品" value="FINISHED"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input v-model="query.keyword" placeholder="批次号" clearable style="width:180px" @keyup.enter="search"/>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
      </el-form>
    </div>

    <div class="table-card" style="margin-top:16px">
      <el-table :data="data" v-loading="loading" stripe>
        <el-table-column prop="warehouseName" label="仓库" width="130"/>
        <el-table-column label="类型" width="90">
          <template #default="{row}">{{ itemTypeLabel(row.itemType) }}</template>
        </el-table-column>
        <el-table-column label="物料/成品" min-width="180">
          <template #default="{row}">
            <template v-if="row.itemType === 'MATERIAL'">{{ row.materialCode }} {{ row.materialName }}</template>
            <template v-else>{{ row.productName }} / {{ row.skuCode }}</template>
          </template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批次号" width="180"/>
        <el-table-column label="库存数量" width="110" align="right">
          <template #default="{row}">{{ row.quantity }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column label="可用数量" width="110" align="right">
          <template #default="{row}">{{ row.availableQuantity }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="165"/>
      </el-table>
      <div class="table-footer">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
                       :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="fetch"/>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listInventory } from '@/api/inventory'
import { listWarehouses } from '@/api/warehouse'

const loading = ref(false)
const data = ref([]), total = ref(0), warehouses = ref([])
const query = reactive({ page: 1, size: 10, warehouseId: null, itemType: '', keyword: '' })

function itemTypeLabel(type) { return type === 'FINISHED' ? '成品' : '原材料' }

async function fetch() {
  loading.value = true
  try {
    const r = await listInventory({
      page: query.page, size: query.size,
      warehouseId: query.warehouseId || undefined,
      itemType: query.itemType || undefined,
      keyword: query.keyword || undefined,
    })
    data.value = r.data.records; total.value = r.data.total
  } finally { loading.value = false }
}
function search() { query.page = 1; fetch() }
function resetQuery() { query.warehouseId = null; query.itemType = ''; query.keyword = ''; query.page = 1; fetch() }

async function loadWarehouses() {
  const r = await listWarehouses({ page: 1, size: 1000, status: 1 })
  warehouses.value = r.data.records || []
}

onMounted(() => { loadWarehouses(); fetch() })
</script>

<style scoped>
.table-footer { display: flex; justify-content: flex-end; margin-top: var(--spacing-md); }
</style>