# -*- coding: utf-8 -*-
"""企开得胜 - 下载 bge-small-zh-v1.5 模型（ModelScope, 国内直连）。

用法:  C:\\Python\\Python313\\python.exe tools/download_embedding_model.py
产物:  tools/models/bge-small-zh-v1.5/  （约 91MB, 不入库）
"""
import os
import time
import urllib.request

BASE = "https://modelscope.cn/models/BAAI/bge-small-zh-v1.5/resolve/master/{}"
DEST = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models", "bge-small-zh-v1.5")
FILES = [
    "pytorch_model.bin",
    "config.json",
    "tokenizer.json",
    "tokenizer_config.json",
    "vocab.txt",
    "special_tokens_map.json",
    "modules.json",
]


def main():
    os.makedirs(DEST, exist_ok=True)
    for name in FILES:
        dst = os.path.join(DEST, name)
        if os.path.exists(dst) and os.path.getsize(dst) > 1024:
            print(f"skip  {name} (已存在)")
            continue
        t0 = time.time()
        print(f"down  {name} ...", flush=True)
        urllib.request.urlretrieve(BASE.format(name), dst)
        print(f"ok    {name:26s} {os.path.getsize(dst)/1048576:8.2f} MB ({time.time()-t0:.1f}s)")
    print("完成: " + DEST)


if __name__ == "__main__":
    main()
