"""Import MySQL data into Elasticsearch: enterprise + patent indices.

Creates indices if missing, then bulk-inserts rows from MySQL.
Uses only stdlib (urllib) + pymysql to avoid extra deps.
"""
import pymysql
import json
import urllib.request

MYSQL = dict(host='localhost', port=3307, user='root', password='root123', database='industry_brain')
ES = 'http://localhost:9200'

ENTERPRISE_INDEX = 'industry_brain_enterprise'
PATENT_INDEX = 'industry_brain_patent'


def es_req(method, path, body=None):
    url = ES + path
    data = json.dumps(body).encode('utf-8') if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header('Content-Type', 'application/json')
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            raw = resp.read().decode('utf-8')
            return json.loads(raw) if raw.strip() else {}
    except urllib.error.HTTPError as e:
        raw = e.read().decode('utf-8')
        return json.loads(raw) if raw.strip() else {'status': e.code}


def ensure_index(name, mapping):
    exists = es_req('HEAD', f'/{name}')
    if not exists:
        es_req('PUT', f'/{name}', {'mappings': mapping})
        print(f'[ES] 索引已创建: {name}')
    else:
        print(f'[ES] 索引已存在: {name}')


ENTERPRISE_MAPPING = {
    'properties': {
        'enterpriseId': {'type': 'long'},
        'name': {'type': 'text', 'analyzer': 'ik_max_word'},
        'businessScope': {'type': 'text', 'analyzer': 'ik_max_word'},
        'regionId': {'type': 'long'},
        'isLocal': {'type': 'boolean'},
        'enterpriseType': {'type': 'keyword'},
        'legalRepresentative': {'type': 'keyword'},
        'registeredAddress': {'type': 'keyword'},
    }
}

PATENT_MAPPING = {
    'properties': {
        'patentId': {'type': 'long'},
        'patentName': {'type': 'text', 'analyzer': 'ik_max_word'},
        'abstractText': {'type': 'text', 'analyzer': 'ik_max_word'},
        'applicant': {'type': 'keyword'},
        'applicant_enterprise_id': {'type': 'long'},
        'ipcMain': {'type': 'keyword'},
        'patentType': {'type': 'keyword'},
        'applicationDate': {'type': 'date'},
        'legalStatus': {'type': 'keyword'},
    }
}


def bulk_insert(index, docs):
    if not docs:
        return 0
    lines = []
    for d in docs:
        lines.append(json.dumps({'index': {'_index': index, '_id': d['_id']}}, ensure_ascii=False))
        lines.append(json.dumps(d['_source'], ensure_ascii=False))
    payload = '\n'.join(lines) + '\n'
    url = ES + '/_bulk'
    req = urllib.request.Request(url, data=payload.encode('utf-8'), method='POST')
    req.add_header('Content-Type', 'application/x-ndjson')
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            r = json.loads(resp.read().decode('utf-8'))
        return r.get('items', []) and len(r.get('items', [])) or 0
    except Exception as e:
        print(f'[ES] 批量写入失败: {e}')
        return 0


def sync_enterprises():
    conn = pymysql.connect(**MYSQL)
    cur = conn.cursor(pymysql.cursors.DictCursor)
    cur.execute("""SELECT id, name, description, enterprise_type, legal_representative,
                   registered_address, region_id, is_local
                   FROM enterprise WHERE is_deleted = 0""")
    batch = []
    total = 0
    while True:
        row = cur.fetchone()
        if row is None:
            break
        batch.append({
            '_id': str(row['id']),
            '_source': {
                'enterpriseId': row['id'],
                'name': row['name'] or '',
                'businessScope': row['description'] or '',
                'regionId': row['region_id'],
                'isLocal': (row['is_local'] == 1),
                'enterpriseType': row['enterprise_type'] or '',
                'legalRepresentative': row['legal_representative'] or '',
                'registeredAddress': row['registered_address'] or '',
            }
        })
        if len(batch) >= 500:
            n = bulk_insert(ENTERPRISE_INDEX, batch)
            total += n
            print(f'[ES] 企业已写入: {total:,}', end='\r', flush=True)
            batch = []
    if batch:
        n = bulk_insert(ENTERPRISE_INDEX, batch)
        total += n
    print(f'\n[ES] 企业同步完成: {total:,}')
    cur.close(); conn.close()


def sync_patents():
    conn = pymysql.connect(**MYSQL)
    cur = conn.cursor(pymysql.cursors.DictCursor)
    cur.execute("""SELECT id, patent_number, patent_name, applicant, applicant_enterprise_id,
                   ipc_main, patent_type, application_date, legal_status, abstract_text
                   FROM patent WHERE is_deleted = 0""")
    batch = []
    total = 0
    while True:
        row = cur.fetchone()
        if row is None:
            break
        batch.append({
            '_id': str(row['id']),
            '_source': {
                'patentId': row['id'],
                'patentName': row['patent_name'] or '',
                'abstractText': row['abstract_text'] or '',
                'applicant': row['applicant'] or '',
                'applicant_enterprise_id': row['applicant_enterprise_id'],
                'ipcMain': row['ipc_main'] or '',
                'patentType': row['patent_type'] or '',
                'applicationDate': str(row['application_date']) if row['application_date'] else None,
                'legalStatus': row['legal_status'] or '',
            }
        })
        if len(batch) >= 500:
            n = bulk_insert(PATENT_INDEX, batch)
            total += n
            print(f'[ES] 专利已写入: {total:,}', end='\r', flush=True)
            batch = []
    if batch:
        n = bulk_insert(PATENT_INDEX, batch)
        total += n
    print(f'\n[ES] 专利同步完成: {total:,}')
    cur.close(); conn.close()


def main():
    ensure_index(ENTERPRISE_INDEX, ENTERPRISE_MAPPING)
    ensure_index(PATENT_INDEX, PATENT_MAPPING)
    sync_enterprises()
    sync_patents()


if __name__ == '__main__':
    main()
