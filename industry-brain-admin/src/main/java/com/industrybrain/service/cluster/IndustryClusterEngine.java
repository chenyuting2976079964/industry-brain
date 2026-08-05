package com.industrybrain.service.cluster;

import com.industrybrain.config.EmbeddingConfig;
import com.industrybrain.mapper.mysql.EnterpriseMapper;
import com.industrybrain.mapper.mysql.EnterpriseTagMapper;
import com.industrybrain.mapper.mysql.PatentMapper;
import com.industrybrain.service.embedding.EmbeddingProvider;
import com.industrybrain.utils.TextSimilarity;
import com.industrybrain.utils.VectorOps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 产业链自动聚类引擎（向量 + 规则匹配）
 *
 * 聚类对象 = 技术标签（enterprise_tag.tag_name 去重），每簇 = 一条产业链，标签 = 节点。
 * 流程：
 *   STEP 0  通用服务标签预过滤（不归链）
 *   STEP 1  向量编码：嵌入文本 = 标签名 + 代表企业简介聚合
 *   STEP 2  亲和矩阵 A = 向量余弦*vectorWeight + 规则亲和*ruleWeight
 *   STEP 3  传递闭包（并查集）按阈值合并成簇
 *   STEP 4  规则修正：共享 IPC 小簇合并
 *   STEP 5  存在性校验：簇内标签≥3 且 覆盖企业≥5 且 含规模企业
 *   STEP 6  分 layer + 命名 → ClusterResult
 *
 * embedding 不可用时（enabled=false 或失败率>50%）降级为纯规则聚类。
 *
 * @author 产业大脑平台
 */
@Slf4j
@Service
public class IndustryClusterEngine {

    /** 最小簇内标签数（不足不成链） */
    private static final int MIN_TAGS_PER_CLUSTER = 3;
    /** 最小链覆盖企业数 */
    private static final int MIN_ENTERPRISES_PER_CHAIN = 5;
    /** 纯规则模式下合并阈值（SAME_CHAIN=0.9 / 互相包含=0.5 触发合并） */
    private static final double RULE_THRESHOLD = 0.45;
    /** 孤立小簇通过 IPC 并入主簇的标签数上限 */
    private static final int ORPHAN_MAX_TAGS = 2;
    /** 生成 IPC class 的专利按标签取企业上限（防过载） */
    private static final int MAX_IPC_SAMPLE_ENTERPRISES = 300;
    /** 嵌入文本聚合企业简介条数上限（来自配置，此处兜底） */
    private static final int DEFAULT_DESC_LIMIT = 5;

    /** 规范产业链名（TagRuleUtil.TAG_CHAIN_NAME 的值集）：
     *  链名拆分后允许以 2 个标签成链（如 交通装备/检验检测），避免被 MIN_TAGS_PER_CLUSTER 误杀 */
    private static final Set<String> CANONICAL_CHAIN_NAMES = Set.of(
            "软件与信息技术", "电子电气制造", "汽车及新能源", "装备制造", "金属材料",
            "化工新材料", "食品加工", "生物医药", "环保水务", "物流仓储",
            "能源动力", "建筑工程", "交通装备", "检验检测");

    private final EnterpriseTagMapper enterpriseTagMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final PatentMapper patentMapper;
    private final EmbeddingProvider embeddingProvider;
    private final EmbeddingConfig config;

    public IndustryClusterEngine(EnterpriseTagMapper enterpriseTagMapper,
                                 EnterpriseMapper enterpriseMapper,
                                 PatentMapper patentMapper,
                                 EmbeddingProvider embeddingProvider,
                                 EmbeddingConfig config) {
        this.enterpriseTagMapper = enterpriseTagMapper;
        this.enterpriseMapper = enterpriseMapper;
        this.patentMapper = patentMapper;
        this.embeddingProvider = embeddingProvider;
        this.config = config;
    }

