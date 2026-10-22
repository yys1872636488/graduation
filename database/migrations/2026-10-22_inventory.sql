-- =============================================
-- 库存管理模块增量迁移脚本
-- 执行：mysql --default-character-set=utf8mb4 -u root -p graduation_db < database/migrations/2026-10-22_inventory.sql
-- =============================================
USE graduation_db;

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
