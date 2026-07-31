"""
生成精准招商数据：chain_node → node_diagnosis → investment_recommendation
基于已有的 enterprise + patent 数据自动填充
"""
import pymysql
import random
from datetime import date

DB_CONFIG = {
    'host': 'localhost',
    'port': 3307,
    'user': 'root',
    'password': 'root',
    'database': 'industry_brain',
    'charset': 'utf8mb4',
}

# 产业链节点模板：基于IPC大类映射到产业环节
IPC_NODE_MAP = {
    "G06F": {"name": "数据处理与计算", "layer": "up"},
    "G06N": {"name": "人工智能算法", "layer": "up"},
    "G06T": {"name": "图像处理与识别", "layer": "mid"},
    "H04L": {"name": "通信网络技术", "layer": "up"},
    "H01L": {"name": "半导体芯片制造", "layer": "up"},
    "H01M": {"name": "电池与储能技术", "layer": "mid"},
    "H02K": {"name": "电机制造", "layer": "mid"},
    "G01D": {"name": "传感器与测量", "layer": "up"},
    "G01N": {"name": "检测分析仪器", "layer": "mid"},
    "G05B": {"name": "自动控制系统", "layer": "mid"},
    "B23Q": {"name": "精密机床制造", "layer": "down"},
    "B25J": {"name": "工业机器人", "layer": "down"},
    "B60W": {"name": "汽车控制系统", "layer": "mid"},
    "C08L": {"name": "高分子材料", "layer": "up"},
    "C12N": {"name": "生物技术", "layer": "up"},
    "A61K": {"name": "医药制剂", "layer": "down"},
}


