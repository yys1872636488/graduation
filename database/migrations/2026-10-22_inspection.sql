-- =============================================
-- 来料质检模块增量迁移脚本
-- 执行：mysql -u root -p graduation_db < database/migrations/2026-10-22_inspection.sql
-- =============================================

USE graduation_db;

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