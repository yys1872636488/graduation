<template>
  <div class="page-container">
    <h2>操作日志</h2>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="操作人">
          <el-input v-model="query.username" placeholder="用户名" clearable style="width:140px" @keyup.enter="search"/>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="query.operation" placeholder="全部" clearable filterable style="width:180px">
            <el-option v-for="item in operationOptions" :key="item.value" :label="item.label" :value="item.value"/>
          </el-select>
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:110px">
            <el-option label="成功" :value="1"/>
            <el-option label="失败" :value="0"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card" style="margin-top:16px">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="createdAt" label="时间" width="165"/>
        <el-table-column label="操作人" width="110">
          <template #default="{ row }">{{ row.username || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作内容" min-width="280">
          <template #default="{ row }">
            <div class="op-cell">
              <span class="op-name">{{ operationLabel(row.operation) }}</span>
              <span v-if="objectLabel(row)" class="op-object" :title="objectLabel(row)">{{ objectLabel(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
                       :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="fetchData"/>
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="操作详情" size="460px">
      <div v-if="current" class="detail-body">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="操作人">{{ current.username || '—' }}</el-descriptions-item>
          <el-descriptions-item label="操作内容">{{ operationLabel(current.operation) }}</el-descriptions-item>
          <el-descriptions-item label="操作对象">{{ objectLabel(current) || '—' }}</el-descriptions-item>
          <el-descriptions-item label="操作时间">{{ current.createdAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="执行结果">
            <el-tag :type="current.status === 1 ? 'success' : 'danger'" size="small">
              {{ current.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="current.errorMsg" label="错误信息">
            <span class="error-text">{{ current.errorMsg }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="detail-section">
          <h4>请求参数</h4>
          <pre v-if="prettySummary" class="summary-box">{{ prettySummary }}</pre>
          <p v-else class="muted">无</p>
        </div>

        <div class="detail-section technical">
          <h4>技术信息</h4>
          <div class="tech-row"><span>请求方式</span><span>{{ current.requestMethod || '—' }}</span></div>
          <div class="tech-row"><span>请求地址</span><span class="mono">{{ current.requestUrl || '—' }}</span></div>
          <div class="tech-row"><span>来源 IP</span><span class="mono">{{ displayIp(current.requestIp) }}</span></div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { listOperationLogs } from '@/api/log'

// 将后端记录的 Controller.method 转为业务动作名称
const OPERATION_LABELS = {
  'AuthController.login': '登录',
  'AuthController.logout': '退出登录',
  'AuthController.changePassword': '修改密码',
  'SysUserController.create': '新增用户',
  'SysUserController.update': '编辑用户',
  'SysUserController.disable': '禁用用户',
  'SysUserController.enable': '启用用户',
  'SysUserController.resetPassword': '重置密码',
  'SysRoleController.assignPermissions': '分配角色权限',
  'SupplierController.create': '新增供应商',
  'SupplierController.update': '编辑供应商',
  'SupplierController.toggleStatus': '启用/停用供应商',
  'MaterialController.create': '新增原材料',
  'MaterialController.update': '编辑原材料',
  'MaterialController.toggleStatus': '启用/停用原材料',
  'ProductController.create': '新增产品',
  'ProductController.update': '编辑产品',
  'ProductController.toggleStatus': '启用/停用产品',
  'ProductSkuController.create': '新增SKU',
  'ProductSkuController.update': '编辑SKU',
  'ProductSkuController.toggleStatus': '启用/停用SKU',
  'WarehouseController.create': '新增仓库',
  'WarehouseController.update': '编辑仓库',
  'WarehouseController.toggleStatus': '启用/停用仓库',
  'PurchaseOrderController.create': '新增采购订单',
  'PurchaseOrderController.update': '编辑采购订单',
  'PurchaseOrderController.submit': '提交采购订单',
  'PurchaseOrderController.cancel': '取消采购订单',
  'ArrivalController.register': '登记到货',
  'InspectionController.uploadImage': '上传布料图片',
  'InspectionController.deleteImage': '删除布料图片',
  'InspectionController.detect': '发起视觉检测',
  'InspectionController.review': '质检复核',
  'ProductionPlanController.create': '新增生产计划',
  'ProductionPlanController.update': '编辑生产计划',
  'ProductionPlanController.release': '下达生产计划',
  'ProductionPlanController.cancel': '取消生产计划',
  'WorkOrderController.create': '新增生产工单',
  'WorkOrderController.start': '开始生产',
  'WorkOrderController.createIssue': '登记领料',
  'WorkOrderController.addProgress': '登记生产进度',
  'WorkOrderController.addInspection': '登记成品质检',
  'WorkOrderController.addDisposition': '不合格品处理',
  'WorkOrderController.createBatch': '生成成品批次',
  'InventoryController.materialInbound': '原材料入库',
  'InventoryController.finishedInbound': '成品入库',
  'SalesController.createOrder': '新增销售订单',
  'SalesController.cancelOrder': '取消销售订单',
  'SalesController.createShipment': '销售发货',
  'SalesController.createAfterSales': '新增售后记录',
  'SalesController.processAfterSales': '处理售后',
}

const operationOptions = Object.entries(OPERATION_LABELS)
  .map(([value, label]) => ({ value, label }))
  .sort((a, b) => a.label.localeCompare(b.label, 'zh-CN'))

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const detailVisible = ref(false)
const current = ref(null)

const query = reactive({ page: 1, size: 10, username: '', operation: '', status: null })

function operationLabel(operation) {
  if (!operation) return '未知操作'
  return OPERATION_LABELS[operation] || operation
}

function parseSummary(summary) {
  if (!summary) return null
  try {
    return JSON.parse(summary)
  } catch {
    return null
  }
}

// 从请求参数中提取便于识别的业务对象名称
function objectLabel(row) {
  const summary = parseSummary(row?.summary)
  if (!Array.isArray(summary) || summary.length === 0) return ''
  const first = summary[0]
  if (first === null || first === undefined) return ''
  if (typeof first === 'number') return `#${first}`
  if (typeof first === 'string') return first
  const keys = [
    'name', 'realName', 'username', 'supplierCode', 'materialCode',
    'productCode', 'skuCode', 'warehouseCode', 'roleName', 'roleCode',
    'permName', 'permCode',
  ]
  for (const key of keys) {
    if (first[key]) return String(first[key])
  }
  return ''
}

function displayIp(ip) {
  if (!ip) return '—'
  if (ip === '0:0:0:0:0:0:0:1' || ip === '::1') return '本机（127.0.0.1）'
  return ip
}

const prettySummary = computed(() => {
  const summary = current.value?.summary
  if (!summary) return ''
  try {
    return JSON.stringify(JSON.parse(summary), null, 2)
  } catch {
    return summary
  }
})

async function fetchData() {
  loading.value = true
  try {
    const params = {
      page: query.page,
      size: query.size,
      username: query.username || undefined,
      operation: query.operation || undefined,
      status: query.status ?? undefined,
    }
    const r = await listOperationLogs(params)
    tableData.value = r.data.records
    total.value = r.data.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  fetchData()
}

function resetQuery() {
  query.username = ''
  query.operation = ''
  query.status = null
  query.page = 1
  fetchData()
}

function openDetail(row) {
  current.value = row
  detailVisible.value = true
}

onMounted(fetchData)
</script>

<style scoped>
.op-cell { display: flex; align-items: center; gap: 8px; min-width: 0; }
.op-name { color: var(--color-text); white-space: nowrap; }
.op-object {
  display: inline-block; max-width: 220px; padding: 1px 8px;
  border-radius: 10px; background: #f3f5f8; color: var(--color-text-secondary);
  font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.detail-body { padding: 0 4px; }
.detail-section { margin-top: 24px; }
.detail-section h4 {
  margin: 0 0 10px; font-size: 13px; font-weight: 600; color: var(--color-text-secondary);
}
.summary-box {
  margin: 0; padding: 14px; max-height: 260px; overflow: auto;
  background: #f7f8fa; border: 1px solid var(--color-border); border-radius: var(--radius-md);
  font-size: 12px; line-height: 1.6; color: #3f4652; white-space: pre-wrap; word-break: break-all;
}
.muted { margin: 0; font-size: 13px; color: var(--color-text-secondary); }
.technical { padding-top: 16px; border-top: 1px dashed var(--color-border); }
.tech-row {
  display: flex; justify-content: space-between; gap: 16px;
  padding: 4px 0; font-size: 12px; color: var(--color-text-secondary);
}
.mono { font-family: Consolas, Monaco, monospace; text-align: right; word-break: break-all; }
.error-text { color: var(--color-danger); }
.table-footer { display: flex; justify-content: flex-end; margin-top: var(--spacing-md); }
</style>