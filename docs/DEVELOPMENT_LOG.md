# DEVELOPMENT_LOG.md

> 本文档用于记录毕业设计项目的实际开发进度，帮助开发者和 Claude Code 在中断后恢复工作。

---

## 一、项目基本信息

- **项目名称：** 集成机器视觉的服装产品数字化追溯管理系统设计与实现
- **项目阶段：** 开发中
- **最近更新时间：** 2026-10-22
- **当前开发任务：** 阶段 1-2 已完成，阶段 3 待开始
- **总体完成度：** 六个业务模块已全部实现，共 35 张表，92 个权限，4 个角色，89 个 API 接口；下一步为系统测试、部署文档和 YOLO 模型接入

## 二、整体开发进度

| 序号 | 开发模块 | 状态 | 备注 |
|---|---|---|---|
| 0 | 项目骨架与环境验证 | ✅ 已完成 | 三端骨架建立，环境验证通过 |
| 1 | 系统管理与基础档案 | ✅ 已完成 | 用户/角色/权限 + 供应商/原材料/产品/SKU/仓库 |
| 2 | 采购与来料质检 | ✅ 已完成 | 采购订单、到货登记、原材料批次、图片上传、视觉检测、人工复核 |
| 3 | 生产与质量管理 | ✅ 已完成 | 生产计划、工单、领料、进度、成品质检、返工/报废、成品批次 |
| 4 | 仓储与库存管理 | ✅ 已完成 | 原材料/成品入库、领料出库、销售出库、库存余额与流水 |
| 5 | 销售与售后管理 | ✅ 已完成 | 销售订单、发货、退货/售后处理 |
| 6 | 生命周期追溯与统计分析 | ✅ 已完成 | 原材料批次正向追溯、成品批次反向追溯、工作台统计 |
| 7 | 系统测试与部署 | 🔜 下一步 | 功能测试、异常测试、部署说明 |
| 8 | YOLO 模型接入 | 暂缓 | 用户自行训练模型，最后阶段接入 |

## 三、当前项目状态

### 3.1 已完成的工作

- [x] 项目目录及基础配置
- [x] 前端工程初始化（Vue 3 + Element Plus + Vite）
- [x] 后端工程初始化（Spring Boot 3.3 + MyBatis-Plus + JWT）
- [x] 数据库连接与初始化（35 张表）
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
- 后端：可编译启动（mvn spring-boot:run），89 个 API 接口可用
- 数据库：35 张表已创建，4 角色/92 权限/4 用户已初始化
- 机器视觉服务：已安装 FastAPI/Uvicorn 最小依赖并可启动；当前无模型权重，`/detect` 返回 `model_not_configured`，不伪造检测结果

### 3.3 测试账号

| 用户名 | 密码 | 角色 | 权限数 |
|--------|------|------|--------|
| admin | admin123 | 系统管理员 | 92（全部） |
| purchaser | 123456 | 采购人员 | 24 |
| qc | 123456 | 生产与质检人员 | 45 |
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

### 3.6 修改密码功能（2026-10-22）

- 后端新增 `PUT /api/auth/password`：校验当前密码；新密码 6-128 位；不允许与当前密码相同。
- 前端右上角“修改密码”弹窗：当前密码、新密码、确认新密码；修改成功后提示并退出登录。
- 操作日志 AOP 跳过 `ServletRequest` / `ServletResponse` 参数，修复摘要显示“参数序列化失败”的问题。

验证结果：

| 测试 | 结果 |
|---|---|
| 当前密码错误 | 400 |
| 新密码过短 | 400 |
| 正确修改密码 | 200 |
| 旧密码登录 | 失败 |
| 新密码登录 | 成功 |
| 操作日志摘要 | `oldPassword` / `newPassword` 均为 `***` |

### 3.7 采购管理模块（2026-10-22）

本阶段只实现采购三块，来料质检留到下一模块。

**数据库（4 张新表）**
- `purchase_order`、`purchase_order_item`、`arrival_record`、`material_batch`
- 新建库执行更新后的 `database/schema.sql`；已有库执行 `database/migrations/2026-10-22_procurement.sql`

**后端**
- 采购订单：列表、详情、新增、编辑（仅草稿）、提交、取消
- 到货登记：按采购明细登记，生成 `PENDING_INSPECTION` 批次，事务内更新已到货数量和订单状态
- 原材料批次：列表、详情
- 编号规则：`PO/ARR/MB-YYYYMMDD-序号`
- 到货使用 `SELECT ... FOR UPDATE` 锁定明细，防止并发超收

**前端**
- 替换占位页：`PurchaseOrderList.vue`、`ArrivalManage.vue`、`MaterialBatchList.vue`
- 新增 API：`purchase.js`、`arrival.js`、`batch.js`
- 状态标签统一放在 `utils/constants.js`

**测试结果**

| 测试 | 结果 |
|---|---|
| 后端编译 / 前端构建 | 通过 |
| 新建采购订单 | 200，生成 `PO-20261022-001`，状态 `DRAFT` |
| 提交订单 | 200，状态 `SUBMITTED` |
| 到货 40 | 200，生成 `ARR-20261022-001` / `MB-20261022-001`，订单 `PARTIAL_RECEIVED` |
| 超量到货 100（剩余 60） | 400，提示不能超过剩余数量 |
| 再到货 60 | 200，生成 `MB-20261022-002`，订单 `COMPLETED` |
| 批次初始状态 | `PENDING_INSPECTION`，可用数量 0 |
| 编辑已提交订单 | 400，被拒绝 |
| 取消已完成订单 | 400，被拒绝 |
| 仓储人员读取采购订单 | 403 |
| 仓储人员读取批次 | 200 |
| 采购人员数量为 0 新建 | 400，被拒绝 |

**测试数据：** 本地库保留了 `PO-20261022-001` 及其两条到货、两个批次，可直接用于页面演示。

### 3.8 来料质检模块（2026-10-22）

**数据库（3 张新表）**
- `inspection_image`、`vision_detection_result`、`inspection_record`
- 已有库执行 `database/migrations/2026-10-22_inspection.sql`

**后端**
- 图片上传：格式/大小校验、`ImageIO` 解析、UUID 文件名、本地目录存储
- 检测：调用 Python 视觉服务，保存模型信息、检测框 JSON、耗时、错误信息
- 复核：正式结论与检测结果分开保存，更新批次状态
- 静态资源：`/uploads/**` 映射本地上传目录
- 视觉服务不可用时保存 `SERVICE_UNAVAILABLE`，不生成虚假检测框
- 补充 `@RequestBody` 参数校验异常处理，统一返回 400

**前端**
- 替换占位页：`InspectionUpload.vue`、`InspectionReview.vue`
- 新增 API：`api/inspection.js`
- 检测结果按 `SUCCESS / MODEL_NOT_CONFIGURED / SERVICE_UNAVAILABLE / FAILED` 分类展示
- 复核页支持合格/不合格/暂停使用，以及不合格处理方式

**测试结果**

| 测试 | 结果 |
|---|---|
| 上传 PNG 图片 | 200，保存 20×20 图片 |
| 图片访问 `/uploads/**` | 200（后端直连与前端代理均通过） |
| 视觉服务未启动时检测 | 200，状态 `SERVICE_UNAVAILABLE`，检测框为空 |
| 缺失 imageId | 400，提示“请选择要检测的图片” |
| 视觉服务启动后检测 | 200，状态 `MODEL_NOT_CONFIGURED`，批次进入 `PENDING_REVIEW` |
| 复核合格 | 200，批次变为 `QUALIFIED` |
| 不合格未填处理方式 | 400，被拒绝 |
| 不合格并选择退货 | 200，批次变为 `UNQUALIFIED` |
| 仓储人员上传图片 | 403 |
| 操作日志 | 图片上传/检测/复核均有记录 |

**测试数据：**
- `MB-20261022-001`：QUALIFIED
- `MB-20261022-002`：UNQUALIFIED（处理方式 RETURN）
- `MB-20261022-003`：PENDING_INSPECTION（30 米，供页面演示）

