"""Import enterprise CSV data into MySQL using iterative single-row inserts."""
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

CSV_FILES = [
    r'D:\work2\industry-brain-date\enterprise1.csv',
    r'D:\work2\industry-brain-date\enterprise2.csv',
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
                        vals.append(raw[:64] if raw else None)
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
