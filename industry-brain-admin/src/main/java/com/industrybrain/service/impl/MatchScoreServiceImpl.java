package com.industrybrain.service.impl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.industrybrain.domain.model.IndustryChainNode;
import com.industrybrain.domain.model.MatchScoreResult;
import com.industrybrain.domain.model.Patent;
import com.industrybrain.domain.model.Enterprise;
import com.industrybrain.domain.model.Region;
import com.industrybrain.domain.model.IpcChainMapping;
import com.industrybrain.mapper.elasticsearch.EnterpriseSearchMapper;
import com.industrybrain.mapper.mysql.EnterpriseMapper;
import com.industrybrain.mapper.mysql.IndustryChainNodeMapper;
import com.industrybrain.mapper.mysql.MatchScoreResultMapper;
import com.industrybrain.mapper.mysql.PatentMapper;
import com.industrybrain.mapper.mysql.RegionMapper;
import com.industrybrain.mapper.mysql.IpcChainMappingMapper;
import com.industrybrain.service.IMatchScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 技术匹配度算法实现类（算法A3）
 *
 * 算法流程：
 *   Step1: 标签向量化 — 企业技术标签 + 节点需求标签 → 合并去重 → TF-IDF词袋向量
 *   Step2: 余弦相似度 → 基础分(0~100)
 *   Step3: 附加项 — IPC交集加分(上限15%) + 高价值专利加分 + 同省加分
 *   Step4: 加权融合 — 标签相似60% + IPC加分 + 专利加分 + 地理加分 → 上限100%
 *   Step5: 分级 — ≥85%高度匹配 / 70-84%中度 / <70%低度
 */
@Service
@ConditionalOnProperty(name = "mysql.enabled", havingValue = "true", matchIfMissing = true)
public class MatchScoreServiceImpl implements IMatchScoreService {

    private static final Logger log = LoggerFactory.getLogger(MatchScoreServiceImpl.class);

    @Resource
    private EnterpriseMapper enterpriseMapper;

    @Resource
    private PatentMapper patentMapper;

    @Resource(name = "industryChainNodeMapper")
    private IndustryChainNodeMapper chainNodeMapper;

    @Resource
    private MatchScoreResultMapper matchScoreResultMapper;

    @Resource
    private EnterpriseSearchMapper enterpriseSearchMapper;

    @Resource
    private RegionMapper regionMapper;

    @Resource
    private IpcChainMappingMapper ipcChainMappingMapper;  // IPC->产业链映射规则表

    // ==================== 权重常量 ====================
    /** 标签余弦相似度权重 */
    private static final double TAG_SIMILARITY_WEIGHT = 0.60;
    /** IPC 附加分每重叠1个的加分 */
    private static final double IPC_OVERLAP_BONUS_PER = 0.05;
    /** IPC 附加分上限 */
    private static final double IPC_BONUS_CAP = 0.15;
    /** 高价值专利>20 加分 */
    private static final double HIGH_VALUE_20_BONUS = 0.05;
    /** 高价值专利>50 加分 */
    private static final double HIGH_VALUE_50_BONUS = 0.10;
    /** 同省加分 */
    private static final double SAME_PROVINCE_BONUS = 0.05;

    // ==================== 分级阈值 ====================
    private static final double HIGH_MATCH_THRESHOLD = 85.0;
    private static final double MEDIUM_MATCH_THRESHOLD = 70.0;

