# DEVELOPMENT_LOG.md

> 本文档用于记录毕业设计项目的实际开发进度，帮助开发者和 Claude Code 在中断后恢复工作。

---

## 一、项目基本信息

- **项目名称：** 集成机器视觉的服装产品数字化追溯管理系统设计与实现
- **项目阶段：** 开发中
- **最近更新时间：** 2026-10-22
- **当前开发任务：** 阶段 1-2 已完成，阶段 3 待开始
- **总体完成度：** 系统管理 + 基础档案已完成，共 11 张表，87 个权限，4 个角色，35 个 API 接口；基础修复已完成

## 二、整体开发进度

| 序号 | 开发模块 | 状态 | 备注 |
|---|---|---|---|
| 0 | 项目骨架与环境验证 | ✅ 已完成 | 三端骨架建立，环境验证通过 |
| 1 | 系统管理与基础档案 | ✅ 已完成 | 用户/角色/权限 + 供应商/原材料/产品/SKU/仓库 |
| 2 | 采购与来料质检 | 🔜 下一步 | 采购订单、到货登记、布料瑕疵检测 |
| 3 | 生产与质量管理 | 未开始 | 生产工单、领料、进度、成品质检 |
| 4 | 仓储与库存管理 | 未开始 | 入库、出库、库存流水 |
| 5 | 销售与售后管理 | 未开始 | 销售订单、发货、退换货与售后 |
| 6 | 生命周期追溯与统计分析 | 未开始 | 原材料批次至销售售后的关联追溯 |
| 7 | 系统测试与部署 | 未开始 | 功能测试、异常测试、部署说明 |
| 8 | YOLO 模型接入 | 暂缓 | 用户自行训练模型，最后阶段接入 |

## 三、当前项目状态

### 3.1 已完成的工作

- [x] 项目目录及基础配置
- [x] 前端工程初始化（Vue 3 + Element Plus + Vite）
- [x] 后端工程初始化（Spring Boot 3.3 + MyBatis-Plus + JWT）
- [x] 数据库连接与初始化（11 张表）
- [x] Python 视觉服务骨架（MODEL_AVAILABLE=False）
- [x] 登录与权限控制（JWT + Spring Security + @PreAuthorize）
- [x] 用户管理（CRUD + 禁用/启用 + 重置密码 + 修改角色）
- [x] 角色管理（卡片展示 + 抽屉式按模块分组权限分配）
- [x] 操作日志（AOP 自动记录 + 分页查询）
- [x] 供应商/原材料/产品/SKU/仓库 新增/编辑/查询/启停（按设计不提供物理删除）
- [x] 前端设计系统（工业精准风格，CSS 变量 + Element Plus 主题覆盖）
- [x] 自动编号生成（SUP/MAT/PRD/SKU/WH-YYYYMMDD-XXX）
- [x] 时间格式修复（ISO 8601 → yyyy-MM-dd HH:mm:ss）
- [x] 基础修复：操作日志敏感字段脱敏、未认证统一返回 401、刷新页面恢复用户与菜单

### 3.2 当前可运行状态

- 前端：可启动（npm run dev），登录可用，系统管理+基础档案页面可用
- 后端：可编译启动（mvn spring-boot:run），35 个 API 接口可用
- 数据库：11 张表已创建，4 角色/87 权限/4 用户已初始化
- 机器视觉服务：骨架代码已就绪，但当前 venv 未安装依赖，需先执行 `pip install -r requirements.txt`，且无模型权重

### 3.3 测试账号

| 用户名 | 密码 | 角色 | 权限数 |
|--------|------|------|--------|
| admin | admin123 | 系统管理员 | 87（全部） |
| purchaser | 123456 | 采购人员 | 22 |
| qc | 123456 | 生产与质检人员 | 40 |
| warehouse | 123456 | 仓储与销售人员 | 39 |

> 以上账号仅用于开发演示环境，登录页已不再展示默认账号。部署前必须修改初始密码或移除测试账号。

### 3.4 基础修复记录（2026-10-22）

在不新增业务模块的前提下完成以下修复：

1. **操作日志脱敏**（`OperationLogAspect`）
   - 使用 Spring 统一配置的 ObjectMapper，支持时间字段序列化；
   - 递归屏蔽 `password`、`oldPassword`、`newPassword`、`confirmPassword`、`token`、`secret`、`authorization` 字段，写日志前替换为 `***`。
2. **未认证响应统一为 401**（`SecurityConfig`）
   - 未登录/令牌失效返回 401 JSON：`{"code":401,"message":"未登录或登录已过期","data":null}`；
   - 已登录但无权限返回 403 JSON，与前端拦截器逻辑一致。
3. **刷新页面恢复用户信息**（`stores/user.js`、`router/index.js`、`main.js`）
   - 新增 `ensureUserInfo()`；路由守卫在有 token 但 Store 为空时重新请求 `/api/auth/me`；
   - `main.js` 调整为先安装 Pinia 再安装 Router。
