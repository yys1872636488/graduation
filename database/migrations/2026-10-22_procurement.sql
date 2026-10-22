-- =============================================
-- 采购管理模块增量迁移脚本
-- 适用：已执行过旧版 schema.sql 的 graduation_db
-- 执行：mysql -u root -p graduation_db < database/migrations/2026-10-22_procurement.sql
-- =============================================

USE graduation_db;

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