    @Override
    public MatchScoreResult calculateMatch(Long enterpriseId, Long chainId) {
        log.info("=== A3 技术匹配度计算开始: enterpriseId={}, chainId={} ===", enterpriseId, chainId);

        // -------------------- Step1: 标签向量化 --------------------
        // 1a. 获取企业技术标签
        Enterprise enterprise = enterpriseMapper.selectById(enterpriseId);
        if (enterprise == null) {
            log.warn("企业不存在: enterpriseId={}", enterpriseId);
            return buildZeroResult(enterpriseId, chainId, "企业不存在");
        }

        List<String> enterpriseTags = extractEnterpriseTags(enterprise);
        log.info("Step1a: 企业技术标签 = {}", enterpriseTags);

        // 1b. 获取节点需求标签
        IndustryChainNode node = chainNodeMapper.selectById(chainId);
        List<String> nodeDemandTags = extractNodeDemandTags(node);
        log.info("Step1b: 节点需求标签 = {}", nodeDemandTags);

        // 1c. 获取企业IPC分类
        List<Patent> patents = patentMapper.selectByEnterpriseId(enterpriseId);
        Set<String> enterpriseIpcSet = patents.stream()
                .map(Patent::getIpcCode)
                .filter(Objects::nonNull)
                .map(ipc -> ipc.length() >= 4 ? ipc.substring(0, 4) : ipc)
                .collect(Collectors.toSet());
        log.info("Step1c: 企业IPC大类 = {}", enterpriseIpcSet);

        // 1d. 获取节点关键IPC（从节点描述/标签推断或从专利映射获取）
        Set<String> nodeIpcSet = extractNodeKeyIpc(node);
        log.info("Step1d: 节点关键IPC = {}", nodeIpcSet);

        // 1e. 合并标签去重，构建词袋
        Set<String> vocabulary = new LinkedHashSet<>();
        vocabulary.addAll(enterpriseTags);
        vocabulary.addAll(nodeDemandTags);
        // 将IPC也加入词袋
        vocabulary.addAll(enterpriseIpcSet);
        vocabulary.addAll(nodeIpcSet);
        log.info("Step1e: 合并词袋大小 = {}", vocabulary.size());

        // 1f. 计算 TF 向量
        Map<String, Double> enterpriseTf = computeTf(enterpriseTags, enterpriseIpcSet, vocabulary);
        Map<String, Double> nodeTf = computeTf(nodeDemandTags, nodeIpcSet, vocabulary);

        // -------------------- Step2: 余弦相似度 --------------------
        double cosineSimilarity = cosineSimilarity(enterpriseTf, nodeTf, vocabulary);
        double baseScore = cosineSimilarity * 100.0; // 转换到0~100
        log.info("Step2: 余弦相似度 = {}, 基础分 = {}", String.format("%.4f", cosineSimilarity), String.format("%.2f", baseScore));

        // -------------------- Step3: 附加项 --------------------
        double bonusTotal = 0.0;
        List<String> matchReasons = new ArrayList<>();

        // 3a. IPC 交集加分：每重叠1个 +5%，上限15%
        Set<String> ipcIntersection = new HashSet<>(enterpriseIpcSet);
        ipcIntersection.retainAll(nodeIpcSet);
        double ipcBonus = Math.min(ipcIntersection.size() * IPC_OVERLAP_BONUS_PER, IPC_BONUS_CAP);
        bonusTotal += ipcBonus;
        if (ipcBonus > 0) {
            matchReasons.add("IPC技术领域重叠" + ipcIntersection.size() + "项(+" + String.format("%.0f%%", ipcBonus * 100) + ")");
        }
        log.info("Step3a: IPC交集 = {}, 加分 = {}", ipcIntersection.size(), String.format("%.2f", ipcBonus));

        // 3b. 高价值专利加分
        long highValueCount = patentMapper.selectHighValueCountByEnterprise(enterpriseId);
        double patentBonus = 0.0;
        if (highValueCount > 50) {
            patentBonus = HIGH_VALUE_50_BONUS;
            matchReasons.add("高价值专利超50项(+" + String.format("%.0f%%", patentBonus * 100) + ")");
        } else if (highValueCount > 20) {
            patentBonus = HIGH_VALUE_20_BONUS;
            matchReasons.add("高价值专利超20项(+" + String.format("%.0f%%", patentBonus * 100) + ")");
        }
        bonusTotal += patentBonus;
        log.info("Step3b: 高价值专利数 = {}, 加分 = {}", highValueCount, String.format("%.2f", patentBonus));

        // 3c. 同省加分
        double geoBonus = 0.0;
        String enterpriseProvince = null;
        String nodeProvince = null;
        if (enterprise.getRegionId() != null) {
            Region region = regionMapper.selectById(enterprise.getRegionId());
            if (region != null && region.getCode() != null && region.getCode().length() >= 2) {
                enterpriseProvince = region.getCode().substring(0, 2);
            }
        }
        // 节点的省份获取逻辑待完善（需要通过产业链-区域关联查询）
        // 此处暂时获取企业省份用于日志记录
        log.info("Step3c: 企业省份={}, 地理加分={}", enterpriseProvince, String.format("%.2f", geoBonus));

        // -------------------- Step4: 加权融合 --------------------
        // 标签相似度60% + IPC加分 + 专利加分 + 地理加分
        double finalScore = baseScore * TAG_SIMILARITY_WEIGHT + bonusTotal * 100.0;
        finalScore = Math.min(finalScore, 100.0);
        finalScore = Math.max(finalScore, 0.0);
        log.info("Step4: 加权融合得分 = {}", String.format("%.2f", finalScore));

        // -------------------- Step5: 分级 --------------------
        String matchLevel;
        if (finalScore >= HIGH_MATCH_THRESHOLD) {
            matchLevel = "高";
        } else if (finalScore >= MEDIUM_MATCH_THRESHOLD) {
            matchLevel = "中";
        } else {
            matchLevel = "低";
        }
        log.info("Step5: 匹配等级 = {}", matchLevel);

        // 构建结果
        MatchScoreResult result = new MatchScoreResult();
        result.setEnterpriseId(enterpriseId);
        result.setEnterpriseName(enterprise.getName());
        result.setChainId(chainId);
        result.setChainName(node != null ? node.getNodeName() : null);
        result.setTechMatchScore(BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP));
        result.setOverallScore(BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP));
        result.setMatchLevel(matchLevel);
        result.setMatchDetail(String.join("; ", matchReasons));

        // 持久化结果
        try {
            MatchScoreResult existing = matchScoreResultMapper.selectByEnterpriseAndChain(enterpriseId, chainId);
            if (existing != null) {
                result.setId(existing.getId());
                matchScoreResultMapper.update(result);
            } else {
                matchScoreResultMapper.insert(result);
            }
        } catch (Exception e) {
            log.warn("匹配度结果持久化失败: {}", e.getMessage());
        }

        log.info("=== A3 技术匹配度计算完成: 得分={}, 等级={} ===", finalScore, matchLevel);
        return result;
    }

    @Override
    public List<MatchScoreResult> batchCalculate(List<Long> enterpriseIds, Long chainId) {
        log.info("=== A3 批量匹配度计算: count={}, chainId={} ===", enterpriseIds.size(), chainId);

        List<MatchScoreResult> results = new ArrayList<>();
        for (Long enterpriseId : enterpriseIds) {
            try {
                MatchScoreResult result = calculateMatch(enterpriseId, chainId);
                results.add(result);
            } catch (Exception e) {
                log.error("批量匹配度计算失败: enterpriseId={}, error={}", enterpriseId, e.getMessage());
                results.add(buildZeroResult(enterpriseId, chainId, "计算异常: " + e.getMessage()));
            }
        }

        // 批量持久化
        try {
            List<MatchScoreResult> toInsert = results.stream()
                    .filter(r -> r.getId() == null)
                    .collect(Collectors.toList());
            if (!toInsert.isEmpty()) {
                matchScoreResultMapper.batchInsert(toInsert);
            }
        } catch (Exception e) {
            log.warn("批量持久化匹配度结果失败: {}", e.getMessage());
        }

        log.info("=== A3 批量匹配度计算完成: {} 条结果 ===", results.size());
        return results;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 从企业对象中提取技术标签
     */
    private List<String> extractEnterpriseTags(Enterprise enterprise) {
        List<String> tags = new ArrayList<>();

        // 从经营范围提取关键词（如果有 businessScope 字段）
        String businessScope = null;
        try {
            // 尝试获取 businessScope 字段（通过反射或直接使用）
            java.lang.reflect.Field field = enterprise.getClass().getDeclaredField("businessScope");
            if (field != null) {
                field.setAccessible(true);
                businessScope = (String) field.get(enterprise);
            }
        } catch (Exception e) {
            // 字段不存在，忽略
        }

        if (businessScope != null && !businessScope.isEmpty()) {
            // 按常见分隔符分词
            String[] words = businessScope.split("[，,、\\s]+");
            for (String word : words) {
                String trimmed = word.trim();
                if (trimmed.length() >= 2 && trimmed.length() <= 10) {
                    tags.add(trimmed);
                }
            }
        }

        // 补充：行业名作为标签
        String industry = null;
        try {
            java.lang.reflect.Field field = enterprise.getClass().getDeclaredField("industry");
            if (field != null) {
                field.setAccessible(true);
                industry = (String) field.get(enterprise);
            }
        } catch (Exception e) {
            // 字段不存在，忽略
        }
        if (industry != null && !industry.isEmpty()) {
            tags.add(industry);
        }

        // 补充：企业名称作为关键词
        String name = enterprise.getName();
        if (name != null && !name.isEmpty()) {
            // 从企业名称中提取关键词（简单处理）
            String[] nameWords = name.split("[\\s-]");
            for (String word : nameWords) {
                if (word.length() >= 2) {
                    tags.add(word);
                }
            }
        }

        return tags;
    }

    /**
     * 从产业链节点中提取需求标签
     */
    private List<String> extractNodeDemandTags(IndustryChainNode node) {
        List<String> tags = new ArrayList<>();
        if (node == null) return tags;

        // 节点名称作为标签
        if (node.getNodeName() != null) {
            tags.add(node.getNodeName());
        }
        // 节点层级
        if (node.getLevel() != null) {
            tags.add(node.getLevel());
        }
        // 从描述中提取关键词（简单分词）
        if (node.getDescription() != null && !node.getDescription().isEmpty()) {
            // 按常见分隔符分词
            String[] words = node.getDescription().split("[，,、\\s]+");
            for (String word : words) {
                String trimmed = word.trim();
                if (trimmed.length() >= 2 && trimmed.length() <= 10) {
                    tags.add(trimmed);
                }
            }
        }
        return tags;
    }

    /**
     * 从产业链节点推断关键IPC分类
     * 优先从 ipc_chain_mapping 表查询，无法匹配时返回空集合。
     */
    private Set<String> extractNodeKeyIpc(IndustryChainNode node) {
        Set<String> ipcSet = new LinkedHashSet<>();
        if (node == null) return ipcSet;

        try {
            // 优先通过 IpcChainMapping 表查找该节点关联的IPC
            List<IpcChainMapping> mappings = ipcChainMappingMapper.selectList(null);
            if (mappings != null && node.getId() != null) {
                for (IpcChainMapping mapping : mappings) {
                    if (node.getId().equals(mapping.getNodeId()) && mapping.getIpcCode() != null) {
                        ipcSet.add(mapping.getIpcCode());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("从ipc_chain_mapping表查询IPC失败: {}", e.getMessage());
        }

        // 后备：从节点名/描述中提取技术关键词做模糊匹配
        if (ipcSet.isEmpty() && node.getNodeName() != null) {
            try {
                List<IpcChainMapping> allMappings = ipcChainMappingMapper.selectList(null);
                if (allMappings != null) {
                    String nodeText = (node.getNodeName() != null ? node.getNodeName() : "")
                        + (node.getDescription() != null ? node.getDescription() : "");
                    for (IpcChainMapping mapping : allMappings) {
                        if (mapping.getDescription() != null && nodeText.contains(mapping.getDescription())) {
                            ipcSet.add(mapping.getIpcCode());
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("IPC模糊匹配失败: {}", e.getMessage());
            }
        }

        return ipcSet;
    }

    /**
     * 计算TF（词频）向量
     */
    private Map<String, Double> computeTf(List<String> tags, Set<String> ipcSet, Set<String> vocabulary) {
        Map<String, Double> tf = new LinkedHashMap<>();

        // 统计标签词频
        Map<String, Integer> tagFreq = new HashMap<>();
        for (String tag : tags) {
            tagFreq.merge(tag, 1, Integer::sum);
        }

        int totalTerms = tags.size() + ipcSet.size();
        if (totalTerms == 0) {
            // 全零向量
            for (String term : vocabulary) {
                tf.put(term, 0.0);
            }
            return tf;
        }

        for (String term : vocabulary) {
            double freq = tagFreq.getOrDefault(term, 0) + (ipcSet.contains(term) ? 1 : 0);
            tf.put(term, freq / totalTerms);
        }

        return tf;
    }

    /**
     * 计算两个TF向量的余弦相似度
     */
    private double cosineSimilarity(Map<String, Double> vec1, Map<String, Double> vec2, Set<String> vocabulary) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (String term : vocabulary) {
            double v1 = vec1.getOrDefault(term, 0.0);
            double v2 = vec2.getOrDefault(term, 0.0);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 从区域编码提取省份（前2位）
     */
    private String extractProvince(String regionCode) {
        if (regionCode == null || regionCode.length() < 2) return null;
        return regionCode.substring(0, 2);
    }

    /**
     * 构建零分结果
     */
    private MatchScoreResult buildZeroResult(Long enterpriseId, Long chainId, String detail) {
        MatchScoreResult result = new MatchScoreResult();
        result.setEnterpriseId(enterpriseId);
        result.setChainId(chainId);
        result.setTechMatchScore(BigDecimal.ZERO);
        result.setIndustryFitScore(BigDecimal.ZERO);
        result.setOverallScore(BigDecimal.ZERO);
        result.setMatchLevel("低");
        result.setMatchDetail(detail);
        return result;
    }
}