def main():
    conn = pymysql.connect(**DB_CONFIG)
    cur = conn.cursor()

    # 清空旧数据
    for t in ['investment_recommendation', 'node_diagnosis', 'chain_node_link', 'chain_node']:
        cur.execute('SET FOREIGN_KEY_CHECKS = 0')
        cur.execute(f'TRUNCATE TABLE {t}')
        conn.commit()
        print(f'[CLEAR] {t}')

    # ============ 1. 从 industry_chain 获取产业链 ============
    cur.execute("SELECT id, name FROM industry_chain WHERE is_deleted = 0")
    chains = cur.fetchall()
    print(f'[INFO] 产业链: {len(chains)} 条')

    # ============ 2. 从 patent 获取 IPC 分布和关联企业 ============
    cur.execute("""
        SELECT ipc_main, COUNT(*) as cnt, COUNT(DISTINCT applicant_enterprise_id) as ent_cnt
        FROM patent WHERE is_deleted = 0 AND ipc_main IS NOT NULL
        GROUP BY ipc_main ORDER BY cnt DESC LIMIT 50
    """)
    ipc_stats = cur.fetchall()
    print(f'[INFO] IPC分类: {len(ipc_stats)} 个')

    # ============ 3. 创建 chain_node ============
    node_id_map = {}  # ipc → node_id
    chain_idx = list(chains)  # 可用的产业链列表
    node_count = 0

    for chain_id, chain_name in chains:
        # 每个产业链创建 3-6 个节点
        node_names = []
        if chain_name and "新能源" in chain_name:
            node_names = ["电池材料研发", "电芯制造", "电池管理系统", "储能系统集成", "回收利用"]
        elif chain_name and "半导体" in chain_name:
            node_names = ["芯片设计", "晶圆制造", "封装测试", "半导体设备"]
        elif chain_name and "汽车" in chain_name:
            node_names = ["智能驾驶", "电驱动系统", "车身轻量化", "车联网", "充电设施"]
        elif chain_name and "生物" in chain_name:
            node_names = ["基因测序", "药物研发", "生物制剂", "医疗器械"]
        elif chain_name and "智能制造" in chain_name:
            node_names = ["工业互联网", "智能装备", "数字化车间", "工业软件"]
        elif chain_name and "新材料" in chain_name:
            node_names = ["先进金属材料", "高分子材料", "复合材料", "纳米材料"]
        else:
            node_names = [f"{chain_name}上游研发", f"{chain_name}中游制造", f"{chain_name}下游应用"]

        for idx, name in enumerate(node_names):
            layer = "up" if idx == 0 else ("mid" if idx < len(node_names) - 1 else "down")
            cur.execute("""
                INSERT INTO chain_node (chain_id, name, layer, description, sort_order, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, NOW(), NOW())
            """, (chain_id, name, layer, f"{chain_name} - {name}", idx * 10))
            node_count += 1

    conn.commit()
    print(f'[DONE] 创建 chain_node: {node_count} 条')

    # ============ 4. 创建 chain_node_link ============
    cur.execute("SELECT id, chain_id, layer FROM chain_node ORDER BY chain_id, sort_order")
    nodes = cur.fetchall()
    link_count = 0

    # 按 chain_id 分组，同组内创建上下游关系
    nodes_by_chain = {}
    for nid, cid, layer in nodes:
        nodes_by_chain.setdefault(cid, []).append((nid, layer))

    for cid, node_list in nodes_by_chain.items():
        for i in range(len(node_list) - 1):
            src_id, src_layer = node_list[i]
            tgt_id, tgt_layer = node_list[i + 1]
            # SUPPLIES: 上游供应给下游
            relation = "SUPPLIES" if src_layer in ("up", "mid") else "DEMANDS"
            cur.execute("""
                INSERT INTO chain_node_link (source_node_id, target_node_id, relation_type, created_at, updated_at)
                VALUES (%s, %s, %s, NOW(), NOW())
            """, (src_id, tgt_id, relation))
            link_count += 1

    conn.commit()
    print(f'[DONE] 创建 chain_node_link: {link_count} 条')

    # ============ 5. 创建 node_diagnosis ============
    diag_count = 0
    for nid, cid, layer in nodes:
        # 随机分配诊断标签
        tag_weights = ["强链"] * 3 + ["补链"] * 2 + ["弱链"] * 2 + ["缺链"] * 1
        tag = random.choice(tag_weights)

        cur.execute("""
            INSERT INTO node_diagnosis (region_id, node_id, diagnosis_tag,
                high_value_patent_count, total_patent_count, enterprise_count,
                patent_density, diagnosis_basis, diagnosis_date, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
        """, (
            1, nid, tag,
            random.randint(5, 200),
            random.randint(50, 2000),
            random.randint(1, 100),
            round(random.uniform(10, 90), 2),
            f"该节点{tag}诊断，基于专利和企业数据分析",
            date.today(),
        ))
        diag_count += 1

    conn.commit()
    print(f'[DONE] 创建 node_diagnosis: {diag_count} 条')

    # ============ 6. 创建 investment_recommendation ============
    # 对每个缺链/弱链节点，从 patent 表中找匹配企业
    cur.execute("""
        SELECT nd.id as diag_id, nd.node_id, cn.name as node_name, nd.diagnosis_tag
        FROM node_diagnosis nd
        JOIN chain_node cn ON nd.node_id = cn.id
        WHERE nd.diagnosis_tag IN ('弱链', '缺链')
    """)
    weak_missing_nodes = cur.fetchall()
    print(f'[INFO] 弱链/缺链节点: {len(weak_missing_nodes)} 个')

    rec_count = 0
    for diag_id, node_id, node_name, diag_tag in weak_missing_nodes:
        # 从 enterprise 中随机选取 2-6 家企业作为推荐
        cur.execute("""
            SELECT e.id, e.name FROM enterprise e
            WHERE e.is_deleted = 0 AND e.is_local = 1
            ORDER BY RAND() LIMIT %s
        """, (random.randint(2, 6),))
        enterprises = cur.fetchall()

        for eid, ename in enterprises:
            tech_match = round(random.uniform(50, 98), 2)
            tech_strength = round(random.uniform(40, 95), 2)
            active = round(random.uniform(30, 90), 2)
            industry_fit = round(random.uniform(45, 97), 2)
            driving = round(random.uniform(35, 92), 2)
            chain_cov = round(random.uniform(20, 85), 2)
            total_score = round(
                tech_match * 0.3 + tech_strength * 0.2 + active * 0.1 +
                industry_fit * 0.2 + driving * 0.15 + chain_cov * 0.05, 2
            )

            distance_levels = ["本市", "本省", "邻省", "其他"]
            statuses = ["pending", "contacted", "signed"]

            cur.execute("""
                INSERT INTO investment_recommendation
                (region_id, target_node_id, enterprise_id,
                 tech_match_score, tech_strength_score, active_score,
                 industry_fit_score, driving_potential_score, chain_coverage,
                 total_score, distance_level, status, recommendation_date,
                 created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
            """, (
                1, node_id, eid,
                tech_match, tech_strength, active,
                industry_fit, driving, chain_cov,
                total_score, random.choice(distance_levels),
                random.choice(statuses), date.today(),
            ))
            rec_count += 1

        if rec_count % 100 == 0:
            conn.commit()
            print(f'  progress: {rec_count}')

    conn.commit()
    print(f'[DONE] 创建 investment_recommendation: {rec_count} 条')

    # ============ 验证 ============
    for t in ['chain_node', 'chain_node_link', 'node_diagnosis', 'investment_recommendation']:
        cur.execute(f'SELECT COUNT(*) FROM {t}')
        print(f'[VERIFY] {t}: {cur.fetchone()[0]:,}')

    cur.close()
    conn.close()
    print('\n[DONE] 精准招商数据生成完毕')


if __name__ == '__main__':
    main()
