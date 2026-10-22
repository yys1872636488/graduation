-- =============================================
-- 质检记录菜单与权限增量脚本
-- 执行：mysql -u root -p graduation_db < database/migrations/2026-10-22_inspection-record.sql
-- =============================================

USE graduation_db;

SET @inspection_id = (
    SELECT id FROM sys_permission WHERE perm_code = 'inspection' LIMIT 1
);

INSERT INTO sys_permission
    (parent_id, perm_code, perm_name, type, path, component, icon, sort_order)
SELECT @inspection_id, 'inspection:record', '质检记录', 'menu',
       '/inspection/records', 'inspection/InspectionRecordList', 'DocumentChecked', 76
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE perm_code = 'inspection:record'
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.perm_code = 'inspection:record'
WHERE r.role_code IN ('ADMIN', 'PURCHASER', 'PRODUCTION_QC')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 采购人员没有“来料质检”父菜单，补充父菜单授权后才能在菜单树中显示“质检记录”
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.perm_code = 'inspection'
WHERE r.role_code = 'PURCHASER'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );