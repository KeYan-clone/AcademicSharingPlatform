"""
AI 服务主入口（精简版）
- 支持知识库文件上传入向量 (/embedding/upload)
- 支持知识库问答 (/kb/qa)
- 支持删除知识库向量与存储 (/kb/{kb_id})
- 支持可选的存储路径配置与查询
"""
import os
import json
from typing import List, Optional
from fastapi import FastAPI, File, UploadFile, HTTPException, Form, Body, Request
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from services.doc_parser import DocumentParser
from services.rag import RAGService
from services.storage import StorageService
from utils.logger import api_logger as logger


app = FastAPI(title="AI Knowledge Service")

# 配置 CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 初始化基础服务（公共 / 默认库）
storage_service = StorageService()
doc_parser = DocumentParser()
rag_service = RAGService(storage_service)

# 为不同 user_id 做隔离
user_kb_services: dict[str, tuple[StorageService, RAGService]] = {}

# 用户知识库路径选择持久化
user_kb_settings: dict[str, list[str]] = {}
KB_SETTINGS_FILE = "user_kb_settings.json"


def load_user_kb_settings():
    """从文件加载用户知识库设置"""
    global user_kb_settings
    try:
        if os.path.exists(KB_SETTINGS_FILE):
            with open(KB_SETTINGS_FILE, 'r', encoding='utf-8') as f:
                user_kb_settings = json.load(f)
                logger.info(f"Loaded user KB settings from {KB_SETTINGS_FILE}")
    except Exception as e:
        logger.warning(f"Failed to load user KB settings: {e}")
        user_kb_settings = {}


def save_user_kb_settings():
    """保存用户知识库设置到文件"""
    try:
        with open(KB_SETTINGS_FILE, 'w', encoding='utf-8') as f:
            json.dump(user_kb_settings, f, ensure_ascii=False, indent=2)
        logger.info(f"Saved user KB settings to {KB_SETTINGS_FILE}")
    except Exception as e:
        logger.error(f"Failed to save user KB settings: {e}")


load_user_kb_settings()


def get_services(user_id: str | None):
    """根据 user_id 返回对应的 (StorageService, RAGService)"""
    if not user_id:
        return storage_service, rag_service
    if user_id not in user_kb_services:
        ss = StorageService(user_id=user_id)
        if user_id in user_kb_settings:
            saved_paths = user_kb_settings[user_id]
            ss.set_base_paths(saved_paths)
            logger.info(f"恢复用户 {user_id} 的知识库设置: {saved_paths}")
        rs = RAGService(ss)
        user_kb_services[user_id] = (ss, rs)
        logger.info(f"用户 {user_id} 的服务实例已创建，当前知识库: {ss.get_base_paths()}")
    return user_kb_services[user_id]


# --------- 模型定义 ---------
class PaperQARequest(BaseModel):
    question: str
    top_k: Optional[int] = 5
    kb_id: Optional[str] = None


class BasePathRequest(BaseModel):
    base_path: str


# --------- 存储路径配置（可选） ---------
@app.post("/storage/base_path")
async def set_storage_base_path(request: BasePathRequest):
    try:
        storage_service.set_base_path(request.base_path)
        rag_service.reload_vector_db()
        return {"status": "success", "message": "Base path updated"}
    except Exception as e:
        logger.error(f"Error setting base path: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/embedding/base_path")
async def set_storage_base_path_alt(request: BasePathRequest):
    return await set_storage_base_path(request)


@app.post("/storage/base_path/reset")
async def reset_storage_base_path():
    try:
        default_path = os.path.abspath(".")
        storage_service.set_base_path(default_path)
        rag_service.reload_vector_db()
        return {"status": "success", "message": "Base path reset to default", "base_path": default_path}
    except Exception as e:
        logger.error(f"Error resetting base path: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/embedding/base_path/reset")
async def reset_storage_base_path_alt():
    return await reset_storage_base_path()


