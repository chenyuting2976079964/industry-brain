"""Import auxiliary data CSVs into MySQL: region, industrial_park, industry_chain, patent, user, enterprise_tag."""
import pymysql
import csv
import os

DB_CONFIG = {
    'host': 'localhost',
    'port': 3307,
    'user': 'root',
    'password': 'root123',
    'database': 'industry_brain',
}

DATA_DIR = r'D:\work2\industry-brain-date'


def _to_int(v):
    v = (v or '').strip()
    return int(v) if v and v.isdigit() else None


def _to_bool(v):
    v = (v or '').strip()
    return int(v) if v and v.isdigit() else 0


def _or_none(v, maxlen=None):
    v = (v or '').strip()
    if not v or v.lower() in ('null', 'nan'):
        return None
    return v[:maxlen] if maxlen and len(v) > maxlen else v


# park_level 数据库 CHECK 约束仅允许：国家级/省级/市级/区级
def _map_park_level(v):
    v = (v or '').strip()
    if not v:
        return None
    if '国家级' in v:
        return '国家级'
    if '省级' in v:
        return '省级'
    if '市级' in v:
        return '市级'
    if '区级' in v:
        return '区级'
    return None


# patent_type 数据库 CHECK 约束仅允许：发明/实用新型/外观设计
def _map_patent_type(v):
    v = (v or '').strip()
    if not v:
        return None
    if '实用新型' in v:
        return '实用新型'
    if '外观' in v:
        return '外观设计'
    if '发明' in v:
        return '发明'
    return v


# role_code 数据库 CHECK 约束仅允许：ADMIN/OPERATOR/VIEWER
_ROLE_MAP = {'ADMIN': 'ADMIN', 'OPERATOR': 'OPERATOR', 'VIEWER': 'VIEWER', 'VISITOR': 'VIEWER'}



def import_region(cur):
    path = os.path.join(DATA_DIR, 'region.csv')
    rows = []
    with open(path, 'r', encoding='utf-8-sig') as f:
        reader = csv.reader(f)
        next(reader)
        for row in reader:
            row = row + [''] * (8 - len(row))
            rows.append((
                _to_int(row[0]), _or_none(row[1], 100), _or_none(row[2], 50),
                _to_int(row[3]), _to_bool(row[4]), _or_none(row[5]),
                _or_none(row[6]), _or_none(row[7]),
            ))
    sql = ("INSERT IGNORE INTO region (id, name, code, parent_id, is_deleted, deleted_at, created_at, updated_at) "
           "VALUES (%s, %s, %s, %s, %s, %s, %s, %s)")
    _exec_batches(cur, sql, rows, 'region')


def import_industrial_park(cur):
    path = os.path.join(DATA_DIR, 'industrial_park.csv')
    rows = []
    with open(path, 'r', encoding='utf-8-sig') as f:
        reader = csv.reader(f)
        next(reader)
        for row in reader:
            row = row + [''] * (17 - len(row))
            rows.append((
                _to_int(row[0]), _or_none(row[1], 128), _to_int(row[2]),
                _map_park_level(row[3]), _or_none(row[4], 255), _or_none(row[5]),
                _or_none(row[6], 30), _to_int(row[7]), _to_int(row[8]), _to_int(row[9]),
                _or_none(row[10], 500), _or_none(row[11], 500), _or_none(row[12], 500),
                _to_bool(row[13]), _or_none(row[14]), _or_none(row[15]), _or_none(row[16]),
            ))
    sql = ("INSERT IGNORE INTO industrial_park (id, name, region_id, park_level, address, description, park_type, "
           "enterprise_count, patent_count, high_value_patent_count, dominant_industries, key_firms, chain_coverage, "
           "is_deleted, deleted_at, created_at, updated_at) "
           "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)")
    _exec_batches(cur, sql, rows, 'industrial_park')


def import_industry_chain(cur):
    path = os.path.join(DATA_DIR, 'industry_chain.csv')
    rows = []
    with open(path, 'r', encoding='utf-8-sig') as f:
        reader = csv.reader(f)
        next(reader)
        for row in reader:
            row = row + [''] * (7 - len(row))
            rows.append((
                _to_int(row[0]), _or_none(row[1], 100), _or_none(row[2], 50),
                _to_bool(row[3]), _or_none(row[4]), _or_none(row[5]), _or_none(row[6]),
            ))
    sql = ("INSERT IGNORE INTO industry_chain (id, name, code, is_deleted, deleted_at, created_at, updated_at) "
           "VALUES (%s, %s, %s, %s, %s, %s, %s)")
    _exec_batches(cur, sql, rows, 'industry_chain')


