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