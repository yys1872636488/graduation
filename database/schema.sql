-- =============================================
-- 服装产品数字化追溯管理系统 - 数据库初始化脚本
-- Database: MySQL 8.0
-- Charset: utf8mb4
-- =============================================

CREATE DATABASE IF NOT EXISTS graduation_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE graduation_db;

-- =============================================
-- 阶段 1：系统管理与权限
-- =============================================

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码哈希',
    real_name VARCHAR(50) DEFAULT '' COMMENT '真实姓名',
    phone VARCHAR(20) DEFAULT '' COMMENT '手机号',
    email VARCHAR(100) DEFAULT '' COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=正常, 0=禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删除',
    INDEX idx_sys_user_username (username),
    INDEX idx_sys_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    description VARCHAR(200) DEFAULT '' COMMENT '说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_sys_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色';

-- 权限表（菜单 + 按钮）
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父权限ID，0为顶级',
    perm_code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限标识，如 purchase:read',
    perm_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    type VARCHAR(20) NOT NULL DEFAULT 'button' COMMENT '类型: menu=菜单, button=按钮',
    path VARCHAR(200) DEFAULT '' COMMENT '前端路由路径（菜单类型）',
    component VARCHAR(200) DEFAULT '' COMMENT '前端组件路径（菜单类型）',
    icon VARCHAR(50) DEFAULT '' COMMENT '图标名称（菜单类型）',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_sys_perm_parent (parent_id),
    INDEX idx_sys_perm_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统权限';

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_ur_user (user_id),
    INDEX idx_ur_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联';

