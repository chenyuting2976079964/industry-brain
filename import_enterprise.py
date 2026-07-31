"""Import enterprise CSV data into MySQL using iterative single-row inserts."""
import pymysql
import csv
import os
import re
from datetime import date, datetime

DB_CONFIG = {
    'host': 'localhost',
    'port': 3307,
    'user': 'root',
    'password': 'root',
    'database': 'industry_brain',
    'charset': 'utf8mb4',
}

CSV_FILES = [
    r'C:\Users\17855\Desktop\产业大脑\数据\industry-brain-date\industry-brain-date\enterprise1.csv',
    r'C:\Users\17855\Desktop\产业大脑\数据\industry-brain-date\industry-brain-date\enterprise2.csv',
]

# CSV columns in order
COLS = ['id', 'external_id', 'name', 'description', 'unified_code',
        'legal_representative', 'registered_capital', 'establishment_date',
        'business_status', 'registered_address', 'contact_phone', 'region_id',
        'park_id', 'enterprise_type', 'employee_scale', 'is_local',
        'data_version', 'is_deleted', 'deleted_at', 'created_at', 'updated_at']

MAX_LEN = {'name': 200, 'unified_code': 18, 'legal_representative': 50,
           'registered_capital': 50, 'business_status': 20,
           'registered_address': 255, 'contact_phone': 50,
           'external_id': 50, 'enterprise_type': 64, 'employee_scale': 30}

COL_NAMES = ','.join(COLS)
PH = ','.join(['%s'] * 21)
# Use INSERT IGNORE — skip duplicates, auto-inc unsupplied ids
SQL = f'INSERT IGNORE INTO enterprise ({COL_NAMES}) VALUES ({PH})'


def get_capital_wan(cap_text: str | None) -> float | None:
    """
    提取注册资本数值，统一转换为万元。
    支持格式：
      "1000万元人民币", "1.5亿元", "500万美元", "1000万港元",
      "1000万元", "1亿元", "1000", "1000万"
    无法提取数字返回 None
    """
    if not cap_text:
        return None
    text = str(cap_text).strip()
    if not text:
        return None

    # 提取数字
    m = re.search(r"[0-9.]+", text)
    if not m:
        return None
    num = float(m.group())

    # 识别单位并换算
    if "亿" in text:
        num *= 10000  # 亿 → 万
    # 万美元/万港元/万美元 已经是"万"为单位，无需再乘
    # "万" 本身已在单位中，乘以系数即可
    if "美元" in text or "美金" in text:
        num *= 7.0  # 1 USD ≈ 7 RMB
    elif "港元" in text or "港币" in text:
        num *= 0.9  # 1 HKD ≈ 0.9 RMB

    return round(num, 2)


def calc_enterprise_type(enterprise: dict) -> str | None:
    """
    根据企业信息自动计算企业类型标签。

    :param enterprise: 包含 name, description, establishment_date,
                       employee_scale, registered_capital 的字典
    :return: '高新技术企业' | '专精特新' | '科技型中小企业' | '龙头企业' | None
    """
    name = (enterprise.get("name") or "").strip()
    desc = (enterprise.get("description") or "").strip()
    estab_raw = enterprise.get("establishment_date")
    emp_scale = (enterprise.get("employee_scale") or "").strip()
    cap_raw = enterprise.get("registered_capital")

    # 解析成立日期
    estab_date = None
    if estab_raw:
        if isinstance(estab_raw, date):
            estab_date = estab_raw
        elif isinstance(estab_raw, datetime):
            estab_date = estab_raw.date()
        elif isinstance(estab_raw, str):
            raw_str = estab_raw.strip()[:10]
            if raw_str:
                try:
                    estab_date = datetime.strptime(raw_str, "%Y-%m-%d").date()
                except ValueError:
                    pass

    cap_wan = get_capital_wan(cap_raw)

    # ===================== 1. 高新技术企业 =====================
    name_tech_keys = ("科技", "技术")
    desc_rd_keys = ("研发", "技术开发", "技术服务", "技术转让", "技术推广", "知识产权", "高新产品")
    cond1_name = any(k in name for k in name_tech_keys)
    cond1_desc = any(k in desc for k in desc_rd_keys)
    if cond1_name and cond1_desc:
        return "高新技术企业"

    # ===================== 2. 专精特新 =====================
    spec_keys = ("专精特新", "小巨人", "专精")
    cond2_keyword = any(k in name for k in spec_keys) or any(k in desc for k in spec_keys)
    cond2_date = False
    if estab_date:
        three_year_day = date(estab_date.year + 3, estab_date.month, estab_date.day)
        cond2_date = date.today() >= three_year_day
    if cond2_keyword and cond2_date:
        return "专精特新"

    # ===================== 3. 科技型中小企业 =====================
    tech_pos_keys = ("科技", "软件", "信息", "网络", "数字", "智能", "数据",
                     "生物", "芯片", "电子", "新材料", "新能源")
    black_keys = ("商贸", "置业", "建设", "地产", "物流", "运输")
    cond3_pos = any(k in name for k in tech_pos_keys)
    cond3_black = any(k in name for k in black_keys)
    cond3_scale = emp_scale in ("中小型", "小型")
    if cond3_pos and (not cond3_black) and cond3_scale:
        return "科技型中小企业"

    # ===================== 4. 龙头企业 =====================
    # 前置条件：不属于以上三类科技企业
    leader_name_keys = ("集团", "控股", "产业集团", "城投", "产投", "国资")
    cond4_name = any(k in name for k in leader_name_keys)
    cond4_cap = cap_wan is not None and cap_wan >= 10000  # ≥1亿元(10000万元)
    cond4_scale = emp_scale in ("大型", "超大型")
    if (cond4_name and cond4_cap) or cond4_scale:
        return "龙头企业"

    # ===================== 5. 兜底 =====================
    return None


