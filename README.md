# 集成机器视觉的服装产品数字化追溯管理系统

面向服装企业的内部业务管理系统，以原材料批次为起点，串联采购、来料质检（YOLO布料瑕疵检测）、生产、成品质检、仓储、销售、售后，支持按批次正向/反向追溯。

## 技术栈

| 层次 | 技术 |
|------|------|
| 前端 | Vue 3 + Vite + Vue Router + Pinia + Element Plus + Axios |
| 业务后端 | Java 17 + Spring Boot 3.3 + Spring Security + MyBatis-Plus |
| 数据库 | MySQL 8.0 |
| 机器视觉 | Python 3 + FastAPI + YOLO（仅用于布料来料图片检测） |

## 项目结构

```
├── frontend/                 # Vue 3 前端
│   └── src/
│       ├── api/              # Axios 请求封装
│       ├── assets/           # 静态资源
│       ├── components/       # 通用组件
│       ├── layouts/          # 后台统一布局
│       ├── router/           # 路由与守卫
│       ├── stores/           # Pinia 状态管理
│       ├── utils/            # 工具函数
│       └── views/            # 页面组件
├── backend/                  # Spring Boot 业务后端
│   └── src/main/java/org/example/graduation/
│       ├── common/           # 统一响应、异常、分页
│       ├── config/           # 配置类
│       ├── security/         # 认证授权
│       ├── controller/       # 控制器
│       ├── service/          # 业务接口
│       ├── service/impl/     # 业务实现
│       ├── mapper/           # MyBatis 映射
│       ├── entity/           # 实体类
│       ├── dto/              # 数据传输对象
│       └── vo/               # 视图对象
├── vision-service/           # Python YOLO 推理服务
│   ├── app/                  # FastAPI 应用
│   ├── models/               # 模型权重（不提交到 Git）
│   ├── uploads/              # 上传图片
│   └── requirements.txt
├── database/                 # 数据库脚本
│   └── schema.sql
└── docs/                     # 项目文档
    ├── SYSTEM_DESIGN.md      # 系统设计总纲
    └── DEVELOPMENT_LOG.md    # 开发日志
```

## 快速启动

### 1. 数据库

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS graduation_db DEFAULT CHARSET utf8mb4;
-- 导入表结构
-- mysql -u root -p graduation_db < database/schema.sql
```

### 2. 后端

```bash
cd backend
# 数据库密码与 JWT 密钥通过环境变量提供（JWT_SECRET 必填，至少 32 个字符）
export DB_PASSWORD=你的数据库密码
export JWT_SECRET=$(openssl rand -base64 32)
mvn spring-boot:run
```

> `JWT_SECRET` 未配置或长度不足时后端会启动失败，这是为避免使用可猜测的默认密钥。
> 生产部署请通过环境变量或密钥管理服务提供，不要写入代码或提交到 Git。

### 3. 前端

```bash
cd frontend
npm install
npm run dev
```

### 4. 视觉服务

```bash
cd vision-service
python -m venv venv
venv\Scripts\activate    # Windows
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

## 四类角色

- **系统管理员**：账号、角色、权限、基础配置
- **采购人员**：采购订单、到货登记、原材料批次
- **生产与质检人员**：生产执行、来料检验(YOLO)、成品质检
- **仓储与销售人员**：出入库、销售发货、售后

## 开发顺序

按阶段小步迭代，详见 `docs/SYSTEM_DESIGN.md` 第 10 节。

## 注意事项

- 机器视觉仅用于布料来料图片检测，无模型权重时不得伪造结果
- 库存变化必须在数据库事务中完成并生成流水
- 被业务记录引用的数据禁止物理删除