-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    UNIQUE KEY uk_role_perm (role_id, permission_id),
    INDEX idx_rp_role (role_id),
    INDEX idx_rp_perm (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联';

-- 操作日志表
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT '操作人ID',
    username VARCHAR(50) DEFAULT '' COMMENT '操作人用户名',
    operation VARCHAR(100) NOT NULL COMMENT '操作类型/描述',
    object_type VARCHAR(50) DEFAULT '' COMMENT '操作对象类型',
    object_id VARCHAR(50) DEFAULT '' COMMENT '操作对象ID',
    summary TEXT COMMENT '操作摘要/请求参数',
    request_method VARCHAR(10) DEFAULT '' COMMENT '请求方法',
    request_url VARCHAR(500) DEFAULT '' COMMENT '请求URL',
    request_ip VARCHAR(50) DEFAULT '' COMMENT '请求IP',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '操作结果: 1=成功, 0=失败',
    error_msg TEXT COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_sys_log_user (user_id),
    INDEX idx_sys_log_created (created_at),
    INDEX idx_sys_log_operation (operation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志';

-- =============================================
-- 阶段 2：基础档案
-- =============================================

CREATE TABLE IF NOT EXISTS supplier (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_code VARCHAR(50) NOT NULL UNIQUE COMMENT '供应商编号',
    name VARCHAR(100) NOT NULL COMMENT '供应商名称',
    contact_person VARCHAR(50) DEFAULT '' COMMENT '联系人',
    phone VARCHAR(20) DEFAULT '' COMMENT '电话',
    address VARCHAR(200) DEFAULT '' COMMENT '地址',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=正常 0=已停用',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_supplier_code (supplier_code),
    INDEX idx_supplier_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商';

CREATE TABLE IF NOT EXISTS material (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_code VARCHAR(50) NOT NULL UNIQUE COMMENT '原材料编号',
    name VARCHAR(100) NOT NULL COMMENT '原材料名称',
    category VARCHAR(50) DEFAULT '' COMMENT '类别',
    specification VARCHAR(200) DEFAULT '' COMMENT '规格',
    color VARCHAR(50) DEFAULT '' COMMENT '颜色',
    unit VARCHAR(20) NOT NULL DEFAULT '' COMMENT '单位',
    default_supplier_id BIGINT DEFAULT NULL COMMENT '默认供应商',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=正常 0=已停用',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_material_code (material_code),
    INDEX idx_material_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='原材料主数据';

CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(50) NOT NULL UNIQUE COMMENT '产品编号',
    name VARCHAR(100) NOT NULL COMMENT '产品名称',
    category VARCHAR(50) DEFAULT '' COMMENT '品类',
    style VARCHAR(50) DEFAULT '' COMMENT '款式',
    season VARCHAR(50) DEFAULT '' COMMENT '季节/系列',
    description VARCHAR(500) DEFAULT '' COMMENT '描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=正常 0=已停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_product_code (product_code),
    INDEX idx_product_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服装产品';

CREATE TABLE IF NOT EXISTS product_sku (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL COMMENT '所属产品ID',
    sku_code VARCHAR(50) NOT NULL UNIQUE COMMENT 'SKU编码',
    color VARCHAR(50) DEFAULT '' COMMENT '颜色',
    size_label VARCHAR(50) DEFAULT '' COMMENT '尺码',
    unit VARCHAR(20) NOT NULL DEFAULT '件' COMMENT '单位',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=正常 0=已停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_sku_code (sku_code),
    INDEX idx_sku_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品SKU';

CREATE TABLE IF NOT EXISTS warehouse (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    warehouse_code VARCHAR(50) NOT NULL UNIQUE COMMENT '仓库编号',
    name VARCHAR(100) NOT NULL COMMENT '仓库名称',
    location VARCHAR(200) DEFAULT '' COMMENT '位置',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=正常 0=已停用',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_warehouse_code (warehouse_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓库';

-- =============================================
-- 阶段 3：采购管理
-- =============================================

CREATE TABLE IF NOT EXISTS purchase_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '采购订单号',
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    order_date DATE NOT NULL COMMENT '下单日期',
    expected_date DATE DEFAULT NULL COMMENT '预计到货日期',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED/PARTIAL_RECEIVED/COMPLETED/CANCELLED',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    submitted_at DATETIME DEFAULT NULL COMMENT '提交时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_po_no (order_no),
    INDEX idx_po_supplier (supplier_id),
    INDEX idx_po_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单';

CREATE TABLE IF NOT EXISTS purchase_order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '采购订单ID',
    material_id BIGINT NOT NULL COMMENT '原材料ID',
    quantity DECIMAL(14,2) NOT NULL COMMENT '采购数量',
    unit VARCHAR(20) NOT NULL DEFAULT '' COMMENT '单位',
    unit_price DECIMAL(14,2) DEFAULT NULL COMMENT '单价',
    received_quantity DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '已到货数量',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_poi_order (order_id),
    INDEX idx_poi_material (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单明细';

CREATE TABLE IF NOT EXISTS arrival_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    arrival_no VARCHAR(50) NOT NULL UNIQUE COMMENT '到货单号',
    order_id BIGINT NOT NULL COMMENT '采购订单ID',
    order_item_id BIGINT NOT NULL COMMENT '采购订单明细ID',
    material_id BIGINT NOT NULL COMMENT '原材料ID',
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    quantity DECIMAL(14,2) NOT NULL COMMENT '到货数量',
    unit VARCHAR(20) NOT NULL DEFAULT '' COMMENT '单位',
    arrival_date DATE NOT NULL COMMENT '到货日期',
    supplier_batch_no VARCHAR(100) DEFAULT '' COMMENT '供应商批次号',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_by BIGINT DEFAULT NULL COMMENT '登记人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登记时间',
    INDEX idx_arrival_no (arrival_no),
    INDEX idx_arrival_order (order_id),
    INDEX idx_arrival_item (order_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='到货登记';

CREATE TABLE IF NOT EXISTS material_batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_no VARCHAR(50) NOT NULL UNIQUE COMMENT '系统批次号',
    material_id BIGINT NOT NULL COMMENT '原材料ID',
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    purchase_order_id BIGINT NOT NULL COMMENT '来源采购订单ID',
    purchase_order_item_id BIGINT NOT NULL COMMENT '来源采购明细ID',
    arrival_id BIGINT NOT NULL COMMENT '来源到货登记ID',
    supplier_batch_no VARCHAR(100) DEFAULT '' COMMENT '供应商批次号',
    quantity DECIMAL(14,2) NOT NULL COMMENT '到货数量',
    unit VARCHAR(20) NOT NULL DEFAULT '' COMMENT '单位',
    arrival_date DATE NOT NULL COMMENT '到货日期',
    inspection_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_INSPECTION'
        COMMENT 'PENDING_INSPECTION/INSPECTING/PENDING_REVIEW/QUALIFIED/UNQUALIFIED/STOCKED/DEPLETED/ON_HOLD',
    available_quantity DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '可用数量',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_batch_no (batch_no),
    INDEX idx_batch_material (material_id),
    INDEX idx_batch_status (inspection_status),
    INDEX idx_batch_order (purchase_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='原材料批次';

-- =============================================
-- 阶段 3：来料质检
-- =============================================

CREATE TABLE IF NOT EXISTS inspection_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL COMMENT '原材料批次ID',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_path VARCHAR(500) NOT NULL COMMENT '相对存储路径',
    content_type VARCHAR(100) NOT NULL COMMENT '文件类型',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    image_width INT DEFAULT NULL COMMENT '图片宽度',
    image_height INT DEFAULT NULL COMMENT '图片高度',
    created_by BIGINT DEFAULT NULL COMMENT '上传人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    INDEX idx_image_batch (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='布料检测图片';

CREATE TABLE IF NOT EXISTS vision_detection_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL COMMENT '原材料批次ID',
    image_id BIGINT NOT NULL COMMENT '图片ID',
    model_name VARCHAR(100) DEFAULT '' COMMENT '模型名称',
    model_version VARCHAR(50) DEFAULT '' COMMENT '模型版本',
    status VARCHAR(30) NOT NULL COMMENT 'SUCCESS/MODEL_NOT_CONFIGURED/SERVICE_UNAVAILABLE/FAILED',
    image_width INT DEFAULT NULL COMMENT '图片宽度',
    image_height INT DEFAULT NULL COMMENT '图片高度',
    detection_count INT NOT NULL DEFAULT 0 COMMENT '检测框数量',
    detections_json LONGTEXT COMMENT '检测框 JSON',
    annotated_image_path VARCHAR(500) DEFAULT '' COMMENT '标注图路径',
    inference_time_ms INT DEFAULT NULL COMMENT '推理耗时（毫秒）',
    error_message VARCHAR(1000) DEFAULT '' COMMENT '错误信息',
    created_by BIGINT DEFAULT NULL COMMENT '检测发起人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
    INDEX idx_detection_batch (batch_id),
    INDEX idx_detection_image (image_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机器视觉检测结果';

CREATE TABLE IF NOT EXISTS inspection_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL COMMENT '原材料批次ID',
    image_id BIGINT DEFAULT NULL COMMENT '关联图片ID',
    detection_id BIGINT DEFAULT NULL COMMENT '关联检测结果ID',
    reviewer_id BIGINT NOT NULL COMMENT '复核人ID',
    review_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '复核时间',
    conclusion VARCHAR(30) NOT NULL COMMENT 'QUALIFIED/UNQUALIFIED/ON_HOLD',
    opinion VARCHAR(1000) DEFAULT '' COMMENT '复核意见',
    disposition VARCHAR(50) DEFAULT '' COMMENT '不合格处理方式',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_review_batch (batch_id),
    INDEX idx_review_reviewer (reviewer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人工质检复核记录';

-- =============================================
-- 阶段 4：生产与成品质检
-- =============================================

CREATE TABLE IF NOT EXISTS production_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_no VARCHAR(50) NOT NULL UNIQUE COMMENT '生产计划号',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    planned_quantity DECIMAL(14,2) NOT NULL COMMENT '计划数量',
    start_date DATE NOT NULL COMMENT '计划开始日期',
    end_date DATE DEFAULT NULL COMMENT '计划结束日期',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'LOW/NORMAL/HIGH',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/RELEASED/IN_PROGRESS/COMPLETED/CANCELLED',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    released_at DATETIME DEFAULT NULL COMMENT '下达时间',
    completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_pp_no (plan_no),
    INDEX idx_pp_sku (sku_id),
    INDEX idx_pp_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产计划';

CREATE TABLE IF NOT EXISTS work_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '工单号',
    plan_id BIGINT DEFAULT NULL COMMENT '关联生产计划ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    planned_quantity DECIMAL(14,2) NOT NULL COMMENT '计划生产数量',
    completed_quantity DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '已完成数量',
    qualified_quantity DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '质检合格数量',
    manager VARCHAR(50) DEFAULT '' COMMENT '负责人',
    start_date DATE DEFAULT NULL COMMENT '开始日期',
    expected_end_date DATE DEFAULT NULL COMMENT '预计完成日期',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/PENDING_QC/REWORKING/COMPLETED/CANCELLED',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    actual_start_time DATETIME DEFAULT NULL COMMENT '实际开工时间',
    actual_end_time DATETIME DEFAULT NULL COMMENT '实际完工时间',
    cancel_reason VARCHAR(500) DEFAULT '' COMMENT '取消原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_wo_no (work_order_no),
    INDEX idx_wo_plan (plan_id),
    INDEX idx_wo_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产工单';

CREATE TABLE IF NOT EXISTS production_status_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    biz_type VARCHAR(20) NOT NULL COMMENT 'PLAN/WORK_ORDER',
    biz_id BIGINT NOT NULL COMMENT '业务ID',
    from_status VARCHAR(30) DEFAULT '' COMMENT '原状态',
    to_status VARCHAR(30) NOT NULL COMMENT '新状态',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    remark VARCHAR(500) DEFAULT '' COMMENT '说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_psl_biz (biz_type, biz_id),
    INDEX idx_psl_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产状态流转日志';

CREATE TABLE IF NOT EXISTS material_issue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    issue_no VARCHAR(50) NOT NULL UNIQUE COMMENT '领料单号',
    work_order_id BIGINT NOT NULL COMMENT '工单ID',
    issue_date DATE NOT NULL COMMENT '领料日期',
    issuer VARCHAR(50) DEFAULT '' COMMENT '领料人',
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED' COMMENT 'ISSUED',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_mi_no (issue_no),
    INDEX idx_mi_order (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='领料单';

CREATE TABLE IF NOT EXISTS material_issue_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    issue_id BIGINT NOT NULL COMMENT '领料单ID',
    material_batch_id BIGINT NOT NULL COMMENT '原材料批次ID',
    material_id BIGINT NOT NULL COMMENT '原材料ID',
    quantity DECIMAL(14,2) NOT NULL COMMENT '领料数量',
    unit VARCHAR(20) NOT NULL DEFAULT '' COMMENT '单位',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_mii_issue (issue_id),
    INDEX idx_mii_batch (material_batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='领料明细';

CREATE TABLE IF NOT EXISTS production_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_order_id BIGINT NOT NULL COMMENT '工单ID',
    progress_date DATE NOT NULL COMMENT '进度日期',
    process_name VARCHAR(100) DEFAULT '' COMMENT '工序/进度说明',
    completed_quantity DECIMAL(14,2) NOT NULL COMMENT '累计完成数量',
    progress_percent DECIMAL(5,2) DEFAULT NULL COMMENT '进度百分比',
    recorder VARCHAR(50) DEFAULT '' COMMENT '登记人',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登记时间',
    INDEX idx_progress_order (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产进度';

CREATE TABLE IF NOT EXISTS finished_inspection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inspection_no VARCHAR(50) NOT NULL UNIQUE COMMENT '成品质检单号',
    work_order_id BIGINT NOT NULL COMMENT '工单ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    inspected_quantity DECIMAL(14,2) NOT NULL COMMENT '送检数量',
    qualified_quantity DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '合格数量',
    unqualified_quantity DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '不合格数量',
    result VARCHAR(20) NOT NULL COMMENT 'QUALIFIED/PARTIAL/UNQUALIFIED',
    inspector VARCHAR(50) DEFAULT '' COMMENT '检验人',
    inspection_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检验时间',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_fi_no (inspection_no),
    INDEX idx_fi_order (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成品质检';

CREATE TABLE IF NOT EXISTS quality_disposition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inspection_id BIGINT NOT NULL COMMENT '成品质检ID',
    work_order_id BIGINT NOT NULL COMMENT '工单ID',
    type VARCHAR(20) NOT NULL COMMENT 'REWORK/RECHECK/SCRAP',
    quantity DECIMAL(14,2) NOT NULL COMMENT '处理数量',
    reason VARCHAR(500) DEFAULT '' COMMENT '原因',
    handler VARCHAR(50) DEFAULT '' COMMENT '处理人',
    handled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_qd_inspection (inspection_id),
    INDEX idx_qd_order (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='不合格品处理';

CREATE TABLE IF NOT EXISTS finished_batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_no VARCHAR(50) NOT NULL UNIQUE COMMENT '成品批次号',
    work_order_id BIGINT NOT NULL COMMENT '工单ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    quantity DECIMAL(14,2) NOT NULL COMMENT '批次数量',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_INBOUND' COMMENT 'PENDING_INBOUND/AVAILABLE/DEPLETED',
    production_date DATE NOT NULL COMMENT '生产日期',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库ID（入库后填写）',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_fb_no (batch_no),
    INDEX idx_fb_order (work_order_id),
    INDEX idx_fb_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成品批次';

CREATE TABLE IF NOT EXISTS finished_batch_material (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    finished_batch_id BIGINT NOT NULL COMMENT '成品批次ID',
    material_batch_id BIGINT NOT NULL COMMENT '原材料批次ID',
    quantity DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '使用数量',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_fbm_batch (finished_batch_id),
    INDEX idx_fbm_material (material_batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成品批次与原材料批次关联';

-- =============================================
-- 阶段 5：库存管理
-- =============================================

CREATE TABLE IF NOT EXISTS inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    item_type VARCHAR(20) NOT NULL COMMENT 'MATERIAL/FINISHED',
    material_id BIGINT DEFAULT NULL COMMENT '原材料ID（原料库存）',
    sku_id BIGINT DEFAULT NULL COMMENT 'SKU ID（成品库存）',
    batch_id BIGINT NOT NULL COMMENT '原材料批次ID或成品批次ID',
    quantity DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '库存数量',
    available_quantity DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '可用数量',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_inventory (warehouse_id, item_type, batch_id),
    INDEX idx_inv_warehouse (warehouse_id),
    INDEX idx_inv_type (item_type),
    INDEX idx_inv_material (material_id),
    INDEX idx_inv_sku (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存余额';

CREATE TABLE IF NOT EXISTS inventory_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_no VARCHAR(50) NOT NULL UNIQUE COMMENT '流水号',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    item_type VARCHAR(20) NOT NULL COMMENT 'MATERIAL/FINISHED',
    material_id BIGINT DEFAULT NULL COMMENT '原材料ID',
    sku_id BIGINT DEFAULT NULL COMMENT 'SKU ID',
    batch_id BIGINT NOT NULL COMMENT '批次ID',
    business_type VARCHAR(30) NOT NULL COMMENT 'MATERIAL_INBOUND/MATERIAL_ISSUE/FINISHED_INBOUND/SALES_OUTBOUND',
    direction VARCHAR(10) NOT NULL COMMENT 'IN/OUT',
    quantity DECIMAL(14,2) NOT NULL COMMENT '本次数量',
    before_quantity DECIMAL(14,2) NOT NULL COMMENT '变更前数量',
    after_quantity DECIMAL(14,2) NOT NULL COMMENT '变更后数量',
    business_no VARCHAR(50) DEFAULT '' COMMENT '关联业务单号',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_it_no (transaction_no),
    INDEX idx_it_warehouse (warehouse_id),
    INDEX idx_it_type (business_type),
    INDEX idx_it_batch (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存流水';

-- =============================================
-- 阶段 5：销售与售后
-- =============================================

CREATE TABLE IF NOT EXISTS sales_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '销售订单号',
    customer_name VARCHAR(100) NOT NULL COMMENT '客户名称',
    order_date DATE NOT NULL COMMENT '下单日期',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_SHIPMENT' COMMENT 'DRAFT/PENDING_SHIPMENT/PARTIAL_SHIPPED/SHIPPED/COMPLETED/CANCELLED',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_so_no (order_no),
    INDEX idx_so_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售订单';

CREATE TABLE IF NOT EXISTS sales_order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '销售订单ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    quantity DECIMAL(14,2) NOT NULL COMMENT '订单数量',
    unit_price DECIMAL(14,2) DEFAULT NULL COMMENT '单价',
    shipped_quantity DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '已发货数量',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_soi_order (order_id),
    INDEX idx_soi_sku (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售订单明细';

CREATE TABLE IF NOT EXISTS shipment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipment_no VARCHAR(50) NOT NULL UNIQUE COMMENT '发货单号',
    order_id BIGINT NOT NULL COMMENT '销售订单ID',
    shipment_date DATE NOT NULL COMMENT '发货日期',
    logistics_no VARCHAR(100) DEFAULT '' COMMENT '物流单号',
    operator VARCHAR(50) DEFAULT '' COMMENT '经办人',
    status VARCHAR(20) NOT NULL DEFAULT 'SHIPPED' COMMENT 'SHIPPED',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_ship_no (shipment_no),
    INDEX idx_ship_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发货单';

CREATE TABLE IF NOT EXISTS shipment_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipment_id BIGINT NOT NULL COMMENT '发货单ID',
    finished_batch_id BIGINT NOT NULL COMMENT '成品批次ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    quantity DECIMAL(14,2) NOT NULL COMMENT '发货数量',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_si_shipment (shipment_id),
    INDEX idx_si_batch (finished_batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发货明细';

CREATE TABLE IF NOT EXISTS after_sales_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_no VARCHAR(50) NOT NULL UNIQUE COMMENT '售后单号',
    sales_order_id BIGINT NOT NULL COMMENT '销售订单ID',
    shipment_id BIGINT DEFAULT NULL COMMENT '发货单ID',
    finished_batch_id BIGINT DEFAULT NULL COMMENT '成品批次ID',
    type VARCHAR(20) NOT NULL COMMENT 'RETURN/EXCHANGE/REPAIR/CONSULT',
    description VARCHAR(1000) DEFAULT '' COMMENT '问题描述',
    apply_date DATE NOT NULL COMMENT '申请日期',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/PENDING_ACCEPTANCE/COMPLETED/CLOSED',
    conclusion VARCHAR(500) DEFAULT '' COMMENT '处理结论',
    acceptance_result VARCHAR(200) DEFAULT '' COMMENT '验收结果',
    handler VARCHAR(50) DEFAULT '' COMMENT '经办人',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_as_no (record_no),
    INDEX idx_as_order (sales_order_id),
    INDEX idx_as_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='售后记录';