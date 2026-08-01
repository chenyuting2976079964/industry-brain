package com.industrybrain.service.impl;

import com.industrybrain.domain.model.IndustryChainNode;
import com.industrybrain.domain.model.MatchScoreResult;
import com.industrybrain.domain.model.Enterprise;
import com.industrybrain.domain.model.Region;
import com.industrybrain.mapper.mysql.EnterpriseMapper;
import com.industrybrain.mapper.mysql.IndustryChainNodeMapper;
import com.industrybrain.mapper.mysql.MatchScoreResultMapper;
import com.industrybrain.mapper.mysql.RegionMapper;
import com.industrybrain.service.IMatchScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 企业匹配度算法实现（三维度评分）
 *
 * 算法流程：
 *   Dimension 1: 业务契合度得分 (权重50%) — 经营范围文本 × 节点需求文本 → 余弦相似度 → 分段映射
 *   Dimension 2: 规模实力得分     (权重35%) — 团队规模得分×20 + 资本实力得分×15 ÷ 35
 *   Dimension 3: 地域协同度得分   (权重15%) — 企业省份 vs 需求方省份 → 省内100/省外60
 *
 * 综合得分 = 业务契合度×0.5 + 规模实力×0.35 + 地域协同度×0.15
 * 取值范围：0~100，保留2位小数
 */
@Service
@ConditionalOnProperty(name = "mysql.enabled", havingValue = "true", matchIfMissing = true)
public class MatchScoreServiceImpl implements IMatchScoreService {

    private static final Logger log = LoggerFactory.getLogger(MatchScoreServiceImpl.class);

    // ==================== 权重常量 ====================
    /** 业务契合度权重 */
    private static final double BUSINESS_FIT_WEIGHT = 0.50;
    /** 规模实力权重 */
    private static final double SCALE_STRENGTH_WEIGHT = 0.35;
    /** 地域协同度权重 */
    private static final double REGIONAL_SYNERGY_WEIGHT = 0.15;

    // ==================== 规模实力子权重 ====================
    /** 团队规模子权重 */
    private static final double TEAM_SCALE_SUB_WEIGHT = 20.0;
    /** 资本实力子权重 */
    private static final double CAPITAL_SUB_WEIGHT = 15.0;
    /** 规模实力总分母 */
    private static final double SCALE_STRENGTH_DENOMINATOR = 35.0;

    // ==================== 分级阈值 ====================
    private static final double HIGH_MATCH_THRESHOLD = 85.0;
    private static final double MEDIUM_MATCH_THRESHOLD = 70.0;

    @Resource
    private EnterpriseMapper enterpriseMapper;

    @Resource(name = "industryChainNodeMapper")
    private IndustryChainNodeMapper chainNodeMapper;

    @Resource
    private MatchScoreResultMapper matchScoreResultMapper;

    @Resource
    private RegionMapper regionMapper;

    @Override
    public MatchScoreResult calculateMatch(Long enterpriseId, Long chainId, String demandProvince) {
        log.info("=== 三维度匹配度计算开始: enterpriseId={}, chainId={}, demandProvince={} ===",
                enterpriseId, chainId, demandProvince);

        // 获取企业信息
        Enterprise enterprise = enterpriseMapper.selectById(enterpriseId);
        if (enterprise == null) {
            log.warn("企业不存在: enterpriseId={}", enterpriseId);
            return buildZeroResult(enterpriseId, chainId, "企业不存在");
        }

        // 获取节点信息
        IndustryChainNode node = chainNodeMapper.selectById(chainId);

        // ---- 维度1: 业务契合度得分 ----
        double businessFitScore = calculateBusinessFit(enterprise, node);
        log.info("维度1: 业务契合度 = {}", String.format("%.2f", businessFitScore));

        // ---- 维度2: 规模实力得分 ----
        double scaleStrengthScore = calculateScaleStrength(enterprise);
        log.info("维度2: 规模实力 = {}", String.format("%.2f", scaleStrengthScore));

        // ---- 维度3: 地域协同度得分 ----
        double regionalSynergyScore = calculateRegionalSynergy(enterprise, demandProvince);
        log.info("维度3: 地域协同度 = {}", String.format("%.2f", regionalSynergyScore));

        // ---- 综合得分 ----
        double comprehensiveScore = businessFitScore * BUSINESS_FIT_WEIGHT
                                  + scaleStrengthScore * SCALE_STRENGTH_WEIGHT
                                  + regionalSynergyScore * REGIONAL_SYNERGY_WEIGHT;
        comprehensiveScore = Math.min(comprehensiveScore, 100.0);
        comprehensiveScore = Math.max(comprehensiveScore, 0.0);
        log.info("综合得分 = {}×0.5 + {}×0.35 + {}×0.15 = {}",
                String.format("%.2f", businessFitScore),
                String.format("%.2f", scaleStrengthScore),
                String.format("%.2f", regionalSynergyScore),
                String.format("%.2f", comprehensiveScore));

        // ---- 分级 ----
        String matchLevel;
        if (comprehensiveScore >= HIGH_MATCH_THRESHOLD) {
            matchLevel = "高";
        } else if (comprehensiveScore >= MEDIUM_MATCH_THRESHOLD) {
            matchLevel = "中";
        } else {
            matchLevel = "低";
        }

        // ---- 构建匹配理由 ----
        List<String> reasons = new ArrayList<>();
        reasons.add("业务契合度=" + String.format("%.0f", businessFitScore));
        reasons.add("规模实力=" + String.format("%.0f", scaleStrengthScore));
        reasons.add("地域协同度=" + String.format("%.0f", regionalSynergyScore));

        // 构建结果
        MatchScoreResult result = new MatchScoreResult();
        result.setEnterpriseId(enterpriseId);
        result.setEnterpriseName(enterprise.getName());
        result.setChainId(chainId);
        result.setChainName(node != null ? node.getNodeName() : null);
        result.setBusinessFitScore(BigDecimal.valueOf(businessFitScore).setScale(2, RoundingMode.HALF_UP));
        result.setScaleStrengthScore(BigDecimal.valueOf(scaleStrengthScore).setScale(2, RoundingMode.HALF_UP));
        result.setRegionalSynergyScore(BigDecimal.valueOf(regionalSynergyScore).setScale(2, RoundingMode.HALF_UP));
        result.setOverallScore(BigDecimal.valueOf(comprehensiveScore).setScale(2, RoundingMode.HALF_UP));
        result.setMatchLevel(matchLevel);
        result.setMatchDetail(String.join("; ", reasons));

        // 持久化
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

        log.info("=== 三维度匹配度计算完成: 综合={}, 等级={} ===",
                String.format("%.2f", comprehensiveScore), matchLevel);
        return result;
    }

    @Override
    public List<MatchScoreResult> batchCalculate(List<Long> enterpriseIds, Long chainId, String demandProvince) {
        log.info("=== 三维度批量匹配度计算: count={}, chainId={} ===", enterpriseIds.size(), chainId);

        List<MatchScoreResult> results = new ArrayList<>();
        for (Long enterpriseId : enterpriseIds) {
            try {
                MatchScoreResult result = calculateMatch(enterpriseId, chainId, demandProvince);
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

        log.info("=== 三维度批量匹配度计算完成: {} 条结果 ===", results.size());
        return results;
    }

    // ==================== 维度1: 业务契合度 ====================

    /**
     * 计算业务契合度得分
     * 输入：企业经营范围文本、补链节点需求文本
     * 通过文本相似度计算 Sim 值 → 分段映射到得分
     */
    private double calculateBusinessFit(Enterprise enterprise, IndustryChainNode node) {
        // 获取企业经营范围文本
        String businessScope = enterprise.getBusinessScope();

        // 兜底：经营范围为空
        if (businessScope == null || businessScope.trim().isEmpty()) {
            log.debug("企业经营范围为空，Sim=0");
            return 0.0;
        }

        // 获取节点需求文本
        String nodeDemandText = buildNodeDemandText(node);
        if (nodeDemandText.isEmpty()) {
            log.debug("节点需求文本为空，Sim=0");
            return 0.0;
        }

        // 计算文本相似度
        double sim;
        try {
            sim = computeTextSimilarity(businessScope, nodeDemandText);
        } catch (Exception e) {
            log.warn("文本相似度计算异常: {}", e.getMessage());
            sim = 0.0; // 兜底
        }

        log.debug("业务契合度 Sim = {}", String.format("%.4f", sim));

        // 分段映射
        if (sim >= 0.8) {
            return 100.0;
        } else if (sim >= 0.5) {
            return 75.0;
        } else if (sim >= 0.2) {
            return 40.0;
        } else {
            return 0.0;
        }
    }

    /**
     * 构建节点需求文本（节点名称 + 描述）
     */
    private String buildNodeDemandText(IndustryChainNode node) {
        if (node == null) return "";
        StringBuilder sb = new StringBuilder();
        if (node.getNodeName() != null) {
            sb.append(node.getNodeName());
        }
        if (node.getDescription() != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(node.getDescription());
        }
        return sb.toString();
    }

    /**
     * 计算两段文本的词级TF余弦相似度
     */
    private double computeTextSimilarity(String text1, String text2) {
        // 分词
        List<String> words1 = tokenize(text1);
        List<String> words2 = tokenize(text2);

        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0;
        }

        // 构建词袋
        Set<String> vocabulary = new LinkedHashSet<>();
        vocabulary.addAll(words1);
        vocabulary.addAll(words2);

        // 计算TF向量
        Map<String, Double> tf1 = computeTf(words1, vocabulary);
        Map<String, Double> tf2 = computeTf(words2, vocabulary);

        // 余弦相似度
        return cosineSimilarity(tf1, tf2, vocabulary);
    }

    /**
     * 中文文本分词（按分隔符切分）
     */
    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return tokens;

        // 按中文和常见分隔符切分
        String[] words = text.split("[，,、；;。\\s]+");
        for (String word : words) {
            String trimmed = word.trim();
            if (trimmed.length() >= 2 && trimmed.length() <= 15) {
                tokens.add(trimmed);
            }
        }

        // 对于长文本，补充2-gram子串提升匹配
        if (text.length() > 2) {
            for (int i = 0; i < text.length() - 1; i++) {
                String bigram = text.substring(i, Math.min(i + 2, text.length()));
                if (bigram.length() == 2 && !bigram.matches("[，,、；;。\\s]+")) {
                    tokens.add(bigram);
                }
            }
        }

        return tokens;
    }

    /**
     * 计算词频（TF）向量
     */
    private Map<String, Double> computeTf(List<String> words, Set<String> vocabulary) {
        Map<String, Double> tf = new LinkedHashMap<>();
        Map<String, Integer> freq = new HashMap<>();
        for (String word : words) {
            freq.merge(word, 1, Integer::sum);
        }

        int total = words.size();
        for (String term : vocabulary) {
            tf.put(term, total > 0 ? (double) freq.getOrDefault(term, 0) / total : 0.0);
        }
        return tf;
    }

    /**
     * 计算两个TF向量的余弦相似度
     */
    private double cosineSimilarity(Map<String, Double> vec1, Map<String, Double> vec2, Set<String> vocabulary) {
        double dotProduct = 0.0, norm1 = 0.0, norm2 = 0.0;

        for (String term : vocabulary) {
            double v1 = vec1.getOrDefault(term, 0.0);
            double v2 = vec2.getOrDefault(term, 0.0);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        if (norm1 == 0.0 || norm2 == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    // ==================== 维度2: 规模实力 ====================

    /**
     * 计算规模实力得分
     * 公式：(团队规模得分 × 20 + 资本实力得分 × 15) ÷ 35
     */
    private double calculateScaleStrength(Enterprise enterprise) {
        double teamScore = calculateTeamScaleScore(enterprise.getEmployeeScale());
        double capitalScore = calculateCapitalScore(enterprise.getRegisteredCapital());

        double scaleStrength = (teamScore * TEAM_SCALE_SUB_WEIGHT + capitalScore * CAPITAL_SUB_WEIGHT)
                / SCALE_STRENGTH_DENOMINATOR;

        log.debug("规模实力: 团队规模={}×20 + 资本实力={}×15 → {}",
                String.format("%.0f", teamScore), String.format("%.0f", capitalScore),
                String.format("%.2f", scaleStrength));

        return Math.min(scaleStrength, 100.0);
    }

    /**
     * 团队规模得分
     */
    private double calculateTeamScaleScore(String employeeScale) {
        if (employeeScale == null || employeeScale.trim().isEmpty()) {
            return 50.0; // 兜底：小型
        }

        String scale = employeeScale.trim();
        switch (scale) {
            case "大型": return 100.0;
            case "中大型": return 90.0;
            case "中型": return 80.0;
            case "中小型": return 65.0;
            case "小型": return 50.0;
            default: return 50.0; // 未知统一按小型
        }
    }

    /**
     * 资本实力得分（基于注册资本，单位：元）
     */
    private double calculateCapitalScore(String registeredCapitalStr) {
        double capitalYuan = parseCapitalToYuan(registeredCapitalStr);

        if (capitalYuan >= 10_000_000) { // ≥1000万
            return 100.0;
        } else if (capitalYuan >= 1_000_000) { // 100万~1000万
            return 80.0;
        } else {
            return 60.0; // <100万 或 null
        }
    }

    /**
     * 解析注册资本字符串为元数值
     * 支持: "10000万元", "5000万", "1亿", "1000.5万", "10000000", null
     */
    private double parseCapitalToYuan(String capitalStr) {
        if (capitalStr == null || capitalStr.trim().isEmpty()) {
            return 0.0;
        }
        try {
            String cleaned = capitalStr.trim();
            if (cleaned.contains("亿")) {
                return Double.parseDouble(cleaned.replace("亿", "").replace("元", "").trim()) * 100_000_000;
            }
            // "万元"
            if (cleaned.contains("万元")) {
                return Double.parseDouble(cleaned.replace("万元", "").trim()) * 10_000;
            }
            // "万"
            if (cleaned.contains("万")) {
                return Double.parseDouble(cleaned.replace("万", "").replace("元", "").trim()) * 10_000;
            }
            // "元" 或纯数字
            cleaned = cleaned.replace("元", "").trim();
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            log.debug("注册资本解析失败: {}, 按0处理", capitalStr);
            return 0.0;
        }
    }

    // ==================== 维度3: 地域协同度 ====================

    /**
     * 计算地域协同度得分
     * 企业省份 vs 需求方省份 → 省内100 / 省外60
     */
    private double calculateRegionalSynergy(Enterprise enterprise, String demandProvince) {
        // 获取企业省份编码（region.code前2位）
        String enterpriseProvince = getEnterpriseProvince(enterprise);

        // 兜底：企业省份为空 → 按省外60
        if (enterpriseProvince == null || enterpriseProvince.isEmpty()) {
            log.debug("企业省份为空，按省外处理");
            return 60.0;
        }

        // 需求方省份为空 → 按省外60
        if (demandProvince == null || demandProvince.trim().isEmpty()) {
            log.debug("需求方省份为空，按省外处理");
            return 60.0;
        }

        if (enterpriseProvince.equals(demandProvince.trim())) {
            return 100.0;
        } else {
            return 60.0;
        }
    }

    /**
     * 从企业 regionId 提取省份编码（前2位）
     */
    private String getEnterpriseProvince(Enterprise enterprise) {
        if (enterprise.getRegionId() == null) return null;
        try {
            Region region = regionMapper.selectById(enterprise.getRegionId());
            if (region != null && region.getCode() != null && region.getCode().length() >= 2) {
                return region.getCode().substring(0, 2);
            }
        } catch (Exception e) {
            log.debug("查询企业区域失败: regionId={}, error={}", enterprise.getRegionId(), e.getMessage());
        }
        return null;
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建零分结果
     */
    private MatchScoreResult buildZeroResult(Long enterpriseId, Long chainId, String detail) {
        MatchScoreResult result = new MatchScoreResult();
        result.setEnterpriseId(enterpriseId);
        result.setChainId(chainId);
        result.setBusinessFitScore(BigDecimal.ZERO);
        result.setRegionalSynergyScore(BigDecimal.ZERO);
        result.setOverallScore(BigDecimal.ZERO);
        result.setMatchLevel("低");
        result.setMatchDetail(detail);
        return result;
    }
}
