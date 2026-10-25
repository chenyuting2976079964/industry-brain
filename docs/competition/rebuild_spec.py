"""按现存 docx 反向重建 supporting-materials-spec.json（块级结构 + 图片映射 + 题注）。"""
import json, re, sys
from docx import Document
from docx.oxml.ns import qn
from docx.oxml.table import CT_Tbl
from docx.oxml.text.paragraph import CT_P
from docx.table import Table
from docx.text.paragraph import Paragraph

DOCX = "D:/unibrain/docs/competition/企开得胜-参赛作品支撑材料.docx"
OUT = "D:/unibrain/docs/competition/supporting-materials-spec.json"

doc = Document(DOCX)
body = doc.element.body
blocks = []
img_no = 0
for child in body.iterchildren():
    if isinstance(child, CT_P):
        p = Paragraph(child, doc)
        blips = p._p.findall('.//' + qn('a:blip'))
        if blips:
            img_no += 1
            blocks.append({"type": "image", "seq": img_no, "caption_after": None})
            continue
        t = p.text.strip()
        if not t:
            continue
        style = p.style.name if p.style else ""
        m = re.search(r"(\d)", style)
        lvl = int(m.group(1)) if ("Heading" in style and m) else (1 if "标题 1" in style else 2)
        blocks.append({"type": f"h{min(lvl,3)}", "text": t})
    elif isinstance(child, CT_Tbl):
        tb = Table(child, doc)
        blocks.append({"type": "table", "rows": [[c.text.strip() for c in r.cells] for r in tb.rows]})
    else:
        continue

for i, b in enumerate(blocks):
    if b.get("type") == "image":
        for j in range(i + 1, min(i + 3, len(blocks))):
            nxt = blocks[j]
            if nxt.get("type") == "p" and nxt["text"].startswith("图"):
                b["caption_after"] = nxt["text"]
                break

spec = {
    "work": "企开得胜 - 专利驱动的产业大脑与精准招商平台",
    "note": "10-25 事故后按现存 docx 反向重建；图片为 10-25 新品牌重截版本（架构图 + 8 张 UI）",
    "images": {
        "1": "docs/competition/product-architecture.png",
        "2": "docs/competition/tech-architecture.png",
        "3": "docs/competition/screenshots/01-login.png",
        "4": "docs/competition/screenshots/02-home.png",
        "5": "docs/competition/screenshots/03-industry.png",
        "6": "docs/competition/screenshots/04-diagnosis.png",
        "7": "docs/competition/screenshots/05-investment.png",
        "8": "docs/competition/screenshots/06-enterprise.png",
        "9": "docs/competition/screenshots/07-reports.png",
        "10": "docs/competition/screenshots/08-dashboard-full.png",
    },
    "blocks": blocks,
}
json.dump(spec, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print("spec rebuilt:", len(blocks), "blocks;", img_no, "images")
for b in blocks:
    if b.get("type") == "image":
        print(f"  img{b['seq']}: {spec['images'].get(str(b['seq']))} | {b.get('caption_after')}")