def import_patent(cur):
    path = os.path.join(DATA_DIR, 'patent.csv')
    rows = []
    with open(path, 'r', encoding='utf-8-sig') as f:
        reader = csv.reader(f)
        next(reader)
        for row in reader:
            row = row + [''] * (18 - len(row))
            aid = row[4].strip()
            applicant_eid = int(aid) if aid.isdigit() else None
            rows.append((
                _to_int(row[0]), _or_none(row[1], 50), _or_none(row[2], 500),
                _or_none(row[3], 200), applicant_eid, _or_none(row[5], 50),
                _map_patent_type(row[6]), _or_none(row[7]), _or_none(row[8], 30),
                _or_none(row[9]), _or_none(row[10], 50), _to_int(row[11]),
                _or_none(row[12], 20), None,  # region_id 悬空引用 → NULL
                _to_bool(row[14]), _or_none(row[15]), _or_none(row[16]), _or_none(row[17]),
            ))
    sql = ("INSERT IGNORE INTO patent (id, patent_number, patent_name, applicant, applicant_enterprise_id, ipc_main, "
           "patent_type, application_date, legal_status, abstract_text, estimated_value, maintenance_years, "
           "data_source, region_id, is_deleted, deleted_at, created_at, updated_at) "
           "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)")
    _exec_batches(cur, sql, rows, 'patent')


def import_user(cur):
    path = os.path.join(DATA_DIR, 'user.csv')
    rows = []
    with open(path, 'r', encoding='utf-8-sig') as f:
        reader = csv.reader(f)
        next(reader)
        for row in reader:
            row = row + [''] * (16 - len(row))
            rid = row[6].strip()
            region_id = int(rid) if rid.isdigit() and int(rid) >= 100000 else None  # 101-103 悬空 → NULL
            role = _ROLE_MAP.get(row[7].strip().upper(), 'VIEWER')
            status = _to_bool(row[8])
            if status not in (0, 1):
                status = 1
            rows.append((
                _to_int(row[0]), _or_none(row[1], 50), _or_none(row[2], 128),
                _or_none(row[3], 50), _or_none(row[4], 100), _or_none(row[5], 20),
                region_id, role, status, _to_int(row[9]),
                _or_none(row[10]), _or_none(row[11]), _to_bool(row[12]), _or_none(row[13]),
                _or_none(row[14]), _or_none(row[15]),
            ))
    sql = ("INSERT IGNORE INTO user (id, username, password, real_name, email, phone, region_id, role_code, status, "
           "login_fail_count, lock_until, password_updated_at, is_deleted, deleted_at, created_at, updated_at) "
           "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)")
    _exec_batches(cur, sql, rows, 'user')


def import_enterprise_tag(cur):
    path = os.path.join(DATA_DIR, 'enterprise_tag.csv')
    rows = []
    with open(path, 'r', encoding='utf-8-sig') as f:
        reader = csv.reader(f)
        next(reader)  # header is wrong (3 cols); actual data is 4 cols
        for row in reader:
            # Actual layout: (seq, enterprise_id, tag_name, created_at)
            row = row + [''] * (4 - len(row))
            eid = row[1].strip()
            if not eid.isdigit():
                continue
            rows.append((int(eid), _or_none(row[2], 100), _or_none(row[3])))
    sql = "INSERT IGNORE INTO enterprise_tag (enterprise_id, tag_name, created_at) VALUES (%s, %s, %s)"
    _exec_batches(cur, sql, rows, 'enterprise_tag')


def _exec_batches(cur, sql, rows, name, batch_size=500):
    total = len(rows)
    print(f'[INFO] {name}: {total:,} rows...', end=' ', flush=True)
    for i in range(0, total, batch_size):
        cur.executemany(sql, rows[i:i + batch_size])
        cur.connection.commit()
    print('done')


def main():
    conn = pymysql.connect(**DB_CONFIG)
    cur = conn.cursor()
    cur.execute('SET FOREIGN_KEY_CHECKS = 0')
    try:
        import_region(cur)
        import_industrial_park(cur)
        import_industry_chain(cur)
        import_patent(cur)
        import_user(cur)
        import_enterprise_tag(cur)
    finally:
        cur.execute('SET FOREIGN_KEY_CHECKS = 1')
        conn.commit()

    # Verification
    for t in ['region', 'industrial_park', 'industry_chain', 'patent', 'user', 'enterprise_tag']:
        cur.execute(f'SELECT COUNT(*) FROM {t}')
        print(f'[VERIFY] {t}: {cur.fetchone()[0]:,}')

    cur.close()
    conn.close()
    print('[DONE] All auxiliary data imported.')


if __name__ == '__main__':
    main()
