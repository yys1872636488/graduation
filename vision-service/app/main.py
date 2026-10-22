"""
服装布料瑕疵检测服务 - YOLO 推理接口

注意：
- 本服务仅负责图像推理，不直接操作业务数据库
- 如果没有可用模型权重，/detect 接口将返回"检测服务未配置"状态
- 业务批次关联、质检结论由 Spring Boot 后端管理
"""
import os
import time
import json
from pathlib import Path

from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse

app = FastAPI(title="Fabric Defect Detection Service", version="0.1.0")

# 配置
UPLOAD_DIR = Path(os.getenv("UPLOAD_DIR", "./uploads"))
RESULT_DIR = Path(os.getenv("RESULT_DIR", "./results"))
MODEL_DIR = Path(os.getenv("MODEL_DIR", "./models"))
MODEL_AVAILABLE = False  # 设为 True 当模型权重文件就绪

# 确保目录存在
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
RESULT_DIR.mkdir(parents=True, exist_ok=True)
MODEL_DIR.mkdir(parents=True, exist_ok=True)


@app.get("/health")
async def health():
    """健康检查"""
    return {
        "status": "ok",
        "model_available": MODEL_AVAILABLE,
        "model_version": "N/A" if not MODEL_AVAILABLE else "v1",
    }


@app.post("/api/v1/detect")
async def detect(file: UploadFile = File(...)):
    """
    布料瑕疵检测接口

    如果模型未配置，返回明确的状态提示，不伪造检测结果。
    """
    # 校验文件类型
    allowed_types = {"image/jpeg", "image/png", "image/bmp", "image/webp"}
    if file.content_type not in allowed_types:
        raise HTTPException(
            status_code=400,
            detail=f"不支持的文件类型: {file.content_type}。支持: JPEG, PNG, BMP, WebP",
        )

    # 读取并保存上传图片
    contents = await file.read()

    # 限制文件大小 (20MB)
    if len(contents) > 20 * 1024 * 1024:
        raise HTTPException(status_code=400, detail="文件大小不能超过 20MB")

    timestamp = int(time.time() * 1000)
    safe_filename = f"{timestamp}_{file.filename}"
    image_path = UPLOAD_DIR / safe_filename
    image_path.write_bytes(contents)

    # 如果没有可用模型，返回"未配置"状态
    if not MODEL_AVAILABLE:
        return JSONResponse(
            status_code=200,
            content={
                "success": True,
                "status": "model_not_configured",
                "message": "检测服务未配置模型权重。当前为演示模式，不产生真实的检测结果。",
                "model_name": None,
                "model_version": None,
                "image_width": None,
                "image_height": None,
                "detections": [],
                "annotated_image_path": None,
                "inference_time_ms": None,
            },
        )

    # TODO: 当模型权重就绪后，在此处实现真实推理
    # 1. 加载模型
    # 2. 预处理图片
    # 3. 执行推理
    # 4. 生成标注图
    # 5. 返回检测结果

    return JSONResponse(
        status_code=200,
        content={
            "success": True,
            "model_name": "fabric-defect-yolo",
            "model_version": "v1",
            "image_width": None,
            "image_height": None,
            "detections": [],
            "annotated_image_path": None,
            "inference_time_ms": None,
        },
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)