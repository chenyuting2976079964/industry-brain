package com.industrybrain.service.impl;

import com.industrybrain.domain.model.*;
import com.industrybrain.mapper.mysql.*;
import com.industrybrain.service.IPatentMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 产业链图谱自动构建引擎
 *
 * 从企业标签（enterprise_tag）聚类生成产业链节点，将企业归入节点，
 * 通过 A1 算法将专利映射到节点，聚合生成节点诊断数据。
 * 启动时若 chain_node 为空则自动构建一次。
 *
 * 图谱数据流：企业标签 → chain_node/chain_node_link/enterprise_node
 *            → A1 专利映射 → patent_node → node_diagnosis
 *
 * @author 产业大脑平台
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphBuildService implements ApplicationRunner {

    private final ChainInfoMapper chainInfoMapper;
    private final ChainNodeMapper chainNodeMapper;
    private final ChainNodeLinkMapper chainNodeLinkMapper;
    private final EnterpriseTagMapper enterpriseTagMapper;
    private final EnterpriseNodeMapper enterpriseNodeMapper;
    private final PatentNodeMapper patentNodeMapper;
    private final NodeDiagnosisMapper nodeDiagnosisMapper;
    private final PatentMapper patentMapper;
    private final IpcChainMappingMapper ipcChainMappingMapper;
    private final IPatentMappingService patentMappingService;
    private final org.springframework.beans.factory.ObjectProvider<Neo4jSyncService> neo4jSyncServiceProvider;

    private static final LocalDateTime NOW = LocalDateTime.now();

    /**
     * 标签 → 产业链归类规则（关键词，命中即归类）
     * 132 个企业标签 → 17 条产业链
     */
    private static final Map<String, List<String>> CHAIN_TAG_RULES = new LinkedHashMap<>();
    static {
        CHAIN_TAG_RULES.put("先进结构材料", List.of(
                "钢铁冶炼", "金属材料", "先进复合材料", "高性能合金", "有色金属", "煤炭/煤化工", "矿产品",
                "玻璃", "陶瓷", "涂料", "建材", "橡胶制品", "塑料制品", "模具制造", "精密加工",
                "石化", "石油/石化", "化工", "精细化工", "电子化学品", "电池材料", "稀土/磁性材料", "再生资源"));
        CHAIN_TAG_RULES.put("智能装备制造", List.of(
                "机械设备", "机器人/自动化", "智能制造", "精密加工", "模具制造", "机电安装", "系统集成", "3D打印"));
        CHAIN_TAG_RULES.put("绿色食品", List.of(
                "食品加工", "食品/零售", "农产品加工", "餐饮/酒店", "冷链物流", "包装科技"));
        CHAIN_TAG_RULES.put("新能源汽车及零部件", List.of(
                "新能源汽车", "充电/换电", "电池材料", "汽车销售/服务", "电线电缆", "电气设备"));
        CHAIN_TAG_RULES.put("智能家电", List.of(
                "家具制造", "日用消费品", "电器/数码", "家用电器"));
        CHAIN_TAG_RULES.put("生命健康", List.of(
                "医疗器械", "制药", "医药制造", "生物技术", "健康/养老", "医疗服务"));
        CHAIN_TAG_RULES.put("电子信息", List.of(
                "电子元器件", "半导体/芯片", "通信技术", "电线电缆", "物联网", "电器/数码"));
        CHAIN_TAG_RULES.put("数字经济", List.of(
                "软件开发", "人工智能", "大数据", "云计算", "区块链", "信息安全", "数字孪生/VR", "地理信息/遥感", "物联网", "系统集成"));
        CHAIN_TAG_RULES.put("新能源", List.of(
                "光伏/太阳能", "风力发电", "储能/氢能", "电池材料", "充电/换电", "新能源汽车", "光热发电", "氢能"));
        CHAIN_TAG_RULES.put("精细化工", List.of(
                "精细化工", "化工", "石油/石化", "电子化学品", "涂料", "塑料制品", "橡胶制品"));
        CHAIN_TAG_RULES.put("现代物流", List.of(
                "物流/仓储", "仓储", "快递配送", "冷链物流", "多式联运/货代", "港口物流", "危险品运输", "港口航道", "船舶/海工"));
        CHAIN_TAG_RULES.put("现代农业", List.of(
                "农产品", "畜牧/水产养殖", "种业/育种", "农产品加工", "农业机械"));
        CHAIN_TAG_RULES.put("机床及刃模具", List.of(
                "模具制造", "精密加工", "机械设备"));
        CHAIN_TAG_RULES.put("节能环保", List.of(
                "污水处理", "废气治理", "固废处理", "土壤修复", "环保", "碳中和", "水务/水环境", "再生资源", "节能"));
        CHAIN_TAG_RULES.put("稀土永磁及软磁材料", List.of(
                "稀土/磁性材料", "金属材料", "有色金属"));
        CHAIN_TAG_RULES.put("低空经济", List.of(
                "无人机", "航空航天", "船舶/海工"));
        CHAIN_TAG_RULES.put("氢能及低碳能源", List.of(
                "储能/氢能", "氢能", "碳中和", "燃气供应", "天然气", "供热", "电力/供电"));
    }

    /**
     * 标签 → 层级归类（上游/中游/下游）
     */
    private String tagToLayer(String tagName) {
        // 原材料/冶炼/开采/化工 → 上游
        if (tagName.contains("冶炼") || tagName.contains("材料") || tagName.contains("矿产")
                || tagName.contains("化工") || tagName.contains("石化") || tagName.contains("原料")
                || tagName.contains("煤") || tagName.contains("石油") || tagName.contains("燃气")
                || tagName.contains("电池材料") || tagName.contains("稀土") || tagName.contains("金属")
                || tagName.contains("铸造") || tagName.contains("锻") || tagName.contains("复合")
                || tagName.contains("元器件") || tagName.contains("半导体") || tagName.contains("芯片")
                || tagName.contains("光伏") || tagName.contains("太阳能")) {
            return "up";
        }
        // 制造/加工/组装/设备 → 中游
        if (tagName.contains("制造") || tagName.contains("加工") || tagName.contains("设备")
                || tagName.contains("机械") || tagName.contains("自动化") || tagName.contains("机器人")
                || tagName.contains("模具") || tagName.contains("组装") || tagName.contains("生产")
                || tagName.contains("制药") || tagName.contains("医药") || tagName.contains("食品加工")
                || tagName.contains("玻璃") || tagName.contains("陶瓷") || tagName.contains("涂料")
                || tagName.contains("建材") || tagName.contains("家具") || tagName.contains("整车")
                || tagName.contains("汽车") || tagName.contains("电器") || tagName.contains("数码")
                || tagName.contains("电动") || tagName.contains("电池")) {
            return "mid";
        }
        // 服务/销售/物流/应用 → 下游
        return "down";
    }

    /**
     * 启动时自动构建（chain_node 为空时）
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            Long count = chainNodeMapper.selectCount(null);
            if (count != null && count > 0) {
                log.info("产业链图谱已存在（chain_node={}），跳过自动构建", count);
                return;
            }
            log.info("chain_node 为空，开始自动构建产业链图谱...");
            rebuildGraph();
            log.info("产业链图谱构建完成");
        } catch (Exception e) {
            log.error("产业链图谱自动构建失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 重建产业链图谱（幂等：先清空再重建）
     * 注意：不使用 @Transactional —— 该方法被同类的 ApplicationRunner 自调用，
     * Spring AOP 代理不拦截自调用，且部分成功的数据也应保留（容错）。
     */
    public void rebuildGraph() {
        clearGraph();
        long start = System.currentTimeMillis();
        int nodeCount = buildNodesFromTags();
        int linkCount = buildLinks();
        int entNodeCount = buildEnterpriseNodes();
        int ipcRuleCount = seedIpcChainMapping();
        int patentNodeCount = buildPatentNodes();
        int diagCount = buildDiagnosis();
        // 同步到 Neo4j（若 Neo4j 未启用则跳过）
        Neo4jSyncService neo4jSync = neo4jSyncServiceProvider.getIfAvailable();
        if (neo4jSync != null) {
            try {
                neo4jSync.syncAll();
            } catch (Exception e) {
                log.warn("Neo4j 图谱同步失败（可忽略）: {}", e.getMessage());
            }
        }
        long elapsed = System.currentTimeMillis() - start;
        log.info("图谱重建完成: 节点={}, 连线={}, 企业关联={}, IPC规则={}, 专利映射={}, 诊断={}, 耗时={}ms",
                nodeCount, linkCount, entNodeCount, ipcRuleCount, patentNodeCount, diagCount, elapsed);
    }

    /**
     * 清空图谱相关表
     */
    private void clearGraph() {
        chainNodeMapper.delete(null);
        chainNodeLinkMapper.delete(null);
        enterpriseNodeMapper.delete(null);
        patentNodeMapper.delete(null);
        nodeDiagnosisMapper.delete(null);
        log.info("已清空图谱相关表");
    }

    /**
     * 从企业标签生成产业链节点
     * 每个标签 → 一个节点，按关键词归类到产业链
     */
    private int buildNodesFromTags() {
        List<Map<String, Object>> tagStats = enterpriseTagMapper.selectTagStats();
        if (tagStats == null || tagStats.isEmpty()) return 0;

        Map<String, Long> chainIdByName = new HashMap<>();
        List<IndustryChain> chains = chainInfoMapper.selectAllOrdered();
        for (IndustryChain c : chains) {
            chainIdByName.put(c.getName(), c.getId());
        }

        int inserted = 0;
        for (Map<String, Object> stat : tagStats) {
            String tagName = (String) stat.get("tagName");
            if (tagName == null || tagName.isBlank()) continue;

            // 归类到产业链
            Long chainId = classifyTagToChain(tagName, chainIdByName);
            if (chainId == null) continue;

            String layer = tagToLayer(tagName);
            ChainNode node = ChainNode.builder()
                    .chainId(chainId)
                    .nodeName(tagName)
                    .layer(layer)
                    .description("由企业标签聚类生成: " + tagName)
                    .sortOrder(0)
                    .isDeleted(0)
                    .createdAt(NOW)
                    .updatedAt(NOW)
                    .build();
            chainNodeMapper.insert(node);
            inserted++;
        }
        log.info("生成产业链节点: {}", inserted);
        return inserted;
    }

    /**
     * 标签 → 产业链归类
     */
    private Long classifyTagToChain(String tagName, Map<String, Long> chainIdByName) {
        for (Map.Entry<String, List<String>> rule : CHAIN_TAG_RULES.entrySet()) {
            for (String kw : rule.getValue()) {
                if (tagName.contains(kw) || kw.contains(tagName)) {
                    Long chainId = chainIdByName.get(rule.getKey());
                    // 精确匹配产业链名（不含括号说明）
                    if (chainId == null) {
                        chainId = chainIdByName.entrySet().stream()
                                .filter(e -> e.getKey().startsWith(rule.getKey()))
                                .map(Map.Entry::getValue).findFirst().orElse(null);
                    }
                    if (chainId != null) return chainId;
                }
            }
        }
        return null;
    }

    /**
     * 生成同链内节点上下游连线
     */
    private int buildLinks() {
        List<ChainNode> allNodes = chainNodeMapper.selectList(null);
        if (allNodes == null || allNodes.isEmpty()) return 0;

        Map<Long, List<ChainNode>> byChain = allNodes.stream()
                .collect(Collectors.groupingBy(ChainNode::getChainId, LinkedHashMap::new, Collectors.toList()));

        int inserted = 0;
        for (List<ChainNode> chainNodes : byChain.values()) {
            if (chainNodes.size() < 2) continue;
            // 按层排序：up → mid → down
            chainNodes.sort(Comparator.comparingInt(n -> layerRank(n.getLayer())));
            for (int i = 0; i < chainNodes.size() - 1; i++) {
                ChainNode src = chainNodes.get(i);
                ChainNode tgt = chainNodes.get(i + 1);
                if (src.getId().equals(tgt.getId())) continue;
                ChainNodeLink link = ChainNodeLink.builder()
                        .sourceNodeId(src.getId())
                        .targetNodeId(tgt.getId())
                        .relationType("SUPPLIES")
                        .isDeleted(0)
                        .createdAt(NOW)
                        .updatedAt(NOW)
                        .build();
                chainNodeLinkMapper.insert(link);
                inserted++;
            }
        }
        log.info("生成产业链连线: {}", inserted);
        return inserted;
    }

    private int layerRank(String layer) {
        if ("up".equals(layer)) return 1;
        if ("mid".equals(layer)) return 2;
        return 3;
    }

    /**
     * 生成企业→节点关联（批量插入，表已清空无需去重）
     */
    private int buildEnterpriseNodes() {
        List<ChainNode> allNodes = chainNodeMapper.selectList(null);
        if (allNodes == null || allNodes.isEmpty()) return 0;

        int inserted = 0;
        List<EnterpriseNode> batch = new ArrayList<>();
        for (ChainNode node : allNodes) {
            List<Long> enterpriseIds = enterpriseTagMapper.selectEnterpriseIdsByTagName(node.getNodeName());
            if (enterpriseIds == null || enterpriseIds.isEmpty()) continue;
            for (Long eid : enterpriseIds) {
                batch.add(EnterpriseNode.builder()
                        .enterpriseId(eid)
                        .nodeId(node.getId())
                        .matchType("auto")
                        .createdAt(NOW)
                        .updatedAt(NOW)
                        .build());
                inserted++;
                if (batch.size() >= 500) {
                    enterpriseNodeMapper.batchInsert(batch);
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            enterpriseNodeMapper.batchInsert(batch);
            batch.clear();
        }
        log.info("生成企业-节点关联: {}", inserted);
        return inserted;
    }

    /**
     * 运行 A1 算法，将专利映射到节点
     */
    private int buildPatentNodes() {
        List<Patent> patents = patentMapper.selectList(null);
        if (patents == null || patents.isEmpty()) return 0;

        int inserted = 0;
        int total = patents.size();
        int processed = 0;
        List<PatentNode> batch = new ArrayList<>();
        for (Patent patent : patents) {
            try {
                List<IPatentMappingService.MappingResult> results = patentMappingService.mapPatentToNode(patent);
                for (IPatentMappingService.MappingResult r : results) {
                    if (r.getNodeId() == null) continue;
                    // 只保留 HIGH/MEDIUM 置信度
                    if ("LOW".equals(r.getConfidenceLevel())) continue;
                    batch.add(PatentNode.builder()
                            .patentId(patent.getId())
                            .nodeId(r.getNodeId())
                            .matchType("auto")
                            .createdAt(NOW)
                            .updatedAt(NOW)
                            .build());
                    inserted++;
                    if (batch.size() >= 500) {
                        patentNodeMapper.batchInsert(batch);
                        batch.clear();
                    }
                }
            } catch (Exception e) {
                log.debug("专利映射失败: id={}, error={}", patent.getId(), e.getMessage());
            }
            processed++;
            if (processed % 500 == 0) log.info("专利映射进度: {}/{}", processed, total);
        }
        if (!batch.isEmpty()) {
            patentNodeMapper.batchInsert(batch);
            batch.clear();
        }
        log.info("生成专利-节点映射: {}", inserted);
        return inserted;
    }

    /**
     * 聚合生成节点诊断数据
     */
    private int buildDiagnosis() {
        List<ChainNode> allNodes = chainNodeMapper.selectList(null);
        if (allNodes == null || allNodes.isEmpty()) return 0;

        int inserted = 0;
        LocalDate today = LocalDate.now();
        for (ChainNode node : allNodes) {
            int enterpriseCount = enterpriseNodeMapper.countByNodeId(node.getId());
            int patentCount = patentNodeMapper.countByNodeId(node.getId());
            int highValueCount = 0;

            // 诊断标签：按企业/专利数量分级
            String tag;
            if (enterpriseCount >= 20 || patentCount >= 200) {
                tag = "强链";
            } else if (enterpriseCount >= 5 || patentCount >= 50) {
                tag = "补链";
            } else if (enterpriseCount > 0 || patentCount > 0) {
                tag = "弱链";
            } else {
                tag = "缺链";
            }

            NodeDiagnosis diag = NodeDiagnosis.builder()
                    .regionId(340500L)  // 马鞍山市
                    .nodeId(node.getId())
                    .diagnosisTag(tag)
                    .highValuePatentCount(highValueCount)
                    .totalPatentCount(patentCount)
                    .enterpriseCount(enterpriseCount)
                    .patentDensity(enterpriseCount > 0 ? new BigDecimal((double) patentCount / enterpriseCount) : BigDecimal.ZERO)
                    .diagnosisBasis("基于企业标签聚类与专利映射自动生成")
                    .diagnosisDate(today)
                    .isDeleted(0)
                    .createdAt(NOW)
                    .updatedAt(NOW)
                    .build();
            nodeDiagnosisMapper.insert(diag);
            inserted++;
        }
        log.info("生成节点诊断: {}", inserted);
        return inserted;
    }

    /**
     * 填充 IPC→节点映射规则
     * 基于专利 IPC 分布与节点名的关键词匹配
     */
    private int seedIpcChainMapping() {
        List<ChainNode> allNodes = chainNodeMapper.selectList(null);
        if (allNodes == null || allNodes.isEmpty()) return 0;

        List<Map<String, Object>> ipcStats = patentMapper.selectIpcStats();
        if (ipcStats == null || ipcStats.isEmpty()) return 0;

        int seeded = 0;
        for (Map<String, Object> stat : ipcStats) {
            String ipc = (String) stat.get("ipcMain");
            Number cnt = (Number) stat.get("cnt");
            if (ipc == null || cnt == null) continue;
            String ipcSection = ipc.split("/")[0];

            // 匹配节点：IPC 部分匹配节点名（简化：直接映射到"先进结构材料"等链）
            ChainNode matched = findNodeByIpc(allNodes, ipcSection);
            if (matched == null) continue;

            IpcChainMapping m = IpcChainMapping.builder()
                    .ipcCode(ipcSection)
                    .ipcLevel("GROUP")
                    .nodeId(matched.getId())
                    .weight(Math.min(0.6 + cnt.intValue() * 0.001, 0.95))
                    .description("专利IPC聚类: " + ipcSection + " (" + cnt + "件)")
                    .build();
            ipcChainMappingMapper.insert(m);
            seeded++;
        }
        log.info("填充 IPC→节点映射规则: {}", seeded);
        return seeded;
    }

    private ChainNode findNodeByIpc(List<ChainNode> nodes, String ipcSection) {
        // 按 IPC 字母映射到产业链（C→材料/化工, B→装备, G→信息）
        String chainName;
        if (ipcSection.startsWith("C22") || ipcSection.startsWith("C21") || ipcSection.startsWith("C23")) chainName = "先进结构材料";
        else if (ipcSection.startsWith("B") && ipcSection.startsWith("B2")) chainName = "智能装备制造";
        else if (ipcSection.startsWith("G") || ipcSection.startsWith("H")) chainName = "电子信息";
        else if (ipcSection.startsWith("A")) chainName = "现代农业";
        else if (ipcSection.startsWith("C") || ipcSection.startsWith("B")) chainName = "精细化工";
        else if (ipcSection.startsWith("F")) chainName = "现代物流";
        else return null;

        Long targetChainId = chainIdByName(chainName);
        if (targetChainId == null) return null;
        return nodes.stream()
                .filter(n -> targetChainId.equals(n.getChainId()))
                .findFirst().orElse(null);
    }

    /** 产业链名称 → id 映射（惰性缓存） */
    private Map<String, Long> chainNameCache;
    private Long chainIdByName(String chainName) {
        if (chainNameCache == null) {
            chainNameCache = new HashMap<>();
            List<IndustryChain> chains = chainInfoMapper.selectAllOrdered();
            for (IndustryChain c : chains) {
                chainNameCache.put(c.getName(), c.getId());
                chainNameCache.put(c.getName().split("（")[0], c.getId());
            }
        }
        return chainNameCache.get(chainName);
    }
}
