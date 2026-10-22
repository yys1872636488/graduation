-- =============================================
-- 生产流程企业化改造增量脚本
-- 执行：mysql --default-character-set=utf8mb4 -u root -p graduation_db < database/migrations/2026-10-22_production-workflow.sql
-- 说明：脚本可重复执行
-- =============================================

USE graduation_db;

-- 1. 生产计划/工单新增时间与取消字段
DROP PROCEDURE IF EXISTS add_column_if_not_exists;
DELIMITER //
CREATE PROCEDURE add_column_if_not_exists(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = tbl AND column_name = col
    ) THEN
        SET @s = CONCAT('ALTER TABLE ', tbl, ' ADD COLUMN ', ddl);
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_column_if_not_exists('production_plan', 'released_at',
    'released_at DATETIME DEFAULT NULL COMMENT ''下达时间''');
CALL add_column_if_not_exists('production_plan', 'completed_at',
    'completed_at DATETIME DEFAULT NULL COMMENT ''完成时间''');
CALL add_column_if_not_exists('work_order', 'actual_start_time',
    'actual_start_time DATETIME DEFAULT NULL COMMENT ''实际开工时间''');
CALL add_column_if_not_exists('work_order', 'actual_end_time',
    'actual_end_time DATETIME DEFAULT NULL COMMENT ''实际完工时间''');
CALL add_column_if_not_exists('work_order', 'cancel_reason',
    'cancel_reason VARCHAR(500) DEFAULT '''' COMMENT ''取消原因''');

DROP PROCEDURE IF EXISTS add_column_if_not_exists;

-- 2. 状态流转日志
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

-- 3. 生产流程细化权限
SET @production_id = (
    SELECT id FROM sys_permission WHERE perm_code = 'production' LIMIT 1
);

INSERT INTO sys_permission
    (parent_id, perm_code, perm_name, type, path, component, icon, sort_order)
SELECT @production_id, 'production:plan:release', '下达生产计划', 'button', '', '', '', 95
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE perm_code = 'production:plan:release');

INSERT INTO sys_permission
    (parent_id, perm_code, perm_name, type, path, component, icon, sort_order)
SELECT @production_id, 'production:order:start', '开始生产', 'button', '', '', '', 96
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE perm_code = 'production:order:start');

INSERT INTO sys_permission
    (parent_id, perm_code, perm_name, type, path, component, icon, sort_order)
SELECT @production_id, 'production:order:report', '生产报工', 'button', '', '', '', 97
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE perm_code = 'production:order:report');

INSERT INTO sys_permission
    (parent_id, perm_code, perm_name, type, path, component, icon, sort_order)
SELECT @production_id, 'production:order:cancel', '取消工单', 'button', '', '', '', 98
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE perm_code = 'production:order:cancel');

-- 分配给管理员和生产质检角色
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p
    ON p.perm_code IN ('production:plan:release', 'production:order:start',
                       'production:order:report', 'production:order:cancel')
WHERE r.role_code IN ('ADMIN', 'PRODUCTION_QC')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );