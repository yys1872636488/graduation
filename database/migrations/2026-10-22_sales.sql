-- =============================================
-- 销售与售后模块增量迁移脚本
-- 执行：mysql --default-character-set=utf8mb4 -u root -p graduation_db < database/migrations/2026-10-22_sales.sql
-- =============================================
USE graduation_db;

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
