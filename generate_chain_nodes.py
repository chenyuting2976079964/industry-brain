"""Generate chain nodes, links, and diagnosis data for all 17 industry chains."""
import pymysql

DB = {'host': 'localhost', 'port': 3307, 'user': 'root', 'password': 'root123', 'database': 'industry_brain'}

# Chain definitions: (id, name, nodes: [(name, layer), ...])
# layer: up=上游, mid=中游, down=下游
CHAINS = {
    1: ("先进结构材料（钢铁及新材料）", [
        ("铁矿石采选", "up"), ("焦化炼焦", "up"), ("耐火材料", "up"),
        ("炼铁", "mid"), ("炼钢", "mid"), ("连铸", "mid"),
        ("热轧板材", "mid"), ("冷轧薄板", "mid"), ("特种钢冶炼", "mid"),
        ("热浸镀锌", "down"), ("彩涂板", "down"), ("钢结构加工", "down"),
        ("汽车用钢", "down"), ("家电用钢", "down"), ("管线钢", "down"),
    ]),
    2: ("智能装备制造", [
        ("伺服电机", "up"), ("减速器", "up"), ("控制器", "up"), ("传感器", "up"),
        ("数控系统", "mid"), ("精密导轨", "mid"), ("液压系统", "mid"),
        ("工业机器人", "down"), ("数控机床", "down"), ("自动化产线", "down"),
        ("智能检测设备", "down"),
    ]),
    3: ("绿色食品", [
        ("农产品种植", "up"), ("畜禽养殖", "up"), ("食品添加剂", "up"),
        ("粮油加工", "mid"), ("肉制品加工", "mid"), ("饮料制造", "mid"),
        ("速冻食品", "down"), ("休闲食品", "down"), ("预制菜", "down"),
        ("冷链物流", "down"),
    ]),
    4: ("新能源汽车及零部件", [
        ("锂矿提取", "up"), ("正极材料", "up"), ("负极材料", "up"), ("电解液", "up"), ("隔膜", "up"),
        ("动力电池", "mid"), ("驱动电机", "mid"), ("电控系统", "mid"),
        ("整车制造", "down"), ("充电桩", "down"), ("电池回收", "down"),
    ]),
    5: ("智能家电（家居）", [
        ("芯片设计", "up"), ("传感器模组", "up"), ("塑料原料", "up"),
        ("智能控制板", "mid"), ("变频压缩机", "mid"), ("显示面板", "mid"),
        ("智能冰箱", "down"), ("智能空调", "down"), ("智能洗衣机", "down"),
        ("全屋智能系统", "down"),
    ]),
    6: ("生命健康（生物医药）", [
        ("原料药", "up"), ("药用辅料", "up"), ("中药材种植", "up"),
        ("化学制药", "mid"), ("生物制药", "mid"), ("医疗器械", "mid"),
        ("中药制剂", "down"), ("体外诊断", "down"), ("医药流通", "down"),
    ]),
    7: ("电子信息", [
        ("半导体材料", "up"), ("电子化学品", "up"), ("IC设计", "up"),
        ("晶圆制造", "mid"), ("封装测试", "mid"), ("PCB制造", "mid"),
        ("显示器件", "down"), ("通信设备", "down"), ("消费电子", "down"),
    ]),
    8: ("数字经济（含人工智能）", [
        ("算力芯片", "up"), ("数据中心", "up"), ("光通信模块", "up"),
        ("云计算平台", "mid"), ("大数据分析", "mid"), ("AI算法框架", "mid"),
        ("工业互联网", "down"), ("智慧城市", "down"), ("数字孪生", "down"),
    ]),
    9: ("新能源（光伏及储能）", [
        ("多晶硅", "up"), ("光伏玻璃", "up"), ("EVA胶膜", "up"),
        ("硅片制造", "mid"), ("电池片", "mid"), ("光伏组件", "mid"),
        ("光伏电站", "down"), ("储能系统", "down"), ("逆变器", "down"),
    ]),
    10: ("精细化工", [
        ("基础化工原料", "up"), ("催化剂", "up"), ("溶剂", "up"),
        ("涂料制造", "mid"), ("胶粘剂", "mid"), ("表面活性剂", "mid"),
        ("电子化学品", "down"), ("医药中间体", "down"), ("农药制剂", "down"),
    ]),
    11: ("现代物流及跨境电商", [
        ("智能仓储", "up"), ("AGV搬运", "up"), ("包装材料", "up"),
        ("干线运输", "mid"), ("分拣配送", "mid"), ("跨境通关", "mid"),
        ("末端配送", "down"), ("冷链物流", "down"), ("电商平台", "down"),
    ]),
    12: ("现代农业", [
        ("种业研发", "up"), ("生物肥料", "up"), ("智能农机", "up"),
        ("大田种植", "mid"), ("设施农业", "mid"), ("畜牧养殖", "mid"),
        ("农产品加工", "down"), ("农贸流通", "down"), ("休闲农业", "down"),
    ]),
    13: ("机床及刃模具", [
        ("工具钢材料", "up"), ("硬质合金", "up"), ("精密铸造", "up"),
        ("数控刀架", "mid"), ("磨削工具", "mid"), ("冲压模具", "mid"),
        ("五轴机床", "down"), ("激光切割", "down"), ("精密测量", "down"),
    ]),
    14: ("节能环保", [
        ("脱硫脱硝剂", "up"), ("膜材料", "up"), ("活性炭", "up"),
        ("工业除尘", "mid"), ("废水处理", "mid"), ("固废处置", "mid"),
        ("余热回收", "down"), ("碳捕集", "down"), ("环境监测", "down"),
    ]),
    15: ("稀土永磁及软磁材料", [
        ("稀土矿采选", "up"), ("稀土分离", "up"), ("金属钕", "up"),
        ("钕铁硼永磁", "mid"), ("软磁铁氧体", "mid"), ("非晶纳米晶", "mid"),
        ("永磁电机", "down"), ("磁传感器", "down"), ("磁共振成像", "down"),
    ]),
    16: ("低空经济", [
        ("碳纤维材料", "up"), ("飞控芯片", "up"), ("卫星导航", "up"),
        ("无人机制造", "mid"), ("eVTOL飞行器", "mid"), ("空管系统", "mid"),
        ("低空物流", "down"), ("航拍测绘", "down"), ("城市空中交通", "down"),
    ]),
    17: ("氢能及低碳能源", [
        ("电解水制氢", "up"), ("工业副产氢", "up"), ("储氢材料", "up"),
        ("氢气纯化", "mid"), ("燃料电池堆", "mid"), ("加氢站", "mid"),
        ("氢能重卡", "down"), ("氢能发电", "down"), ("氢冶金", "down"),
    ]),
}

