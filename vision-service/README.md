# 布料瑕疵检测服务 (Vision Service)

基于 YOLO 的布料来料瑕疵检测服务。

## 环境准备

```bash
python -m venv venv
# Windows
venv\Scripts\activate
# Linux/Mac
source venv/bin/activate

pip install -r requirements.txt
```

## 启动服务

```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

## 接口

### 健康检查
```
GET /health
```

### 布料瑕疵检测
```
POST /api/v1/detect
Content-Type: multipart/form-data

参数:
- file: 布料图片 (JPEG/PNG/BMP/WebP, ≤20MB)

返回:
- success: 是否成功
- status: 模型状态 (model_not_configured | ready)
- detections: 检测到的缺陷列表
```

## 模型配置

1. 将 YOLO 模型权重文件放入 `models/` 目录
2. 修改 `app/main.py` 中的 `MODEL_AVAILABLE = True`
3. 实现模型加载和推理逻辑

## 注意事项

- 此服务仅负责图像推理，不直接操作业务数据库
- 没有可用模型权重时，明确返回"未配置"状态，不伪造检测结果
- 业务批次关联由 Spring Boot 后端管理