def main():
    conn = pymysql.connect(**DB_CONFIG)
    cur = conn.cursor()
    cur.execute('SET FOREIGN_KEY_CHECKS = 0')
    cur.execute('TRUNCATE TABLE enterprise')
    conn.commit()

    total = 0
    batch = []

    for csv_file in CSV_FILES:
        if not os.path.exists(csv_file):
            print(f'[ERROR] Not found: {csv_file}')
            continue
        print(f'[INFO] {os.path.basename(csv_file)}...', end=' ', flush=True)

        with open(csv_file, 'r', encoding='utf-8-sig') as f:
            reader = csv.reader(f)
            next(reader)

            for row in reader:
                row = row + [''] * (21 - len(row))

                # 构建企业字典供自动分类使用
                ent = {
                    'name': (row[2] or '').strip(),
                    'description': (row[3] or '').strip(),
                    'registered_capital': (row[6] or '').strip(),
                    'establishment_date': (row[7] or '').strip()[:10],
                    'employee_scale': (row[14] or '').strip(),
                }
                auto_type = calc_enterprise_type(ent)

                vals = []

                for i, col in enumerate(COLS):
                    raw = row[i].strip() if i < len(row) else ''

                    if col == 'id':
                        # Use id if present and numeric, else None (auto_inc)
                        vals.append(int(raw) if raw and raw.isdigit() else None)
                    elif col in ('region_id', 'park_id'):
                        vals.append(int(raw) if raw and raw.isdigit() else None)
                    elif col in ('is_local', 'is_deleted'):
                        vals.append(int(raw) if raw and raw.isdigit() else 0)
                    elif col == 'name':
                        vals.append(raw[:200] if raw else '')
                    elif col == 'enterprise_type':
                        # 始终由算法自动分类，忽略CSV中的原始值
                        vals.append(auto_type[:64] if auto_type else None)
                    elif col in MAX_LEN:
                        vals.append(raw[:MAX_LEN[col]] if raw else None)
                    elif col in ('establishment_date',):
                        d = raw[:10] if raw else None
                        vals.append(d)
                    elif col in ('data_version', 'deleted_at', 'created_at', 'updated_at'):
                        vals.append(raw if raw else None)
                    else:
                        vals.append(raw if raw else None)

                batch.append(tuple(vals))
                total += 1

                if len(batch) >= 200:
                    cur.executemany(SQL, batch)
                    conn.commit()
                    batch = []

                if total % 20000 == 0:
                    print(f'{total//1000}k...', end='', flush=True)

    if batch:
        cur.executemany(SQL, batch)
        conn.commit()

    print(f'\n[DONE] Total: {total:,}')

    cur.execute('SET FOREIGN_KEY_CHECKS = 1')
    cur.execute('SELECT COUNT(*), MIN(id), MAX(id) FROM enterprise')
    r = cur.fetchone()
    print(f'[VERIFY] count={r[0]:,}, min_id={r[1]}, max_id={r[2]}')

    cur.execute('SELECT enterprise_type, COUNT(*) FROM enterprise GROUP BY enterprise_type ORDER BY COUNT(*) DESC')
    for t, c in cur.fetchall():
        print(f'  {t or "(NULL)"}: {c:,}')

    cur.close()
    conn.close()


if __name__ == '__main__':
    main()
