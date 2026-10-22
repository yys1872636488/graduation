import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录', noAuth: true },
  },
  {
    path: '/',
    name: 'MainLayout',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      // 工作台
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { title: '工作台' } },
      // 系统管理
      { path: 'system/users', name: 'UserManage', component: () => import('@/views/system/UserManage.vue'), meta: { title: '用户管理' } },
      { path: 'system/roles', name: 'RoleManage', component: () => import('@/views/system/RoleManage.vue'), meta: { title: '角色管理' } },
      { path: 'system/logs', name: 'OperationLog', component: () => import('@/views/system/OperationLog.vue'), meta: { title: '操作日志' } },
      // 基础档案
      { path: 'basic/suppliers', name: 'SupplierManage', component: () => import('@/views/basic/SupplierManage.vue'), meta: { title: '供应商管理' } },
      { path: 'basic/materials', name: 'MaterialManage', component: () => import('@/views/basic/MaterialManage.vue'), meta: { title: '原材料管理' } },
      { path: 'basic/products', name: 'ProductManage', component: () => import('@/views/basic/ProductManage.vue'), meta: { title: '产品管理' } },
      { path: 'basic/skus', name: 'SkuManage', component: () => import('@/views/basic/SkuManage.vue'), meta: { title: 'SKU管理' } },
      { path: 'basic/warehouses', name: 'WarehouseManage', component: () => import('@/views/basic/WarehouseManage.vue'), meta: { title: '仓库管理' } },
      // 采购管理
      { path: 'procurement/orders', name: 'PurchaseOrderList', component: () => import('@/views/procurement/PurchaseOrderList.vue'), meta: { title: '采购订单' } },
      { path: 'procurement/arrivals', name: 'ArrivalManage', component: () => import('@/views/procurement/ArrivalManage.vue'), meta: { title: '到货登记' } },
      { path: 'procurement/batches', name: 'MaterialBatchList', component: () => import('@/views/procurement/MaterialBatchList.vue'), meta: { title: '原材料批次' } },
      // 来料质检
      { path: 'inspection/detect', name: 'InspectionUpload', component: () => import('@/views/inspection/InspectionUpload.vue'), meta: { title: '布料检测' } },
      { path: 'inspection/review', name: 'InspectionReview', component: () => import('@/views/inspection/InspectionReview.vue'), meta: { title: '质检复核' } },
      { path: 'inspection/records', name: 'InspectionRecordList', component: () => import('@/views/inspection/InspectionRecordList.vue'), meta: { title: '质检记录' } },
      // 生产管理
      { path: 'production/plans', name: 'ProductionPlanList', component: () => import('@/views/production/ProductionPlanList.vue'), meta: { title: '生产计划' } },
      { path: 'production/orders', name: 'WorkOrderList', component: () => import('@/views/production/WorkOrderList.vue'), meta: { title: '生产工单' } },
      { path: 'production/issues', name: 'MaterialIssue', component: () => import('@/views/production/MaterialIssue.vue'), meta: { title: '领料管理' } },
      { path: 'production/progress', name: 'ProductionProgress', component: () => import('@/views/production/ProductionProgress.vue'), meta: { title: '生产进度' } },
      { path: 'production/qc', name: 'FinishedInspection', component: () => import('@/views/production/FinishedInspection.vue'), meta: { title: '成品质检' } },
      // 仓储管理
      { path: 'warehouse/inventory', name: 'InventoryList', component: () => import('@/views/warehouse/InventoryList.vue'), meta: { title: '库存查询' } },
      { path: 'warehouse/inbound', name: 'InboundManage', component: () => import('@/views/warehouse/InboundManage.vue'), meta: { title: '入库管理' } },
      { path: 'warehouse/outbound', name: 'OutboundManage', component: () => import('@/views/warehouse/OutboundManage.vue'), meta: { title: '出库管理' } },
      { path: 'warehouse/transactions', name: 'InventoryTransaction', component: () => import('@/views/warehouse/InventoryTransaction.vue'), meta: { title: '库存流水' } },
      // 销售管理
      { path: 'sales/orders', name: 'SalesOrderList', component: () => import('@/views/sales/SalesOrderList.vue'), meta: { title: '销售订单' } },
      { path: 'sales/shipments', name: 'ShipmentManage', component: () => import('@/views/sales/ShipmentManage.vue'), meta: { title: '发货管理' } },
      { path: 'sales/aftersales', name: 'AfterSalesManage', component: () => import('@/views/sales/AfterSalesManage.vue'), meta: { title: '售后管理' } },
      // 追溯统计
      { path: 'traceability/forward', name: 'ForwardTrace', component: () => import('@/views/traceability/ForwardTrace.vue'), meta: { title: '正向追溯' } },
      { path: 'traceability/reverse', name: 'ReverseTrace', component: () => import('@/views/traceability/ReverseTrace.vue'), meta: { title: '反向追溯' } },
      { path: 'traceability/statistics', name: 'StatisticsView', component: () => import('@/views/traceability/StatisticsView.vue'), meta: { title: '统计分析' } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 追溯管理系统` : '追溯管理系统'
  const token = localStorage.getItem('token')
  if (to.meta.noAuth) {
    next()
    return
  }
  if (!token) {
    next('/login')
    return
  }
  try {
    // 刷新页面后 Pinia 状态为空，重新拉取用户、角色、权限和菜单
    await useUserStore().ensureUserInfo()
    next()
  } catch (e) {
    localStorage.removeItem('token')
    next('/login')
  }
})

export default router