4. **版本管理**：完成首次 Git 提交，固化阶段 1-2 成果。

验证结果：

| 测试 | 结果 |
|---|---|
| `mvn -DskipTests compile` | 通过 |
| `npm run build` | 通过 |
| 未登录访问 `/api/auth/me`、`/api/suppliers` | 401，JSON 正确 |
| admin 登录后访问 `/api/auth/me`、`/api/suppliers` | 200 |
| purchaser 访问 `/api/users` | 403，JSON 正确 |
| 登录后的操作日志 | 密码显示为 `***` |

**注意：** 修复前已产生的历史日志中仍存在明文密码（如日志 id 52）。是否清理这些历史记录需用户确认，未自动修改历史数据。

### 3.5 安全加固记录（2026-10-22）

针对代码安全审查发现的问题，处理了以下三项：

1. **用户管理接口越权防护**（`SysUserServiceImpl`）
   - 管理员可管理任意账号，但不能通过用户管理接口操作自己；
   - 非管理员只能管理角色权限严格低于自己的账号，禁止操作管理员账号和同级账号；
   - 创建/编辑用户时，非管理员只能分配自己已拥有的角色，防止提权。
2. **移除 JWT 默认密钥**（`application.yml`、`JwtTokenProvider`）
   - `JWT_SECRET` 改为必填环境变量，不再提供可猜测的默认值；
   - 启动时校验：为空、等于示例值或不足 32 字节时直接启动失败。
3. **登录安全**（`LoginAttemptService`、`AuthController`、`AuthServiceImpl`、`LoginView.vue`）
   - 同一“用户名 + IP”在 15 分钟内失败 5 次后锁定 15 分钟；
   - 登录成功后清除失败计数；
   - 移除登录页“测试账号：admin / admin123”提示。

验证结果：

| 测试 | 结果 |
|---|---|
| `mvn -DskipTests compile` | 通过 |
| `npm run build` | 通过 |
| 未配置 `JWT_SECRET` 启动 | 按预期启动失败并提示配置方式 |
| 连续 5 次错误密码后再次登录 | 返回“登录失败次数过多，请15分钟后再试” |
| 管理员通过用户管理接口重置自己的密码（触发权限守卫） | 403，被拒绝 |
| 管理员正常登录与基础查询 | 正常 |

说明：跨账号越权场景需要临时调整角色权限，本次未执行该测试；守卫逻辑已通过自身账号场景验证。

## 四、设计决策记录

### 权限系统
- 格式：`module:action`（如 `purchase:read`、`inspection:review`）
- 角色权限分配从模糊前缀匹配改为精确 code 列表（DataInitializer.java `assign` 方法）
- "工作台"不纳入权限配置（所有角色通用）
- 后端 @PreAuthorize + 前端 v-if 双重控制

### 业务编号
- 格式：`{前缀}-{日期8位}-{序号3位}`，如 `SUP-20261022-001`
- 后端自动生成，前端不输入
- 创建后不可修改

### 下拉标准化
- 所有分类字段使用 `frontend/src/utils/constants.js` 统一管理
- 原材料类别/颜色/单位、产品品类/款式/季节、SKU 颜色/尺码/单位 均标准化

### 设计风格
- 工业精准风：钢蓝主色 #3d5980，深蓝灰侧栏 #1b1f2d，微冷灰背景 #f2f3f6
- 1px 边框替代阴影，统一 4px 圆角
- PingFang SC 中文字体优先

## 五、数据库表清单（11 张）

| 表名 | 说明 | 阶段 |
|------|------|------|
| sys_user | 系统用户 | 阶段 1 |
| sys_role | 系统角色 | 阶段 1 |
| sys_permission | 权限定义 | 阶段 1 |
| sys_user_role | 用户角色关联 | 阶段 1 |
| sys_role_permission | 角色权限关联 | 阶段 1 |
| sys_operation_log | 操作日志 | 阶段 1 |
| supplier | 供应商 | 阶段 2 |
| material | 原材料主数据 | 阶段 2 |
| product | 服装产品 | 阶段 2 |
| product_sku | 产品 SKU | 阶段 2 |
| warehouse | 仓库 | 阶段 2 |

## 六、API 接口清单（35 个）

### 认证
- POST /api/auth/login — 登录
- POST /api/auth/logout — 登出
- GET /api/auth/me — 当前用户信息（含角色/权限/菜单树）

### 用户管理
- GET/POST /api/users — 列表/新增
- PUT /api/users/{id} — 编辑
- PUT /api/users/{id}/disable — 禁用
- PUT /api/users/{id}/enable — 启用
- PUT /api/users/{id}/reset-password — 重置密码
- GET /api/users/{id}/role-ids — 获取角色ID

### 角色管理
- GET /api/roles — 列表
- GET /api/roles/{id}/permissions — 已有权限
- PUT /api/roles/{id}/permissions — 分配权限
- GET /api/permissions — 所有权限