    /**
     * 主入口：自动聚类生成产业链
     */
    public ClusterResult buildClusters() {
        // 1. 加载数据
        List<String> allTags = loadAllTags();
        Map<String, Set<Long>> tagEnts = loadTagEnterprises(allTags);
        Map<String, Set<String>> tagIpc = loadTagIpcClasses(allTags);
        Set<Long> scaleEnts = new HashSet<>(enterpriseMapper.selectScaleEnterpriseIds());
        log.info("聚类输入: 标签{}个（通用服务{}个）", allTags.size(), TagRuleUtil.GENERIC_TAGS.size());

        // 2. STEP0 预过滤通用服务标签
        List<String> pool = allTags.stream()
                .filter(t -> !TagRuleUtil.isGeneric(t))
                .collect(Collectors.toList());
        log.info("预过滤后参与聚类标签: {} 个", pool.size());

        if (pool.size() < MIN_TAGS_PER_CLUSTER) {
            log.warn("参与聚类标签过少（{}<3），返回空聚类结果", pool.size());
            return new ClusterResult();
        }

        // 3. STEP1 向量编码
        boolean vectorMode = embeddingProvider.isEnabled();
        Map<String, float[]> tagVector = new HashMap<>();
        if (vectorMode) {
            List<String> texts = pool.stream()
                    .map(t -> buildEmbedText(t, tagEnts.getOrDefault(t, Collections.emptySet())))
                    .collect(Collectors.toList());
            List<float[]> vecs = embeddingProvider.embed(texts);
            int failCount = 0;
            for (int i = 0; i < pool.size(); i++) {
                float[] v = vecs.get(i);
                if (v == null || v.length == 0) { failCount++; v = VectorOps.hashVector(pool.get(i), config.getDimension()); }
                tagVector.put(pool.get(i), VectorOps.resize(v, config.getDimension()));
            }
            double failRate = (double) failCount / pool.size();
            if (failRate > 0.5) {
                log.warn("embedding 失败率 {}（{}），本次降级为纯规则聚类", String.format("%.0f%%", failRate * 100), failCount);
                vectorMode = false;
                tagVector.clear();
            }
            log.info("向量编码完成（{}）", vectorMode ? "向量模式" : "降级规则模式");
        } else {
            log.info("embedding 未启用，走纯规则聚类");
        }

        // 4. STEP2 亲和矩阵
        // 向量模式：affinity = vectorWeight*cos + ruleWeight*rule，阈值 clusterThreshold
        // 纯规则模式：affinity = ruleAffinity（直接），阈值 RULE_THRESHOLD（SAME_CHAIN=0.9 / IPC=0.8 才合并）
        int n = pool.size();
        double[][] affinity = new double[n][n];
        double threshold = vectorMode ? config.getClusterThreshold() : RULE_THRESHOLD;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                String ta = pool.get(i), tb = pool.get(j);
                double vecScore = vectorMode && tagVector.containsKey(ta) && tagVector.containsKey(tb)
                        ? VectorOps.cosine(tagVector.get(ta), tagVector.get(tb)) : 0;
                double ruleScore = ruleAffinity(ta, tb, tagIpc);
                affinity[i][j] = affinity[j][i] = vectorMode
                        ? config.getVectorWeight() * vecScore + config.getRuleWeight() * ruleScore
                        : ruleScore;
            }
        }
        log.info("亲和矩阵完成（{}），阈值 {}", vectorMode ? "向量模式" : "纯规则模式", threshold);

        // 5. STEP3 传递闭包聚类
        List<List<String>> clusters = transitiveClosure(pool, affinity, threshold);
        log.info("传递闭包得到簇: {}", clusters.size());

        // 6. STEP4 规则修正：共享 IPC 的孤立小簇并入主簇（单趟，不连锁）
        clusters = mergeOrphanClusters(clusters, tagIpc);
        log.info("IPC 修正后簇: {}", clusters.size());

        // 7. STEP4b 链名拆分：把跨产业的巨簇按 TAG_CHAIN_NAME 词典拆成多条产业聚焦的簇。
        //    传递闭包按向量+规则相似度合并，标签语义相近但属不同产业（如 医疗器械↔制药、建材↔电子元件）
        //    会被连锁并进同一簇，导致整张图谱只剩一条混业大链。拆到 3 位链名维度即止，避免过度碎片化。
        clusters = splitCrossIndustryClusters(clusters, tagIpc);
        log.info("链名拆分后簇: {}", clusters.size());

        // 7. STEP5 存在性校验 + STEP6 分层命名
        ClusterResult result = new ClusterResult();
        int chainSeq = 0;
        for (List<String> cluster : clusters) {
            // 拆分后的规范链允许 2 个标签成链；其余簇仍要求 ≥ MIN_TAGS_PER_CLUSTER
            String canonName = canonicalNameOf(cluster);
            int minTags = canonName != null && CANONICAL_CHAIN_NAMES.contains(canonName) ? 2 : MIN_TAGS_PER_CLUSTER;
            if (cluster.size() < minTags) continue;
            Set<Long> chainEnts = new HashSet<>();
            for (String tag : cluster) chainEnts.addAll(tagEnts.getOrDefault(tag, Collections.emptySet()));
            boolean hasScale = chainEnts.stream().anyMatch(scaleEnts::contains);
            if (chainEnts.size() < MIN_ENTERPRISES_PER_CHAIN) continue;
            if (!hasScale) {
                log.debug("簇「{}」无规模企业支撑，跳过", cluster);
                continue;
            }
            ClusterResult.ChainDef chain = new ClusterResult.ChainDef();
            chain.setName(nameCluster(cluster, tagIpc, chainSeq++));
            for (String tag : cluster) {
                chain.addTag(assignLayer(tag, tagIpc), tag);
            }
            chain.setTagCount(cluster.size());
            chain.setTotalEnterprises(chainEnts.size());
            result.addChain(chain);
        }
        log.info("聚类完成: 生成产业链 {} 条", result.getChains().size());
        return result;
    }

    // ─── 数据加载 ───

    private List<String> loadAllTags() {
        List<Map<String, Object>> stats = enterpriseTagMapper.selectTagStats();
        if (stats == null) return Collections.emptyList();
        return stats.stream()
                .map(m -> (String) m.get("tagName"))
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private Map<String, Set<Long>> loadTagEnterprises(List<String> tags) {
        Map<String, Set<Long>> result = new HashMap<>();
        for (String tag : tags) {
            // 产业链推导用全部企业（不限本地），节点展示时才限定本地口径
            List<Long> ents = enterpriseTagMapper.selectEnterpriseIdsByTagName(tag);
            result.put(tag, ents == null ? Collections.emptySet() : new HashSet<>(ents));
        }
        return result;
    }

    /** 标签 → 企业专利 IPC class 集（3位前缀） */
    private Map<String, Set<String>> loadTagIpcClasses(List<String> tags) {
        Map<String, Set<String>> result = new HashMap<>();
        for (String tag : tags) {
            Set<String> ipcSet = new HashSet<>();
            // 产业链推导用全部企业（不限本地），节点展示时才限定本地口径
            List<Long> ents = enterpriseTagMapper.selectEnterpriseIdsByTagName(tag);
            if (ents == null || ents.isEmpty()) { result.put(tag, ipcSet); continue; }
            // 采样上限，防过载
            List<Long> sample = ents.size() > MAX_IPC_SAMPLE_ENTERPRISES
                    ? ents.subList(0, MAX_IPC_SAMPLE_ENTERPRISES) : ents;
            List<Map<String, Object>> ipcStats = patentMapper.selectIpcSubclassStatsByEnterpriseIds(sample);
            if (ipcStats != null) {
                for (Map<String, Object> stat : ipcStats) {
                    String ipc = (String) stat.get("ipcSubclass");
                    if (ipc == null || ipc.isEmpty()) continue;
                    String cls = ipc.length() >= 3 ? ipc.substring(0, 3) : ipc;
                    ipcSet.add(cls);
                }
            }
            result.put(tag, ipcSet);
        }
        return result;
    }

    // ─── STEP1 嵌入文本 ───

    /** 嵌入文本 = 标签名 + 该标签下代表企业 description 聚合（前 limit 条） */
    private String buildEmbedText(String tag, Set<Long> enterpriseIds) {
        StringBuilder sb = new StringBuilder(tag);
        int limit = config.getDescriptionLimit() > 0 ? config.getDescriptionLimit() : DEFAULT_DESC_LIMIT;
        int count = 0;
        for (Long eid : enterpriseIds) {
            if (count >= limit) break;
            // 取企业简介（description）作为语义来源；单查成本可控（每标签≤5条）
            var ent = enterpriseMapper.selectById(eid);
            if (ent != null && ent.getDescription() != null && !ent.getDescription().isBlank()) {
                sb.append(" ").append(truncate(ent.getDescription(), 150));
                count++;
            }
        }
        return sb.toString();
    }

    // ─── STEP2 规则亲和 ───

    private double ruleAffinity(String ta, String tb, Map<String, Set<String>> tagIpc) {
        double aff = 0;
        // 语义近亲词典（最可信的规则信号）
        if (TagRuleUtil.isSameChain(ta, tb)) aff = Math.max(aff, 0.9);
        // 互相包含（如 食品加工 ↔ 食品/零售）
        if (TextSimilarity.containsEither(ta, tb)) aff = Math.max(aff, 0.5);
        // 注意：IPC class 共享不在此处计入（IPC 跨产业过于宽泛，易引发传递连锁合并），
        // 仅由 mergeOrphanClusters 用于把孤立小簇并入主簇。
        return aff;
    }

    // ─── STEP3 传递闭包（并查集） ───

    private List<List<String>> transitiveClosure(List<String> tags, double[][] affinity, double theta) {
        int n = tags.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (affinity[i][j] >= theta) union(parent, i, j);
            }
        }
        Map<Integer, List<String>> groups = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            int root = find(parent, i);
            groups.computeIfAbsent(root, k -> new ArrayList<>()).add(tags.get(i));
        }
        return new ArrayList<>(groups.values());
    }

    private int find(int[] parent, int x) {
        while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
        return x;
    }

    private void union(int[] parent, int a, int b) {
        int ra = find(parent, a), rb = find(parent, b);
        if (ra != rb) parent[ra] = rb;
    }

    // ─── STEP4 孤立小簇按共享 IPC 并入主簇（单趟，不连锁） ───

    private List<List<String>> mergeOrphanClusters(List<List<String>> clusters, Map<String, Set<String>> tagIpc) {
        List<List<String>> result = new ArrayList<>();
        List<List<String>> orphans = new ArrayList<>();
        // 主簇：标签数 ≥ ORPHAN_MAX_TAGS 的簇；孤立簇：更小的
        for (List<String> cluster : clusters) {
            if (cluster.size() >= ORPHAN_MAX_TAGS) result.add(cluster);
            else orphans.add(cluster);
        }
        if (orphans.isEmpty()) return result;
        // 每个孤立簇尝试并入第一个与之共享 IPC 的主簇
        for (List<String> orphan : orphans) {
            boolean merged = false;
            for (List<String> main : result) {
                if (shareIpc(orphan, main, tagIpc)) {
                    main.addAll(orphan);
                    merged = true;
                    break;
                }
            }
            if (!merged) result.add(orphan);
        }
        return result;
    }

    private boolean shareIpc(List<String> clusA, List<String> clusB, Map<String, Set<String>> tagIpc) {
        for (String a : clusA) {
            Set<String> ia = tagIpc.getOrDefault(a, Collections.emptySet());
            if (ia.isEmpty()) continue;
            for (String b : clusB) {
                if (!Collections.disjoint(ia, tagIpc.getOrDefault(b, Collections.emptySet()))) return true;
            }
        }
        return false;
    }

    // ─── STEP4b 跨产业巨簇拆分（防连锁合并） ───

    /**
     * 把包含多个规范产业链标签的簇按 TAG_CHAIN_NAME 词典拆分为多条链聚焦簇。
     * 单个簇可能由传递闭包连锁合并而来（如 医疗器械↔制药↔生物技术 把生物医药与医药制造连到一起，
     * 建材↔玻璃↔光伏 把建筑工程与新能源连到一起）。按词典归组，未命中词典的标签作为尾从。
     *
     * 规则：
     *  - 簇内所有标签先映射到规范链名；同一链名的标签聚合为一个子簇（字典序稳定）
     *  - 命中不同链名的标签拆成多个子簇，避免跨产业连锁
     *  - 未命中词典的标签并入最近的已知链名子簇（共享 IPC 优先，否则合并到标签数最多的子簇）
     *  - 簇内只有一个链名（或全未命中）→ 保持原样不拆
     */
    private List<List<String>> splitCrossIndustryClusters(List<List<String>> clusters, Map<String, Set<String>> tagIpc) {
        List<List<String>> result = new ArrayList<>();
        for (List<String> cluster : clusters) {
            result.addAll(splitOneCluster(cluster, tagIpc));
        }
        return result;
    }

    private List<List<String>> splitOneCluster(List<String> cluster, Map<String, Set<String>> tagIpc) {
        if (cluster.size() < 2) return List.of(cluster);

        // 1. 标签 → 链名 归组
        Map<String, List<String>> byChain = new LinkedHashMap<>();
        List<String> unknown = new ArrayList<>();
        for (String tag : cluster) {
            String name = TagRuleUtil.chainNameOf(tag);
            if (name != null) byChain.computeIfAbsent(name, k -> new ArrayList<>()).add(tag);
            else unknown.add(tag);
        }
        if (byChain.size() <= 1) return List.of(cluster);   // 单一产业，无需拆分

        // 2. 未命中词典的标签 → 并入最接近的链名子簇（共享 IPC 优先，其次合并到子簇最大的）
        if (!unknown.isEmpty()) {
            for (String tag : unknown) {
                List<String> best = null;
                double bestScore = -1;
                for (List<String> sub : byChain.values()) {
                    double s = affinityToCluster(tag, sub, tagIpc);
                    if (s > bestScore) { bestScore = s; best = sub; }
                }
                if (best == null) best = largestSubCluster(byChain);
                if (best != null) best.add(tag);
            }
        }
        return new ArrayList<>(byChain.values());
    }

    /** 标签与子簇的亲和度：子簇内与该标签共享 IPC class 的标签数 / 子簇大小（0~1） */
    private double affinityToCluster(String tag, List<String> clusterTags, Map<String, Set<String>> tagIpc) {
        Set<String> tagIpcSet = tagIpc.getOrDefault(tag, Collections.emptySet());
        if (tagIpcSet.isEmpty()) return 0;
        int shared = 0;
        for (String t : clusterTags) {
            if (!Collections.disjoint(tagIpcSet, tagIpc.getOrDefault(t, Collections.emptySet()))) shared++;
        }
        return (double) shared / clusterTags.size();
    }

    /** 标签数最多的子簇（未知标签兜底归属，保证不丢标签） */
    private List<String> largestSubCluster(Map<String, List<String>> byChain) {
        List<String> largest = null;
        for (List<String> sub : byChain.values()) {
            if (largest == null || sub.size() > largest.size()) largest = sub;
        }
        return largest;
    }

    /** 计算簇的规范链名（首个命中词典的标签；无则 null） */
    private String canonicalNameOf(List<String> cluster) {
        for (String tag : cluster) {
            String name = TagRuleUtil.chainNameOf(tag);
            if (name != null) return name;
        }
        return null;
    }

    // ─── STEP6 分层 ───

    /** 标签 → up/mid/down：词典 → IPC众数 → section回退 → mid */
    private String assignLayer(String tag, Map<String, Set<String>> tagIpc) {
        String byDict = TagRuleUtil.layerOf(tag);
        if (byDict != null) return byDict;
        // IPC 众数投票
        Set<String> ipcSet = tagIpc.getOrDefault(tag, Collections.emptySet());
        Map<String, Integer> layerVotes = new HashMap<>();
        for (String ipc : ipcSet) {
            String layer = ipcToLayerByClass(ipc);
            if (layer != null) layerVotes.merge(layer, 1, Integer::sum);
        }
        if (!layerVotes.isEmpty()) {
            return layerVotes.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).get().getKey();
        }
        return "mid";
    }

    private String ipcToLayerByClass(String ipc) {
        if (ipc == null || ipc.isEmpty()) return null;
        char section = ipc.charAt(0);
        if (section == 'C' || section == 'A' || section == 'D') return "up";
        if (section == 'B' || section == 'F' || section == 'E') return "mid";
        if (section == 'G' || section == 'H') return "down";
        return "mid";
    }

    // ─── STEP6 命名 ───

    /** 链命名：词典 → IPC众数 → 最多企业标签+产业链 → 综合产业链-N */
    private String nameCluster(List<String> cluster, Map<String, Set<String>> tagIpc, int seq) {
        // 1. 词典链名（首个命中的）
        for (String tag : cluster) {
            String name = TagRuleUtil.chainNameOf(tag);
            if (name != null) return name;
        }
        // 2. IPC class 众数映射名（复用 GraphBuildService.IPC_CLASS_CHAIN 逻辑在此简化处理）
        Map<String, Integer> ipcFreq = new HashMap<>();
        for (String tag : cluster) {
            for (String ipc : tagIpc.getOrDefault(tag, Collections.emptySet())) {
                ipcFreq.merge(ipc, 1, Integer::sum);
            }
        }
        if (!ipcFreq.isEmpty()) {
            String topIpc = ipcFreq.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).get().getKey();
            String mapped = ipcClassToChainName(topIpc);
            if (mapped != null) return mapped;
        }
        return "综合产业链-" + (seq + 1);
    }

    /** IPC class → 链名（从 GraphBuildService 规则词典反推的简版映射） */
    private String ipcClassToChainName(String ipc) {
        if (ipc == null || ipc.isEmpty()) return null;
        char section = ipc.charAt(0);
        switch (section) {
            case 'C': return "化工新材料";
            case 'B': return "装备制造";
            case 'G': case 'H': return "软件与信息技术";
            case 'A': return "生物医药";
            case 'E': return "建筑工程";
            case 'F': return "装备制造";
            default: return null;
        }
    }

    private String truncate(String s, int max) {
        return s == null ? "" : (s.length() > max ? s.substring(0, max) : s);
    }
}