**说明：** 视觉服务当前只安装了 FastAPI/Uvicorn/python-multipart 最小依赖，没有模型权重；`/detect` 明确返回 `model_not_configured`。

### 3.9 质检记录查询页（2026-10-22）

定位为**只读查询页**，复用现有 `inspection_record`、`vision_detection_result`、`inspection_image`、`material_batch`，不新增业务表。

- 菜单：来料质检 → 质检记录 → `/inspection/records`
- 权限：`inspection:record`，分配给 ADMIN、PRODUCTION_QC、PURCHASER
  - 采购人员同时补充“来料质检”父菜单授权，否则菜单树无法显示该页面
- 接口：`GET /api/inspection-records`，支持批次号、结论、日期范围分页查询
- 页面：列表 + 详情抽屉（复核信息、批次信息、图片、机器检测结果）
- 增量脚本：`database/migrations/2026-10-22_inspection-record.sql`
- `DataInitializer` 同步新权限，保证新建数据库初始化一致

验证结果：

| 测试 | 结果 |
|---|---|
| 质检人员查询记录 | 200，返回 2 条历史复核记录 |
| 按结论筛选（QUALIFIED） | 200，返回 1 条 |
| 按日期范围筛选 | 200，返回 2 条 |
| 采购人员查询记录 | 200 |
| 采购人员查看批次质检详情 | 200（详情接口已允许 `inspection:record`） |
| 仓储人员查询记录 | 403 |
| 管理员菜单/权限 | 包含 `inspection:record` |
| 前端构建 | 通过 |

**注意：** 已登录用户需要刷新页面或重新登录，才能看到新增的“质检记录”菜单。

### 3.10 删除布料图片（2026-10-22）

- 新增 `DELETE /api/material-batches/{id}/inspection-images/{imageId}`
- 仅批次处于待检/检测中/待复核/暂停使用时可删除
- 图片已被检测结果或复核记录引用时禁止删除
- 删除数据库记录的同时清理本地文件；文件删除失败只记录日志，不影响业务记录
- 前端批次图片区域增加“删除”按钮，已检测的图片不显示删除入口
- 删除不存在的静态图片返回 404，而不是 500

验证结果：

| 测试 | 结果 |
|---|---|
| 删除未被引用的图片 | 200，数据库记录和文件均删除 |
| 删除已检测图片 | 400，提示已被引用 |
| 删除已完成质检批次的图片 | 400，提示已完成质检 |
| 仓储人员删除图片 | 403 |
| 删除后的图片地址 | 404 |

### 3.11 生产与成品质检（2026-10-22）

- 新增 9 张表：生产计划、工单、领料单/明细、生产进度、成品质检、不合格品处理、成品批次、成品与原材料批次关联
- 前端替换 5 个占位页：生产计划、生产工单、领料管理、生产进度、成品质检
- 生产计划：草稿 → 下达 → 生产中 → 完成，可取消
- 工单：待执行 → 生产中 → 待质检/返工 → 完成
- 领料：按原材料批次登记，调用库存服务扣减并生成流水
- 成品质检：送检/合格/不合格数量校验，不合格必须处理；合格数量生成成品批次并关联使用的原材料批次

测试结果：

| 测试 | 结果 |
|---|---|
| 新建/下达计划、生成工单、开始生产 | 通过 |
| 领料 5 米（合格批次） | 通过，生成领料单 |
| 领料不合格批次 | 400 拒绝 |
| 超批次数量领料 | 400 拒绝 |
| 登记进度 10/10 | 通过，完成率 100% |
| 成品质检 10/9/1 | 通过，结果 PARTIAL，工单返工中 |
| 不合格处理（返工） | 通过 |
| 生成成品批次 | `FB-20261022-001`，数量 9，待入库 |

### 3.12 库存、销售与售后（2026-10-22）

- 新增 2 张库存表、5 张销售/售后表
- 原材料合格入库：生成库存余额和 `MATERIAL_INBOUND` 流水，批次转为已入库
- 成品入库：生成成品库存和 `FINISHED_INBOUND` 流水，批次转为可售
- 生产领料：扣减原材料库存并生成 `MATERIAL_ISSUE` 流水
- 销售发货：按成品批次扣减库存并生成 `SALES_OUTBOUND` 流水，更新订单发货状态
- 退货/售后：只记录状态和处理结论，不自动恢复可售库存
- 前端替换 4 个仓储页面和 3 个销售页面

