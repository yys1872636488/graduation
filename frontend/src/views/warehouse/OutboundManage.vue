<template>
  <div class="page-container">
    <h2>出库管理</h2>
    <p class="hint">生产领料在“生产管理 → 领料管理”中操作，销售出库在“销售管理 → 发货管理”中操作；本页展示所有出库流水。</p>

    <div class="search-card" style="margin-top:16px">
      <el-form :model="query" inline>
        <el-form-item label="业务类型">
          <el-select v-model="query.businessType" placeholder="全部" clearable style="width:150px">
            <el-option label="生产领料" value="MATERIAL_ISSUE"/>
            <el-option label="销售出库" value="SALES_OUTBOUND"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input v-model="query.keyword" placeholder="流水号 / 业务单号" clearable style="width:200px" @keyup.enter="search"/>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
      </el-form>
    </div>

    <div class="table-card" style="margin-top:16px">
      <el-table :data="data" v-loading="loading" stripe>
        <el-table-column prop="transactionNo" label="流水号" width="180"/>
        <el-table-column label="业务类型" width="110">
          <template #default="{row}">{{ businessMeta(row.businessType).label }}</template>
        </el-table-column>
        <el-table-column prop="warehouseName" label="仓库" width="130"/>
        <el-table-column prop="batchNo" label="批次号" width="180"/>
        <el-table-column label="出库数量" width="100" align="right">
          <template #default="{row}">{{ row.quantity }}</template>
        </el-table-column>
        <el-table-column prop="beforeQuantity" label="出库前" width="90" align="right"/>
        <el-table-column prop="afterQuantity" label="出库后" width="90" align="right"/>
        <el-table-column prop="businessNo" label="业务单号" width="180"/>
        <el-table-column prop="createdAt" label="操作时间" width="165"/>
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
import { listInventoryTransactions } from '@/api/inventory'
import { INVENTORY_BUSINESS_TYPES } from '@/utils/constants'

const loading = ref(false)
const data = ref([]), total = ref(0)
const query = reactive({ page: 1, size: 10, businessType: '', keyword: '' })

function businessMeta(type) { return INVENTORY_BUSINESS_TYPES[type] || { label: type || '—' } }

async function fetch() {
  loading.value = true
  try {
    const r = await listInventoryTransactions({
      page: query.page, size: query.size,
      businessType: query.businessType || undefined,
      keyword: query.keyword || undefined,
    })
    data.value = r.data.records; total.value = r.data.total
  } finally { loading.value = false }
}
function search() { query.page = 1; fetch() }
function resetQuery() { query.businessType = ''; query.keyword = ''; query.page = 1; fetch() }
onMounted(fetch)
</script>

<style scoped>
.hint { margin: 0; font-size: 12px; color: var(--color-text-secondary); }
.table-footer { display: flex; justify-content: flex-end; margin-top: var(--spacing-md); }
</style>