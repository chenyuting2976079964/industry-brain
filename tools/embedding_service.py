# -*- coding: utf-8 -*-
"""
企开得胜 - 本地语义向量服务（OpenAI 兼容, :8701）
====================================================
模型: BAAI/bge-small-zh-v1.5 (512 维, CLS 池化 + L2 归一化, 与 ModelScope 官方
     sentence-transformers 打包一致)。纯本地 CPU 推理, 不出网。

接口 (与 OpenAI /embeddings 兼容, 后端 EmbeddingClient 直接调用):
    POST /embeddings      {"input": "文本" | ["t1","t2"], "model": 可选}
    POST /v1/embeddings   同上（两种路径都支持）
    GET  /health          {"status":"ok","model":...,"dim":512}

模型文件（不入库, 约 91MB）:
    tools/models/bge-small-zh-v1.5/{pytorch_model.bin, tokenizer.json, ...}
缺失时从 ModelScope 一键下载（国内直连）:
    python tools/download_embedding_model.py
    （或手动: https://modelscope.cn/models/BAAI/bge-small-zh-v1.5）

启动（生产约定, cwd=D:\\unibrain）:
    C:\\Python\\Python313\\python.exe tools/embedding_service.py
    端口可用环境变量 EMBED_PORT 覆盖（默认 8701）。
"""
import os
import threading
import time

import torch
import uvicorn
from fastapi import FastAPI
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from transformers import AutoModel, AutoTokenizer

MODEL_NAME = "BAAI/bge-small-zh-v1.5"
MODEL_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models", "bge-small-zh-v1.5")
DIM = 512
MAX_LEN = 512
PORT = int(os.environ.get("EMBED_PORT", "8701"))
HOST = os.environ.get("EMBED_HOST", "127.0.0.1")

print(f"[embedding] 加载模型 {MODEL_DIR} ...", flush=True)
_t0 = time.time()
tokenizer = AutoTokenizer.from_pretrained(MODEL_DIR, local_files_only=True)
model = AutoModel.from_pretrained(MODEL_DIR, local_files_only=True)
model.eval()
model.to("cpu")
print(f"[embedding] 模型就绪 ({time.time()-_t0:.1f}s), dim={DIM}", flush=True)

_lock = threading.Lock()
app = FastAPI(title="企开得胜 本地向量服务")


class EmbedRequest(BaseModel):
    input: str | list
    model: str | None = None
    encoding_format: str | None = None


def _embed(texts: list[str]) -> list[list[float]]:
    """CLS 池化 + L2 归一化（与官方 sentence-transformers 实现一致）。"""
    with _lock:
        inp = tokenizer(
            texts,
            padding=True,
            truncation=True,
            max_length=MAX_LEN,
            return_tensors="pt",
        )
        with torch.no_grad():
            out = model(**inp)
        # transformers v5 输出属性改名, 兼容两者
        hidden = getattr(out, "last_hidden_states", None)
        if hidden is None:
            hidden = out.last_hidden_state
        vecs = hidden[:, 0, :]  # CLS 池化
        vecs = vecs / vecs.norm(dim=1, keepdim=True)
        return vecs.cpu().numpy().astype("float32").tolist()


@app.post("/embeddings")
@app.post("/v1/embeddings")
def embeddings(req: EmbedRequest):
    texts = [req.input] if isinstance(req.input, str) else [str(t) for t in req.input]
    if not texts:
        return JSONResponse({"error": {"message": "empty input"}}, status_code=400)
    texts = [t[:2000] for t in texts]  # 单条长度保护
    vecs = _embed(texts)
    return {
        "object": "list",
        "data": [
            {"object": "embedding", "index": i, "embedding": v}
            for i, v in enumerate(vecs)
        ],
        "model": MODEL_NAME,
        "usage": {"prompt_tokens": sum(len(t) for t in texts), "total_tokens": sum(len(t) for t in texts)},
    }


@app.get("/health")
def health():
    return {"status": "ok", "model": MODEL_NAME, "dim": DIM}


if __name__ == "__main__":
    print(f"[embedding] 监听 http://{HOST}:{PORT}/embeddings", flush=True)
    uvicorn.run(app, host=HOST, port=PORT, log_level="warning")