测试结果：

| 测试 | 结果 |
|---|---|
| 原材料入库（批次 1） | 通过，入库 35 米（扣除历史领料 5 米） |
| 重复入库 | 400 拒绝 |
| 领料 10 米 | 通过，原材料库存 35 → 25 |
| 成品入库（批次 1） | 通过，成品库存 9 |
| 创建销售订单并发货 5 件 | 通过，成品库存 9 → 4 |
| 重复/超量发货 | 400 拒绝 |
| 新增售后并处理完成 | 通过，状态 COMPLETED |
| 库存流水 | 入库/领料/成品入库/销售出库 4 类流水完整 |

### 3.13 追溯与统计（2026-10-22）

- 正向追溯：原材料批次 → 采购/到货 → 质检 → 领料/工单 → 成品批次 → 发货 → 售后
- 反向追溯：成品批次 → 工单/成品质检 → 原材料批次及来料质检 → 发货/售后
- 统计：采购、待检、生产中工单、待入库、原材料/成品库存、销售、售后待处理
- 质量统计：批次状态分布、复核合格率
- 工作台改为读取真实统计数据，不再显示占位“—”
- 前端替换 3 个追溯/统计页面

测试结果：

| 测试 | 结果 |
|---|---|
| 正向追溯 `MB-20261022-001` | 返回采购、质检、工单、成品批次、发货、售后 |
| 反向追溯 `FB-20261022-001` | 返回工单、原材料批次、发货、售后 |
| 统计概览 | 采购 2、待检 1、生产中 1、原材料库存 25、成品库存 4、销售 1 |
| 质量统计 | 复核 2 条，合格率 50% |

### 3.14 生产流程企业化改造（2026-10-22）

修复内容：

1. **计划下达自动生成工单**：下达时可按数量拆分，默认全额；生成 `PENDING` 待生产工单。
2. **计划状态自动汇总**：工单开工/报工/质检/取消后，计划自动变为已下达/生产中/已完成；仍存在未生成数量时不会误判完成。
3. **报工完成自动转待质检**：累计完成数量达到计划数量时，工单自动转为 `PENDING_QC`。
4. **成品质检数量闭环**：累计送检小于完成数量时保持待质检；全部送检且无不合格才完成。
5. **返工/报废闭环**：返工、复检、报废处理后重新计算工单状态。
6. **待生产工单取消**：只有 `PENDING` 可取消，取消后计划状态回算，剩余数量可继续追加工单。
7. **状态轨迹**：新增 `production_status_log`，记录计划/工单每次状态变化、操作人和说明。
8. **权限细化**：新增下达计划、开始生产、生产报工、取消工单 4 个权限。

前端交互：

- 计划页：下达/追加工单弹窗、已生成/剩余数量、查看工单、状态轨迹。
- 工单页：待生产/生产中/待质检/返工中/已完成页签；进度条；已检/待检数量；按状态显示开始、报工、去质检、取消。
- 进度页：支持从工单页跳转并自动打开；报工完成提示转入待质检。
- 质检页：新增“待质检任务”页签；支持从工单页跳转直接质检；限制待送检数量。

测试结果：

| 测试 | 结果 |
|---|---|
| 计划 20，分两次下达 12 + 8 | 生成 2 张 `PENDING` 工单，计划 `RELEASED`，剩余 0 |
| 再次下达 | 400 拒绝 |
| 工单开始生产 | 工单/计划均进入 `IN_PROGRESS` |
| 报工 8/8 | 工单自动 `PENDING_QC` |
| 送检 4/8 全合格 | 工单保持 `PENDING_QC` |
| 再送检 4/4 全合格 | 工单 `COMPLETED` |
| 取消另一张未开工工单 | 计划回到 `IN_PROGRESS`，剩余 12，不误判完成 |
| 追加工单 12 并完成 | 计划最终 `COMPLETED`，已生成数量 20，剩余 0 |
| 状态轨迹 | 计划/工单均记录完整流转 |
| 权限 | 共 92 个权限，质检角色 45 个 |