@app.get("/storage/list")
async def get_storage_base_paths(request: Request):
    user_id = request.query_params.get("user_id")
    ss, _ = get_services(user_id)
    return ss.get_base_paths()


@app.post("/storage/selected")
async def set_storage_selected_paths(paths: List[str] = Body(...), request: Request = None):
    user_id = request.query_params.get("user_id") if request else None
    ss, rs = get_services(user_id)
    try:
        ss.set_base_paths(paths)
        rs.reload_vector_db()
        if user_id:
            user_kb_settings[user_id] = paths.copy()
            save_user_kb_settings()
        current_paths = ss.get_base_paths()
        return {"status": "success", "paths": current_paths}
    except Exception as e:
        logger.error(f"Error setting selected KB paths for {user_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/storage/document_exists")
async def check_document_exists(filename: str, request: Request, course_id: Optional[str] = None):
    user_id = request.query_params.get("user_id")
    ss, _ = get_services(user_id)
    try:
        exists = ss.document_exists(filename, course_id)
        return {"exists": exists}
    except Exception as e:
        logger.error(f"Error checking document exists ({user_id}): {e}")
        raise HTTPException(status_code=500, detail=str(e))


# --------- 上传入库 ---------
@app.post("/embedding/upload")
async def upload_file(
    request: Request,
    file: UploadFile = File(...),
    course_id: Optional[str] = Form(None),
    original_filename: Optional[str] = Form(None)
):
    """上传文件并入库向量（按 kb_id 隔离）"""
    user_id = request.query_params.get("user_id")
    kb_id = request.query_params.get("kb_id")
    ss, rs = get_services(user_id)
    try:
        if ss.document_exists(file.filename, course_id):
            raise HTTPException(status_code=400, detail="文件已存在于知识库中")

        temp_path = os.path.join(ss.get_temp_dir(), file.filename)
        content = await file.read()
        with open(temp_path, "wb") as f:
            f.write(content)

        try:
            doc_path = ss.save_document(temp_path, course_id, kb_id)
            logger.info(f"Saved document to {doc_path}")

            chunks = doc_parser.parse_file(doc_path)
            display_name = original_filename or file.filename
            if display_name:
                base_name = os.path.basename(display_name)
                for c in chunks:
                    src = c.get("source", "")
                    if " " in src:
                        suffix = src[src.find(" "):]
                        c["source"] = f"{base_name}{suffix}"
                    elif src:
                        c["source"] = base_name
                    else:
                        c["source"] = base_name
            if kb_id:
                for c in chunks:
                    c["source"] = f"{kb_id}:{c.get('source','')}"
            logger.info(f"Successfully parsed file {file.filename} into {len(chunks)} chunks")

            rs.add_to_knowledge_base(chunks)
            logger.info(f"Successfully added {len(chunks)} chunks to knowledge base")

            return {
                "status": "success",
                "message": f"文件 {file.filename} 处理成功",
                "chunks_count": len(chunks),
                "file_path": doc_path
            }
        finally:
            if os.path.exists(temp_path):
                os.remove(temp_path)

    except Exception as e:
        logger.error(f"Error processing file {file.filename}: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


# --------- 删除知识库（向量 + 存储） ---------
@app.delete("/kb/{kb_id}")
async def delete_kb(kb_id: str, request: Request):
    user_id = request.query_params.get("user_id")
    _ss, rs = get_services(user_id)
    try:
        removed = rs.delete_kb(kb_id)
        return {"status": "success", "removed": removed}
    except Exception as e:
        logger.error(f"Error deleting kb {kb_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# --------- 知识库问答 ---------
@app.post("/kb/qa")
async def kb_question_answer(request: Request, body: PaperQARequest):
    user_id = request.query_params.get("user_id")
    _ss, rs = get_services(user_id)
    try:
        result = rs.answer_question(body.question, body.top_k or 5, body.kb_id)
        return result
    except Exception as e:
        logger.error(f"Paper QA error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# --------- 健康检查 ---------
@app.get("/health")
async def health_check():
    return {"status": "healthy"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
