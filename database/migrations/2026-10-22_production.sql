-- =============================================
-- 生产与成品质检模块增量迁移脚本
-- 执行：mysql --default-character-set=utf8mb4 -u root -p graduation_db < database/migrations/2026-10-22_production.sql
-- =============================================
USE graduation_db;

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
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_wo_no (work_order_no),
    INDEX idx_wo_plan (plan_id),
    INDEX idx_wo_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产工单';

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