### 3.15 成品批次半自动生成（2026-10-22）

采用“质检合格后生成待办，用户确认后生成批次”的方式，避免质检与批次职责混在一起。

- 工单新增 `batchedQuantity`、`pendingBatchQuantity` 计算字段
- 成品质检页新增“待生成批次”页签
- 质检提交后，如仍有合格数量未生成批次，自动提示并切换到待生成批次页签
- 生成批次弹窗支持：本次数量、生产日期、备注
- 后端校验本次数量不能超过剩余合格数量
- 一个工单可分多次生成成品批次
- 原材料使用量按本次批次数量占合格数量的比例分摊到 `finished_batch_material`
- `finished_batch` 新增 `remark` 字段

测试结果：

| 测试 | 结果 |
|---|---|
| 工单合格 8，生成 5 | 批次 `FB-...-002`，待生成 3 |
| 再生成 10 | 400，提示不能超过剩余 3 |
| 再生成 3 | 批次 `FB-...-003`，待生成 0 |
| 批次备注 | 正确保存 |
| 待生成批次字段 | 工单列表/详情返回正确 |

### 3.16 生产进度工序下拉（2026-10-22）

- 将自由文本“工序/说明”改为“当前工序”下拉选择：
  - 裁剪 CUTTING、缝制 SEWING、整烫 IRONING、包装 PACKING、其他 OTHER
- 进度登记时必选当前工序；进度百分比仍按累计完成数量自动计算
- 生产工单列表新增“当前工序”列，取最近一次进度记录的工序
- 工单详情和进度记录中的工序统一显示中文
- 历史自由文本记录仍按原值显示，不做数据迁移

验证：

| 测试 | 结果 |
|---|---|
| 工单列表返回 `currentProcess` | 通过，如 `CUTTING` |
| 进度记录工序显示 | 通过，代码映射为中文 |
| 前端构建 | 通过 |

### 3.17 领料前置与生产阶段展示（2026-10-22）

业务规则：

1. **未领料不能开工**：工单开始生产前必须至少存在一张领料单，否则后端返回“请先登记领料，再开始生产”。
2. **领料与工单关联**：
   - 工单列表显示“已领料/未领料”；
   - 未领料工单提供“去领料”入口，跳转到领料管理并自动选中该工单；
   - 领料管理支持按工单筛选，并显示“仅看当前工单”提示。
3. **生产阶段展示**：
   - 生产进度页新增“当前工序”列；
   - 工单列表“当前阶段”按状态显示：未开工、当前工序、完工待检、返工中、已完成、已取消；
   - 生产完成时，“当前工序”记录实际最后一道工序（通常为包装），工单状态自动变为“完工待检”。
4. **进度登记提示**：最后一批报工时选择最后一道实际工序，系统自动转待质检。

说明：当前系统没有 BOM/用料定额，因此只校验“至少领过料”，不校验领料数量是否覆盖理论用量。

测试结果：

| 测试 | 结果 |
|---|---|
| 未领料直接开工 | 400，提示先领料 |
| 登记领料后开工 | 200 |
| 领料按工单筛选 | 返回该工单 1 条记录 |
| 报工 5/5，工序包装 | 工单转 `PENDING_QC`，当前工序 `PACKING` |
| 工单列表领料状态/当前阶段 | 正确返回 |

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