TAGS = ["强链", "补链", "弱链", "缺链"]


def main():
    conn = pymysql.connect(**DB)
    cur = conn.cursor()
    cur.execute("SET FOREIGN_KEY_CHECKS = 0")

    # Clear existing data
    cur.execute("DELETE FROM node_diagnosis")
    cur.execute("DELETE FROM chain_node_link")
    cur.execute("DELETE FROM chain_node")
    conn.commit()

    # Get total patent and enterprise counts for diagnosis
    cur.execute("SELECT COUNT(*) FROM enterprise WHERE is_deleted=0")
    total_enterprises = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM patent WHERE is_deleted=0")
    total_patents = cur.fetchone()[0]

    node_id = 1
    link_id = 1
    diag_id = 1

    for chain_id, (chain_name, nodes) in CHAINS.items():
        created_nodes = []
        for i, (node_name, layer) in enumerate(nodes):
            cur.execute(
                "INSERT INTO chain_node (id, chain_id, name, layer, description, sort_order) "
                "VALUES (%s, %s, %s, %s, %s, %s)",
                (node_id, chain_id, node_name, layer,
                 f"{chain_name} - {node_name}", i + 1))
            created_nodes.append((node_id, node_name, layer))
            node_id += 1

        # Create links: up → mid (供给关系), mid → down (供给关系)
        up_nodes = [n for n in created_nodes if n[2] == "up"]
        mid_nodes = [n for n in created_nodes if n[2] == "mid"]
        down_nodes = [n for n in created_nodes if n[2] == "down"]

        n_up, n_mid, n_down = len(up_nodes), len(mid_nodes), len(down_nodes)
        # Up → Mid: each up feeds 1-2 mids
        for ui, up in enumerate(up_nodes):
            for mi in range(ui % max(n_mid, 1), min(n_mid, (ui % max(n_mid, 1)) + 2)):
                if mi < n_mid:
                    cur.execute(
                        "INSERT INTO chain_node_link (id, source_node_id, target_node_id, relation_type) "
                        "VALUES (%s, %s, %s, %s)",
                        (link_id, up[0], mid_nodes[mi][0], "SUPPLIES"))
                    link_id += 1
        # Mid → Down: each mid feeds 1-2 downs
        for mi, mid in enumerate(mid_nodes):
            for di in range(mi % max(n_down, 1), min(n_down, (mi % max(n_down, 1)) + 2)):
                if di < n_down:
                    cur.execute(
                        "INSERT INTO chain_node_link (id, source_node_id, target_node_id, relation_type) "
                        "VALUES (%s, %s, %s, %s)",
                        (link_id, mid[0], down_nodes[di][0], "SUPPLIES"))
                    link_id += 1

        # Generate diagnosis for each node
        import random
        random.seed(chain_id)
        for nid, nname, layer in created_nodes:
            import random
            random.seed(chain_id * 100 + nid)
            # Simulate realistic patent/enterprise counts
            base = random.randint(3, 50)
            tag = random.choices(TAGS, weights=[0.25, 0.30, 0.30, 0.15])[0]
            hp_count = random.randint(0, base * 2)
            ent_count = random.randint(1, max(2, base // 5))
            density = round(random.uniform(0.5, 5.0), 2)

            cur.execute(
                "INSERT INTO node_diagnosis (id, region_id, node_id, diagnosis_tag, "
                "high_value_patent_count, total_patent_count, enterprise_count, "
                "patent_density, diagnosis_basis, diagnosis_date) "
                "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, CURDATE())",
                (diag_id, 340500, nid, tag, hp_count, base, ent_count, density,
                 f"基于{ent_count}家企业专利数据分析，诊断为{tag}"))
            diag_id += 1

        conn.commit()
        print(f"[OK] {chain_name}: {len(nodes)} nodes, {len(nodes)*len(up_nodes+down_nodes)} links")

    cur.execute("SET FOREIGN_KEY_CHECKS = 1")
    conn.commit()

    # Verify
    for t in ['chain_node', 'chain_node_link', 'node_diagnosis']:
        cur.execute(f"SELECT COUNT(*) FROM {t}")
        print(f"[VERIFY] {t}: {cur.fetchone()[0]:,}")

    cur.close()
    conn.close()
    print("[DONE]")


if __name__ == '__main__':
    main()
