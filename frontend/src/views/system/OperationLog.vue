<template>
  <div class="page-container">
    <h2>操作日志</h2>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item><el-input v-model="query.username" placeholder="操作人" clearable style="width:140px"/></el-form-item>
        <el-form-item><el-input v-model="query.operation" placeholder="操作类型" clearable style="width:180px"/></el-form-item>
        <el-form-item><el-button type="primary" @click="fetchData">查询</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
      </el-form>
    </div>

    <div class="table-card" style="margin-top:16px">
      <el-table :data="tableData" v-loading="loading" stripe max-height="620">
        <el-table-column prop="username" label="操作人" width="110"/>
        <el-table-column prop="operation" label="操作" min-width="200" show-overflow-tooltip/>
        <el-table-column prop="requestMethod" label="方法" width="70"/>
        <el-table-column prop="requestUrl" label="URL" min-width="160" show-overflow-tooltip/>
        <el-table-column prop="requestIp" label="IP" width="140"/>
        <el-table-column prop="status" label="结果" width="70">
          <template #default="{row}"><el-tag :type="row.status===1?'success':'danger'" size="small">{{row.status===1?'成功':'失败'}}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="170"/>
      </el-table>
      <div class="table-footer">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="fetchData"/>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listOperationLogs } from '@/api/log'
const loading = ref(false), tableData = ref([]), total = ref(0)
const query = reactive({ page: 1, size: 10, username: '', operation: '' })
async function fetchData() { loading.value = true; try { const r = await listOperationLogs(query); tableData.value = r.data.records; total.value = r.data.total } finally { loading.value = false } }
function resetQuery() { query.username = ''; query.operation = ''; query.page = 1; fetchData() }
onMounted(fetchData)
</script>
<style scoped>.table-footer { display: flex; justify-content: flex-end; margin-top: var(--spacing-md) }</style>