## 五、数据库表清单（35 张）

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
| purchase_order | 采购订单 | 阶段 3 |
| purchase_order_item | 采购订单明细 | 阶段 3 |
| arrival_record | 到货登记 | 阶段 3 |
| material_batch | 原材料批次 | 阶段 3 |
| inspection_image | 布料检测图片 | 阶段 3 |
| vision_detection_result | 机器视觉检测结果 | 阶段 3 |
| inspection_record | 人工质检复核记录 | 阶段 3 |
| production_plan | 生产计划 | 阶段 4 |
| work_order | 生产工单 | 阶段 4 |
| material_issue | 领料单 | 阶段 4 |
| material_issue_item | 领料明细 | 阶段 4 |
| production_progress | 生产进度 | 阶段 4 |
| finished_inspection | 成品质检 | 阶段 4 |
| quality_disposition | 不合格品处理 | 阶段 4 |
| finished_batch | 成品批次 | 阶段 4 |
| finished_batch_material | 成品批次与原材料批次关联 | 阶段 4 |
| production_status_log | 生产计划/工单状态流转日志 | 阶段 4 |
| inventory | 库存余额 | 阶段 5 |
| inventory_transaction | 库存流水 | 阶段 5 |
| sales_order | 销售订单 | 阶段 5 |
| sales_order_item | 销售订单明细 | 阶段 5 |
| shipment | 发货单 | 阶段 5 |
| shipment_item | 发货明细 | 阶段 5 |
| after_sales_record | 售后记录 | 阶段 5 |

## 六、API 接口清单（89 个）

### 认证
- POST /api/auth/login — 登录
- POST /api/auth/logout — 登出
- GET /api/auth/me — 当前用户信息（含角色/权限/菜单树）
- PUT /api/auth/password — 修改本人密码（校验当前密码）

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

### 采购管理
- GET/POST /api/purchase-orders — 采购订单列表/新增
- GET/PUT /api/purchase-orders/{id} — 详情/编辑（仅草稿）
- POST /api/purchase-orders/{id}/submit — 提交订单
- POST /api/purchase-orders/{id}/cancel — 取消订单
- POST /api/purchase-orders/{id}/arrivals — 登记到货并生成批次
- GET /api/arrivals — 到货记录列表
- GET /api/material-batches — 原材料批次列表
- GET /api/material-batches/{id} — 批次详情

### 来料质检
- POST /api/material-batches/{id}/inspection-images — 上传布料图片
- DELETE /api/material-batches/{id}/inspection-images/{imageId} — 删除未被引用的图片
- POST /api/material-batches/{id}/detect — 调用视觉服务检测
- GET /api/material-batches/{id}/inspection — 批次质检详情（图片/检测/复核记录）
- POST /api/material-batches/{id}/review — 人工质检复核
- GET /api/inspection-records — 质检记录分页查询（只读）

### 生产与成品质检
- GET/POST /api/production-plans, PUT /{id}
- POST /api/production-plans/{id}/release — 下达/追加工单（可指定数量）
- POST /api/production-plans/{id}/cancel
- GET /api/production-plans/{id}/status-logs — 计划状态轨迹
- GET/POST /api/work-orders, GET /{id}, POST /{id}/start
- POST /api/work-orders/{id}/cancel — 取消待生产工单
- GET /api/work-orders/{id}/status-logs — 工单状态轨迹
- POST /api/work-orders/{id}/material-issues, GET /api/material-issues
- POST /api/work-orders/{id}/progress
- POST /api/work-orders/{id}/finished-inspections, GET /api/finished-inspections
- POST /api/quality-dispositions
- POST /api/work-orders/{id}/finished-batches, GET /api/finished-batches

### 库存
- GET /api/inventory, GET /api/inventory/transactions
- POST /api/inventory/material-inbound, POST /api/inventory/finished-inbound

### 销售与售后
- GET/POST /api/sales-orders, GET /api/sales-orders/{id}, POST /api/sales-orders/{id}/cancel
- POST /api/sales-orders/{id}/shipments, GET /api/shipments
- GET/POST /api/after-sales, POST /api/after-sales/{id}/process

### 追溯与统计
- GET /api/traceability/material-batches/{batchNo}
- GET /api/traceability/finished-batches/{batchNo}
- GET /api/statistics/overview, GET /api/statistics/quality

## 七、下一步开发计划：系统测试、部署与 YOLO 接入

### 功能范围
1. 前后端页面人工验收，修复交互问题
2. 补充核心成功路径、失败路径、越权访问和库存边界测试
3. 整理演示数据、测试账号和操作说明
4. 完善 README、数据库脚本和部署说明
5. 接入真实 YOLO 模型权重，替换 `vision-service/app/main.py` 中的推理 TODO
6. 安装视觉服务完整依赖（opencv、numpy、Pillow 等）并验证真实检测结果

