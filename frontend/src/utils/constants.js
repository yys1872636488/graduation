/**
 * 企业级标准化枚举常量
 * 统一管理所有业务模块的下拉选项，便于后续扩展和维护
 */

// ========================================
// 原材料
// ========================================

/** 原材料类别 */
export const MATERIAL_CATEGORIES = [
  '面料', '辅料', '里料', '填充料', '其他',
]

/** 布料颜色（通用颜色标准，原材料 + SKU 共用） */
export const COLOR_OPTIONS = [
  '白色', '黑色', '红色', '蓝色', '绿色',
  '黄色', '紫色', '灰色', '棕色', '粉色',
  '橙色', '卡其色', '藏青色', '米色', '其他',
]

/** 原材料单位 */
export const MATERIAL_UNITS = ['米', '千克', '码', '匹', '卷', '件']

// ========================================
// 产品
// ========================================

/** 产品品类 */
export const PRODUCT_CATEGORIES = [
  '上衣', '裤子', '裙子', '外套', '内衣', '配饰', '其他',
]

/** 产品款式 */
export const PRODUCT_STYLES = [
  '圆领', 'V领', '翻领', '连帽', '立领',
  '短袖', '长袖', '无袖',
  '直筒', '修身', '宽松', '阔腿',
  '其他',
]

/** 季节/系列 */
export const SEASON_OPTIONS = [
  '春季', '夏季', '秋季', '冬季',
  '春夏', '秋冬', '四季通用',
]

// ========================================
// SKU
// ========================================

/** 尺码标准（服装行业通用） */
export const SIZE_OPTIONS = [
  'XS', 'S', 'M', 'L', 'XL', 'XXL', 'XXXL', '均码',
]

/** SKU 单位 */
export const SKU_UNITS = ['件', '套', '双', '条']

// ========================================
// 仓库
// ========================================

/** 仓库类型 */
export const WAREHOUSE_TYPES = [
  '原料仓', '成品仓', '半成品仓', '退货仓', '其他',
]

// ========================================
// 供应商
// ========================================

/** 供应商类型 */
export const SUPPLIER_TYPES = [
  '面料供应商', '辅料供应商', '综合供应商', '其他',
]

// ========================================
// 采购管理
// ========================================

/** 采购订单状态 */
export const PURCHASE_ORDER_STATUS = {
  DRAFT: { label: '草稿', type: 'info' },
  SUBMITTED: { label: '已提交', type: 'primary' },
  PARTIAL_RECEIVED: { label: '部分到货', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'danger' },
}

/** 原材料批次检验状态 */
export const BATCH_INSPECTION_STATUS = {
  PENDING_INSPECTION: { label: '待检', type: 'warning' },
  INSPECTING: { label: '检测中', type: 'primary' },
  PENDING_REVIEW: { label: '待复核', type: 'warning' },
  QUALIFIED: { label: '合格', type: 'success' },
  UNQUALIFIED: { label: '不合格', type: 'danger' },
  STOCKED: { label: '已入库', type: 'success' },
  DEPLETED: { label: '已耗尽', type: 'info' },
  ON_HOLD: { label: '暂停使用', type: 'info' },
}

/** 机器视觉检测结果状态 */
export const DETECTION_STATUS = {
  SUCCESS: { label: '检测完成', type: 'success' },
  MODEL_NOT_CONFIGURED: { label: '检测服务未配置', type: 'warning' },
  SERVICE_UNAVAILABLE: { label: '视觉服务不可用', type: 'danger' },
  FAILED: { label: '检测失败', type: 'danger' },
}

/** 不合格处理方式（来料） */
export const DISPOSITIONS = [
  { value: 'RETURN', label: '退货' },
  { value: 'ISOLATE', label: '隔离' },
  { value: 'SCRAP', label: '报废' },
  { value: 'CONCESSION', label: '让步接收' },
]

// ========================================
// 生产管理
// ========================================

/** 生产计划状态 */
export const PRODUCTION_PLAN_STATUS = {
  DRAFT: { label: '草稿', type: 'info' },
  RELEASED: { label: '已下达', type: 'primary' },
  IN_PROGRESS: { label: '生产中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'danger' },
}

/** 生产工单状态 */
export const WORK_ORDER_STATUS = {
  PENDING: { label: '待执行', type: 'info' },
  IN_PROGRESS: { label: '生产中', type: 'primary' },
  PENDING_QC: { label: '待质检', type: 'warning' },
  REWORKING: { label: '返工中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'danger' },
}

/** 成品质检结果 */
export const FINISHED_INSPECTION_RESULT = {
  QUALIFIED: { label: '合格', type: 'success' },
  PARTIAL: { label: '部分合格', type: 'warning' },
  UNQUALIFIED: { label: '不合格', type: 'danger' },
}

/** 不合格品处理类型 */
export const QUALITY_DISPOSITION_TYPES = [
  { value: 'REWORK', label: '返工' },
  { value: 'RECHECK', label: '复检' },
  { value: 'SCRAP', label: '报废' },
]

/** 生产工序（当前工序） */
export const PRODUCTION_PROCESSES = [
  { value: 'CUTTING', label: '裁剪' },
  { value: 'SEWING', label: '缝制' },
  { value: 'IRONING', label: '整烫' },
  { value: 'PACKING', label: '包装' },
  { value: 'OTHER', label: '其他' },
]

/** 成品批次状态 */
export const FINISHED_BATCH_STATUS = {
  PENDING_INBOUND: { label: '待入库', type: 'warning' },
  AVAILABLE: { label: '可售库存', type: 'success' },
  DEPLETED: { label: '已耗尽', type: 'info' },
}

// ========================================
// 库存与销售
// ========================================

/** 库存对象类型 */
export const INVENTORY_ITEM_TYPES = {
  MATERIAL: { label: '原材料' },
  FINISHED: { label: '成品' },
}

/** 库存业务类型 */
export const INVENTORY_BUSINESS_TYPES = {
  MATERIAL_INBOUND: { label: '原材料入库', type: 'success' },
  MATERIAL_ISSUE: { label: '生产领料', type: 'warning' },
  FINISHED_INBOUND: { label: '成品入库', type: 'success' },
  SALES_OUTBOUND: { label: '销售出库', type: 'primary' },
}

/** 销售订单状态 */
export const SALES_ORDER_STATUS = {
  DRAFT: { label: '草稿', type: 'info' },
  PENDING_SHIPMENT: { label: '待发货', type: 'warning' },
  PARTIAL_SHIPPED: { label: '部分发货', type: 'warning' },
  SHIPPED: { label: '已发货', type: 'success' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'danger' },
}

/** 售后类型 */
export const AFTER_SALES_TYPES = [
  { value: 'RETURN', label: '退货' },
  { value: 'EXCHANGE', label: '换货' },
  { value: 'REPAIR', label: '维修' },
  { value: 'CONSULT', label: '咨询' },
]

/** 售后状态 */
export const AFTER_SALES_STATUS = {
  PENDING: { label: '待处理', type: 'warning' },
  PROCESSING: { label: '处理中', type: 'primary' },
  PENDING_ACCEPTANCE: { label: '待验收', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  CLOSED: { label: '已关闭', type: 'info' },
}