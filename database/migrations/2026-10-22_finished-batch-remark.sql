-- =============================================
-- 成品批次备注字段增量脚本
-- 执行：mysql --default-character-set=utf8mb4 -u root -p graduation_db < database/migrations/2026-10-22_finished-batch-remark.sql
-- 说明：脚本可重复执行
-- =============================================

USE graduation_db;

DROP PROCEDURE IF EXISTS add_finished_batch_remark;
DELIMITER //
CREATE PROCEDURE add_finished_batch_remark()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'finished_batch'
          AND column_name = 'remark'
    ) THEN
        ALTER TABLE finished_batch
            ADD COLUMN remark VARCHAR(500) DEFAULT '' COMMENT '备注' AFTER warehouse_id;
    END IF;
END//
DELIMITER ;

CALL add_finished_batch_remark();
DROP PROCEDURE IF EXISTS add_finished_batch_remark;