### 操作日志
- GET /api/operation-logs — 分页查询

### 基础档案
- GET/POST /api/suppliers, PUT /{id}, PUT /{id}/toggle-status
- GET/POST /api/materials, PUT /{id}, PUT /{id}/toggle-status
- GET/POST /api/products, PUT /{id}, PUT /{id}/toggle-status
- GET/POST /api/skus, PUT /{id}, PUT /{id}/toggle-status
- GET/POST /api/warehouses, PUT /{id}, PUT /{id}/toggle-status

## 七、明天开发计划：阶段 3 — 采购与来料质检

### 功能范围
1. 采购订单管理（创建/提交/状态流转/查看）
2. 到货登记（关联采购订单，生成原材料批次）
3. 原材料批次管理（状态：待检→检测中→待复核→合格/不合格）
4. 布料图片上传与存储
5. YOLO 检测接口调用（Python 服务对接）
6. 人工质检复核（检测结果与正式结论分开保存）

### 涉及数据库表（7 张新表）
- `purchase_order` — 采购订单主表
- `purchase_order_item` — 采购订单明细
- `arrival_record` — 到货登记
- `material_batch` — 原材料批次（追溯链核心）
- `inspection_image` — 上传图片元数据
- `vision_detection_result` — YOLO 检测结果
- `inspection_record` — 人工质检复核记录

### 涉及前端页面
- 采购订单列表/详情 → `/procurement/orders`
- 到货登记 → `/procurement/arrivals`
- 原材料批次列表 → `/procurement/batches`
- 布料检测（上传+检测+复核） → `/inspection/detect`
- 质检复核 → `/inspection/review`

### 关键业务规则
1. 采购订单状态：DRAFT → SUBMITTED → PARTIAL_RECEIVED → COMPLETED
2. 到货登记不直接增加可用库存，生成待检批次
3. 原材料批次：PENDING_INSPECTION → INSPECTING → PENDING_REVIEW → QUALIFIED/UNQUALIFIED
4. 不合格批次不能入库/领料
5. YOLO 检测结果与人工复核结论分开保存，允许多次检测
6. 用户必须先选择批次再上传图片

### 实现顺序
1. 建表 + 实体/Mapper
2. 采购订单 CRUD + 状态流转
3. 到货登记 + 原材料批次生成
4. 图片上传 + YOLO 检测接口对接
5. 检测结果保存 + 人工复核

### 启动命令
```bash
# 后端（JWT_SECRET 必填，至少 32 个字符）
cd backend && set DB_PASSWORD=你的数据库密码 && set JWT_SECRET=你的随机密钥 && mvn spring-boot:run   # Windows CMD
# Git Bash / macOS / Linux：
# DB_PASSWORD=你的数据库密码 JWT_SECRET=$(openssl rand -base64 32) mvn spring-boot:run

# 前端
cd frontend && npm run dev
```

---

## 八、已知问题与风险

| 编号 | 问题 | 优先级 | 状态 |
|------|------|--------|------|
| 1 | 历史操作日志中存在明文密码（修复前产生） | 高 | 待用户确认是否清理 |
| 2 | 操作日志 `user_id` 仍为 NULL（principal 为用户名，无法直接解析为 ID） | 低 | 未修复 |
| 3 | 路由守卫只校验登录状态，尚未做菜单/按钮级路由权限校验 | 中 | 未修复 |
| 4 | 顶部“修改密码”菜单项尚未实现 | 低 | 未修复 |
| 5 | 工作台统计卡片为占位“—”，快捷入口未按权限过滤 | 低 | 未修复 |
| 6 | 视觉服务依赖未安装（venv 中仅有 pip） | 中 | 待环境准备 |
| 7 | 自动编号采用“计数+1”，并发或逻辑删除后存在撞号风险 | 低 | 未修复 |
| 8 | 后端无自动化测试（`src/test` 为空） | 中 | 未开始 |
| 9 | 开发演示账号仍使用初始密码（admin/admin123 等） | 中 | 部署前需修改或移除 |
| 10 | 退出登录、重置密码后旧 JWT 仍然有效（无状态令牌，未实现吊销） | 中 | 未修复 |

## 九、Claude Code 交接说明

- **当前项目能否运行：** 可以。后端和前端均可正常启动
- **当前所在分支：** main
- **是否存在未提交修改：** 否（已完成首次提交；后续修改需继续提交）
- **最近一次测试结果：** 基础修复验证通过（编译、前端构建、401/200/403 响应、日志密码脱敏）
- **下次开始前必须阅读：**
  1. `CLAUDE.md`
  2. `docs/SYSTEM_DESIGN.md` 第 5 节（采购与来料质检）
  3. `docs/DEVELOPMENT_LOG.md`（本文）
  4. 现有 `pom.xml`、已创建的实体类和服务