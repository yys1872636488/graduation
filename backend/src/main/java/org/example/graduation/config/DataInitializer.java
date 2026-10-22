package org.example.graduation.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            initRoles();
            initPermissions();
            initUsers();
            initRolePermissions();
            log.info("数据初始化完成");
        } catch (Exception e) {
            log.error("数据初始化失败", e);
        }
    }

    // ========================================
    // 角色
    // ========================================
    private void initRoles() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM sys_role", Long.class);
        if (count != null && count > 0) return;

        jdbc.update("INSERT INTO sys_role (role_code, role_name, description) VALUES (?,?,?)",
                "ADMIN", "系统管理员", "管理系统所有功能、用户、角色和权限");
        jdbc.update("INSERT INTO sys_role (role_code, role_name, description) VALUES (?,?,?)",
                "PURCHASER", "采购人员", "管理采购订单、到货登记和原材料批次");
        jdbc.update("INSERT INTO sys_role (role_code, role_name, description) VALUES (?,?,?)",
                "PRODUCTION_QC", "生产与质检人员", "管理生产工单、来料检验和成品质检");
        jdbc.update("INSERT INTO sys_role (role_code, role_name, description) VALUES (?,?,?)",
                "WAREHOUSE_SALES", "仓储与销售人员", "管理库存出入库、销售发货和售后");
        log.info("初始角色创建完成");
    }

    // ========================================
    // 权限
    // ========================================
    private void initPermissions() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM sys_permission", Long.class);
        if (count != null && count > 0) return;

        // 工作台
        long dashboard = perm(0, "dashboard", "工作台", "menu", "/dashboard", "dashboard/DashboardView", "HomeFilled", 1);

        // 系统管理
        long system = perm(0, "system", "系统管理", "menu", "", "", "Setting", 10);
        perm(system, "system:user", "用户管理", "menu", "/system/users", "system/UserManage", "User", 11);
        perm(system, "system:role", "角色管理", "menu", "/system/roles", "system/RoleManage", "Avatar", 12);
        perm(system, "system:log", "操作日志", "menu", "/system/logs", "system/OperationLog", "Document", 13);
        perm(system, "user:read", "查看用户", "button", "", "", "", 14);
        perm(system, "user:create", "新增用户", "button", "", "", "", 15);
        perm(system, "user:edit", "编辑用户", "button", "", "", "", 16);
        perm(system, "user:disable", "禁用/启用用户", "button", "", "", "", 17);
        perm(system, "user:reset-password", "重置密码", "button", "", "", "", 18);
        perm(system, "role:read", "查看角色", "button", "", "", "", 19);
        perm(system, "role:edit", "分配权限", "button", "", "", "", 20);
        perm(system, "log:read", "查看日志", "button", "", "", "", 21);

        // 基础档案
        long basic = perm(0, "basic", "基础档案", "menu", "", "", "FolderOpened", 30);
        perm(basic, "basic:supplier", "供应商管理", "menu", "/basic/suppliers", "basic/SupplierManage", "OfficeBuilding", 31);
        perm(basic, "basic:material", "原材料管理", "menu", "/basic/materials", "basic/MaterialManage", "Goods", 32);
        perm(basic, "basic:product", "产品管理", "menu", "/basic/products", "basic/ProductManage", "ShoppingBag", 33);
        perm(basic, "basic:sku", "SKU管理", "menu", "/basic/skus", "basic/SkuManage", "Grid", 34);
        perm(basic, "basic:warehouse", "仓库管理", "menu", "/basic/warehouses", "basic/WarehouseManage", "Box", 35);
        perm(basic, "supplier:read", "查看供应商", "button", "", "", "", 36);
        perm(basic, "supplier:write", "编辑供应商", "button", "", "", "", 37);
        perm(basic, "material:read", "查看原材料", "button", "", "", "", 38);
        perm(basic, "material:write", "编辑原材料", "button", "", "", "", 39);
        perm(basic, "product:read", "查看产品", "button", "", "", "", 40);
        perm(basic, "product:write", "编辑产品", "button", "", "", "", 41);
        perm(basic, "sku:read", "查看SKU", "button", "", "", "", 42);
        perm(basic, "sku:write", "编辑SKU", "button", "", "", "", 43);
        perm(basic, "warehouse:read", "查看仓库", "button", "", "", "", 44);
        perm(basic, "warehouse:write", "编辑仓库", "button", "", "", "", 45);

        // 采购管理
        long procurement = perm(0, "procurement", "采购管理", "menu", "", "", "Tickets", 50);
        perm(procurement, "procurement:order", "采购订单", "menu", "/procurement/orders", "procurement/PurchaseOrderList", "Document", 51);
        perm(procurement, "procurement:arrival", "到货登记", "menu", "/procurement/arrivals", "procurement/ArrivalManage", "Van", 52);
        perm(procurement, "procurement:batch", "原材料批次", "menu", "/procurement/batches", "procurement/MaterialBatchList", "Collection", 53);
        perm(procurement, "purchase:read", "查看采购订单", "button", "", "", "", 54);
        perm(procurement, "purchase:create", "创建采购订单", "button", "", "", "", 55);
        perm(procurement, "purchase:edit", "编辑采购订单", "button", "", "", "", 56);
        perm(procurement, "purchase:submit", "提交采购订单", "button", "", "", "", 57);
        perm(procurement, "arrival:read", "查看到货", "button", "", "", "", 58);
        perm(procurement, "arrival:register", "登记到货", "button", "", "", "", 59);
        perm(procurement, "batch:read", "查看批次", "button", "", "", "", 60);

        // 来料质检
        long inspection = perm(0, "inspection", "来料质检", "menu", "", "", "Camera", 70);
        perm(inspection, "inspection:detect", "布料检测", "menu", "/inspection/detect", "inspection/InspectionUpload", "Picture", 71);
        perm(inspection, "inspection:review", "质检复核", "menu", "/inspection/review", "inspection/InspectionReview", "Checked", 72);
        perm(inspection, "inspection:upload", "上传图片", "button", "", "", "", 73);
        perm(inspection, "inspection:detect-btn", "开始检测", "button", "", "", "", 74);
        perm(inspection, "inspection:review-btn", "复核质检", "button", "", "", "", 75);
        perm(inspection, "inspection:record", "质检记录", "menu", "/inspection/records", "inspection/InspectionRecordList", "DocumentChecked", 76);

        // 生产管理
        long production = perm(0, "production", "生产管理", "menu", "", "", "Cpu", 80);
        perm(production, "production:plan", "生产计划", "menu", "/production/plans", "production/ProductionPlanList", "Calendar", 81);
        perm(production, "production:order", "生产工单", "menu", "/production/orders", "production/WorkOrderList", "Notebook", 82);
        perm(production, "production:issue", "领料管理", "menu", "/production/issues", "production/MaterialIssue", "TakeawayBox", 83);
        perm(production, "production:progress", "生产进度", "menu", "/production/progress", "production/ProductionProgress", "TrendCharts", 84);
        perm(production, "production:qc", "成品质检", "menu", "/production/qc", "production/FinishedInspection", "Select", 85);
        perm(production, "production:plan:read", "查看计划", "button", "", "", "", 86);
        perm(production, "production:plan:create", "创建计划", "button", "", "", "", 87);
        perm(production, "production:plan:edit", "编辑计划", "button", "", "", "", 88);
        perm(production, "production:order:read", "查看工单", "button", "", "", "", 89);
        perm(production, "production:order:create", "创建工单", "button", "", "", "", 90);
        perm(production, "production:issue-material", "领料", "button", "", "", "", 91);
        perm(production, "production:progress-write", "登记进度", "button", "", "", "", 92);
        perm(production, "production:qc-write", "成品质检", "button", "", "", "", 93);
        perm(production, "production:qc-review", "返工/复检", "button", "", "", "", 94);
        perm(production, "production:plan:release", "下达生产计划", "button", "", "", "", 95);
        perm(production, "production:order:start", "开始生产", "button", "", "", "", 96);
        perm(production, "production:order:report", "生产报工", "button", "", "", "", 97);
        perm(production, "production:order:cancel", "取消工单", "button", "", "", "", 98);

        // 仓储管理
        long warehouse = perm(0, "warehouse", "仓储管理", "menu", "", "", "Shop", 100);
        perm(warehouse, "warehouse:inventory", "库存查询", "menu", "/warehouse/inventory", "warehouse/InventoryList", "List", 101);
        perm(warehouse, "warehouse:inbound", "入库管理", "menu", "/warehouse/inbound", "warehouse/InboundManage", "Download", 102);
        perm(warehouse, "warehouse:outbound", "出库管理", "menu", "/warehouse/outbound", "warehouse/OutboundManage", "Upload", 103);
        perm(warehouse, "warehouse:transaction", "库存流水", "menu", "/warehouse/transactions", "warehouse/InventoryTransaction", "DataLine", 104);
        perm(warehouse, "inventory:read", "查看库存", "button", "", "", "", 105);
        perm(warehouse, "inventory:inbound-material", "原材料入库", "button", "", "", "", 106);
        perm(warehouse, "inventory:issue-material", "领料出库", "button", "", "", "", 107);
        perm(warehouse, "inventory:inbound-finished", "成品入库", "button", "", "", "", 108);
        perm(warehouse, "inventory:transaction-read", "查看流水", "button", "", "", "", 109);

        // 销售管理
        long sales = perm(0, "sales", "销售管理", "menu", "", "", "Sell", 120);
        perm(sales, "sales:order", "销售订单", "menu", "/sales/orders", "sales/SalesOrderList", "Document", 121);
        perm(sales, "sales:shipment", "发货管理", "menu", "/sales/shipments", "sales/ShipmentManage", "Ship", 122);
        perm(sales, "sales:aftersale", "售后管理", "menu", "/sales/aftersales", "sales/AfterSalesManage", "Service", 123);
        perm(sales, "sales:read", "查看销售订单", "button", "", "", "", 124);
        perm(sales, "sales:create", "创建销售订单", "button", "", "", "", 125);
        perm(sales, "sales:edit", "编辑销售订单", "button", "", "", "", 126);
        perm(sales, "sales:ship", "发货", "button", "", "", "", 127);
        perm(sales, "after-sales:read", "查看售后", "button", "", "", "", 128);
        perm(sales, "after-sales:handle", "处理售后", "button", "", "", "", 129);

        // 追溯统计
        long trace = perm(0, "traceability", "追溯统计", "menu", "", "", "Connection", 140);
        perm(trace, "traceability:forward", "正向追溯", "menu", "/traceability/forward", "traceability/ForwardTrace", "Right", 141);
        perm(trace, "traceability:reverse", "反向追溯", "menu", "/traceability/reverse", "traceability/ReverseTrace", "Back", 142);
        perm(trace, "traceability:statistics", "统计分析", "menu", "/traceability/statistics", "traceability/StatisticsView", "DataAnalysis", 143);
        perm(trace, "traceability:read", "查看追溯", "button", "", "", "", 144);
        perm(trace, "statistics:read", "查看统计", "button", "", "", "", 145);

        Integer permissionCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_permission", Integer.class);
        log.info("初始权限创建完成（共 {} 条）", permissionCount);
    }

    private long perm(long parentId, String code, String name, String type,
                      String path, String component, String icon, int sortOrder) {
        jdbc.update(
            "INSERT INTO sys_permission (parent_id, perm_code, perm_name, type, path, component, icon, sort_order) " +
            "VALUES (?,?,?,?,?,?,?,?)",
            parentId, code, name, type, path, component, icon, sortOrder
        );
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    // ========================================
    // 用户
    // ========================================
    private void initUsers() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM sys_user", Long.class);
        if (count != null && count > 0) return;

        Long adminId = getRoleId("ADMIN");
        Long purchaserId = getRoleId("PURCHASER");
        Long qcId = getRoleId("PRODUCTION_QC");
        Long warehouseId = getRoleId("WAREHOUSE_SALES");

        createUser("admin", "admin123", "系统管理员", adminId);
        createUser("purchaser", "123456", "采购员张三", purchaserId);
        createUser("qc", "123456", "质检员李四", qcId);
        createUser("warehouse", "123456", "仓管员王五", warehouseId);

        log.info("测试用户创建完成: admin/admin123, purchaser/123456, qc/123456, warehouse/123456");
    }

    private Long getRoleId(String code) {
        return jdbc.queryForObject("SELECT id FROM sys_role WHERE role_code = ?", Long.class, code);
    }

    private void createUser(String username, String password, String realName, Long roleId) {
        jdbc.update("INSERT INTO sys_user (username, password, real_name, status) VALUES (?,?,?,?)",
                username, passwordEncoder.encode(password), realName, 1);
        Long userId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?,?)", userId, roleId);
    }

    // ========================================
    // 角色权限（精确列表模式）
    // ========================================
    private void initRolePermissions() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM sys_role_permission", Long.class);
        if (count != null && count > 0) return;

        Long adminId = getRoleId("ADMIN");
        Long purchaserId = getRoleId("PURCHASER");
        Long qcId = getRoleId("PRODUCTION_QC");
        Long warehouseId = getRoleId("WAREHOUSE_SALES");

        // 系统管理员：全部权限
        assignAll(adminId);

        // 采购人员
        assign(purchaserId,
                "dashboard",
                "procurement", "procurement:order", "procurement:arrival", "procurement:batch",
                "purchase:read", "purchase:create", "purchase:edit", "purchase:submit",
                "arrival:read", "arrival:register",
                "batch:read",
                "basic:supplier", "basic:material",
                "supplier:read", "material:read",
                "inspection", "inspection:record",
                "traceability", "traceability:forward", "traceability:reverse", "traceability:statistics",
                "traceability:read", "statistics:read"
        );

        // 生产与质检人员
        assign(qcId,
                "dashboard",
                "inspection", "inspection:detect", "inspection:review",
                "inspection:upload", "inspection:detect-btn", "inspection:review-btn",
                "inspection:record",
                "production", "production:plan", "production:order", "production:issue",
                "production:progress", "production:qc",
                "production:plan:read", "production:plan:create", "production:plan:edit",
                "production:order:read", "production:order:create",
                "production:issue-material",
                "production:progress-write",
                "production:qc-write", "production:qc-review",
                "production:plan:release", "production:order:start",
                "production:order:report", "production:order:cancel",
                "basic:supplier", "basic:material", "basic:product", "basic:sku",
                "supplier:read", "material:read", "product:read", "sku:read",
                "procurement:batch", "batch:read",
                "warehouse:inventory", "inventory:read",
                "traceability", "traceability:forward", "traceability:reverse", "traceability:statistics",
                "traceability:read", "statistics:read"
        );

        // 仓储与销售人员
        assign(warehouseId,
                "dashboard",
                "warehouse", "warehouse:inventory", "warehouse:inbound",
                "warehouse:outbound", "warehouse:transaction",
                "inventory:read", "inventory:inbound-material", "inventory:issue-material",
                "inventory:inbound-finished", "inventory:transaction-read",
                "sales", "sales:order", "sales:shipment", "sales:aftersale",
                "sales:read", "sales:create", "sales:edit", "sales:ship",
                "after-sales:read", "after-sales:handle",
                "basic:supplier", "basic:material", "basic:product", "basic:sku", "basic:warehouse",
                "supplier:read", "material:read", "product:read", "sku:read", "warehouse:read",
                "procurement:batch", "batch:read",
                "traceability", "traceability:forward", "traceability:reverse", "traceability:statistics",
                "traceability:read", "statistics:read"
        );

        log.info("角色权限分配完成（精确列表模式）");
    }

    private void assignAll(Long roleId) {
        jdbc.update("INSERT INTO sys_role_permission (role_id, permission_id) SELECT ?, id FROM sys_permission", roleId);
    }

    private void assign(Long roleId, String... codes) {
        for (String code : codes) {
            jdbc.update("INSERT INTO sys_role_permission (role_id, permission_id) " +
                    "SELECT ?, id FROM sys_permission WHERE perm_code = ?", roleId, code);
        }
    }
}