### 重点测试场景
- 未登录、越权访问返回 401/403
- 采购到货不能超收；不合格批次不能入库/领料
- 库存不足时领料/发货失败，库存和流水不产生部分更新
- 成品未质检合格不能生成可售库存
- 退货未验收不自动恢复库存
- 追溯能展示完整链路和所有关联原材料批次
- YOLO 服务不可用、模型未配置、图片损坏时的提示

### 实现顺序
1. 逐页人工验收并修复
2. 补充自动化/接口回归测试
3. 准备演示数据与截图
4. 完善文档与部署说明
5. 接入模型权重并验证真实推理

### 启动命令
```bash
# 后端（JWT_SECRET 必填，至少 32 个字符）
cd backend && set DB_PASSWORD=你的数据库密码 && set JWT_SECRET=你的随机密钥 && mvn spring-boot:run   # Windows CMD
# Git Bash / macOS / Linux：
# DB_PASSWORD=你的数据库密码 JWT_SECRET=$(openssl rand -base64 32) mvn spring-boot:run

# 前端
cd frontend && npm run dev

# 视觉服务（无模型权重时返回 model_not_configured）
cd vision-service && venv/Scripts/python -m uvicorn app.main:app --port 8000
```

---

## 八、已知问题与风险

| 编号 | 问题 | 优先级 | 状态 |
|------|------|--------|------|
| 1 | 历史操作日志中存在明文密码（修复前产生） | 高 | 待用户确认是否清理 |
| 2 | 操作日志 `user_id` 仍为 NULL（principal 为用户名，无法直接解析为 ID） | 低 | 未修复 |
| 3 | 路由守卫只校验登录状态，尚未做菜单/按钮级路由权限校验 | 中 | 未修复 |
| 4 | 工作台快捷入口未按权限过滤 | 低 | 未修复 |
| 6 | 视觉服务无模型权重，检测返回 `model_not_configured` | 中 | 待提供权重 |
| 7 | 自动编号采用“计数+1”，并发或逻辑删除后存在撞号风险 | 低 | 未修复 |
| 8 | 后端无自动化测试（`src/test` 为空） | 中 | 未开始 |
| 9 | 开发演示账号仍使用初始密码（admin/admin123 等） | 中 | 部署前需修改或移除 |
| 10 | 退出登录、重置密码后旧 JWT 仍然有效（无状态令牌，未实现吊销） | 中 | 未修复 |
| 11 | `/uploads/**` 图片地址免登录访问，仅适合内网/演示环境 | 中 | 未修复 |
| 12 | 上传图片仅保存在本地目录，未做容量清理或对象存储 | 低 | 未处理 |
| 13 | 追溯页面使用表格展示，未做时间线/图形化链路 | 低 | 可后续优化 |
| 14 | 库存仅支持整批入库，未实现盘点、调拨、多仓拆分 | 中 | 未实现 |
| 15 | 退货只记录售后状态，不自动恢复可售库存（按设计） | — | 设计如此 |
| 16 | 编号仍为“计数+1”，并发下依赖唯一约束兜底 | 低 | 未修复 |
| 17 | 前端页面未逐页浏览器人工验收 | 中 | 待验收 |

## 九、Claude Code 交接说明

- **当前项目能否运行：** 可以。后端和前端均可正常启动
- **当前所在分支：** main
- **是否存在未提交修改：** 是（修改密码、操作日志、采购、质检、生产、库存、销售、追溯等改动尚未提交）
- **最近一次测试结果：** 核心业务链路全部通过（采购 → 到货 → 质检 → 生产 → 成品批次 → 入库 → 销售发货 → 售后 → 正反向追溯），后端编译与前端构建通过
- **下次开始前必须阅读：**
  1. `CLAUDE.md`
  2. `docs/SYSTEM_DESIGN.md` 第 5 节（采购与来料质检）
  3. `docs/DEVELOPMENT_LOG.md`（本文）
  4. 现有 `pom.xml`、已创建的实体类和服务