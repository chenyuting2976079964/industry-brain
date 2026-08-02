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

    // ==================== 维度1: 业务契合度（加权连续评分算法） ====================

    /**
     * 计算业务契合度得分（三维度差异化连续评分）
     *
     * 核心思路：经营范围越聚焦于目标节点，得分越高。
     * 一家经营范围只有"电力设备"相关3项的公司，比一家有30项但只1项涉及"电力设备"的公司更适合招商。
     *
     * 三维度：
     *   A. 全称命中得分 (0~50)：
     *      - 任一业务项包含完整节点名 → 50分
     *      - 否则 2-gram加权命中率 × 50分
     *      - 区分完全匹配和部分匹配的企业
     *
     *   B. 聚焦度得分 (0~35)：
     *      - 命中业务项数 / max(总业务项数, 3)
     *      - 分母至少为3，防止"只有1项且命中"的公司得满分
     *      - 聚焦型企业（少项多命中）得分远高于宽泛型企业（多项少命中）
     *
     *   C. 匹配精度得分 (0~15)：
     *      - 平均(节点名长度 / 命中项长度) for 所有命中业务项
     *      - 节点名占业务项比例越高，匹配越精准
     *      - 避免"电力设备XXXYYYZZZ"这种长项与"电力设备"短项等同
     *
     * 最终得分 = A + B + C（0~100连续值，各企业互不相同）
     */
    private double calculateBusinessFit(Enterprise enterprise, IndustryChainNode node) {
        String businessScope = enterprise.getDescription();
        if (businessScope == null || businessScope.trim().isEmpty()) {
            businessScope = enterprise.getBusinessScope();
        }
        if (businessScope == null || businessScope.trim().isEmpty()) {
            return 0.0;
        }

        String nodeName = node != null ? node.getNodeName() : null;
        if (nodeName == null || nodeName.trim().isEmpty()) {
            return 0.0;
        }
        nodeName = nodeName.trim();

        // 1. 解析经营范围为业务项列表
        List<String> businessItems = parseBusinessItems(businessScope);
        if (businessItems.isEmpty()) return 0.0;

        // 2. 生成加权关键词集
        Map<String, Integer> weightedKeywords = generateWeightedKeywords(nodeName);
        if (weightedKeywords.isEmpty()) return 0.0;

        int totalItems = businessItems.size();

        // 3. 匹配分析
        boolean fullNameMatched = false;
        int totalWeight = 0, hitWeight = 0;
        Set<String> hitKeywordSet = new LinkedHashSet<>();
        Set<String> matchedItemSet = new LinkedHashSet<>();

        for (Map.Entry<String, Integer> kw : weightedKeywords.entrySet()) {
            totalWeight += kw.getValue();
            boolean keywordHit = false;
            for (String item : businessItems) {
                if (item.contains(kw.getKey())) {
                    keywordHit = true;
                    matchedItemSet.add(item);
                    // 全称（权重3）命中 → fullNameMatched
                    if (kw.getValue() == 3 && kw.getKey().equals(nodeName)) {
                        fullNameMatched = true;
                    }
                }
            }
            if (keywordHit) {
                hitWeight += kw.getValue();
                hitKeywordSet.add(kw.getKey());
            }
        }

        int matchedItems = matchedItemSet.size();

        // === A. 全称命中得分 (0~50) ===
        double fullNameScore;
        if (fullNameMatched) {
            fullNameScore = 60.0;
        } else {
            double hitRate = totalWeight > 0 ? (double) hitWeight / totalWeight : 0;
            fullNameScore = hitRate * 60.0;
        }

        // === B. 聚焦度得分 (0~35) ===
        // 命中项数 / max(总项数, 3)，最少分母3防止1/1=100%
        double focusRatio = (double) matchedItems / Math.max(totalItems, 3);
        double focusScore = Math.min(focusRatio, 1.0) * 35.0;

        // === C. 匹配精度得分 (0~15) ===
        // 节点名越占业务项的主要部分，匹配越精准
        double totalPrecision = 0;
        int precisionCount = 0;
        for (String item : matchedItemSet) {
            if (item.length() > 0) {
                totalPrecision += (double) nodeName.length() / item.length();
                precisionCount++;
            }
        }
        double avgPrecision = precisionCount > 0 ? totalPrecision / precisionCount : 0;
        double precisionScore = Math.min(avgPrecision, 1.0) * 15.0;

        // === 综合得分（上限99，避免100满分） ===
        double score = fullNameScore + focusScore + precisionScore;
        score = Math.min(90.0, Math.max(0.0, score));

        log.debug("业务契合度: node={}, 业务项={}(命中{}), fullName={}, "
                + "全称得分={}, 聚焦度={}({}/{}), 精度={}, 最终={}",
                nodeName, totalItems, matchedItems, fullNameMatched,
                String.format("%.1f", fullNameScore), String.format("%.1f", focusScore),
                matchedItems, totalItems, String.format("%.1f", precisionScore),
                String.format("%.1f", score));

        return score;
    }

    /**
     * 从节点名称生成加权关键词集
     * 全称匹配权重3，2-gram匹配权重1
     * 例: "电力设备" → {"电力设备":3, "电力":1, "力设":1}  总权重=5
     */
    private Map<String, Integer> generateWeightedKeywords(String nodeName) {
        Map<String, Integer> keywords = new LinkedHashMap<>();
        if (nodeName == null || nodeName.trim().isEmpty()) return keywords;

        String name = nodeName.trim();
        // 全称权重3（2字以上才有意义）
        if (name.length() >= 2) {
            keywords.put(name, 3);
        }

        // 2-gram权重1
        for (int i = 0; i < name.length() - 1; i++) {
            String bigram = name.substring(i, i + 2);
            if (!bigram.matches("[\\s，,、；;。]+")) {
                keywords.merge(bigram, 1, Integer::sum);
            }
        }

        return keywords;
    }

    /**
     * 解析经营范围为业务项列表
     * 处理结构: "许可项目：A；B；C 一般项目：D；E；F"
     */
    private List<String> parseBusinessItems(String businessScope) {
        List<String> items = new ArrayList<>();
        if (businessScope == null || businessScope.trim().isEmpty()) return items;

        // 去掉前缀标签
        String cleaned = businessScope
                .replace("许可项目：", "；")
                .replace("许可项目:", "；")
                .replace("一般项目：", "；")
                .replace("一般项目:", "；")
                .replace("（除许可业务外，可自主依法经营法律法规非禁止或限制的项目）", "")
                .replace("(除许可业务外，可自主依法经营法律法规非禁止或限制的项目)", "")
                .replace("（依法须经批准的项目，经相关部门批准后方可开展经营活动）", "")
                .replace("(依法须经批准的项目，经相关部门批准后方可开展经营活动)", "")
                .replace("（依法需经批准的项目经相关部门批准后方可经营）", "")
                .replace("（依法须经批准的项目，经相关部门批准后方可开展经营活动，具体经营项目以相关部门批准文件或许可证件为准）", "");

        // 按分隔符拆分为业务项
        String[] parts = cleaned.split("[；;，,。]");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.length() >= 2 && trimmed.length() <= 50) {
                items.add(trimmed);
            }
        }

        return items;
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

    /** 马鞍山区域编码前缀 */
    private static final String MAANSHAN_CITY = "3405";
    private static final String ANHUI_PROVINCE = "34";
    /** 邻省编码: 江苏32 浙江33 湖北42 河南41 江西36 山东37 */
    private static final Set<String> NEIGHBOR_PROVINCES = Set.of("32", "33", "42", "41", "36", "37");

    /**
     * 计算地域协同度得分（距离梯度）
     * 基于企业与马鞍山(340500)的距离分级:
     *   马鞍山本市(3405) → 100分
     *   安徽省内(34) → 80分
     *   邻省(苏浙鄂豫赣鲁) → 60分
     *   其他省份 → 40分
     *   未知 → 40分(兜底)
     */
    private double calculateRegionalSynergy(Enterprise enterprise, String demandProvince) {
        String enterpriseCity = getEnterpriseCityCode(enterprise);

        // 兜底：未知 → 40
        if (enterpriseCity == null || enterpriseCity.isEmpty()) {
            log.debug("企业城市编码为空，按其他省份处理");
            return 40.0;
        }

        // 本市: 马鞍山3405
        if (enterpriseCity.startsWith(MAANSHAN_CITY)) {
            return 100.0;
        }

        // 本省: 安徽34
        if (enterpriseCity.startsWith(ANHUI_PROVINCE)) {
            return 80.0;
        }

        // 邻省
        String province = enterpriseCity.length() >= 2 ? enterpriseCity.substring(0, 2) : "";
        if (NEIGHBOR_PROVINCES.contains(province)) {
            return 60.0;
        }

        // 其他省份
        return 40.0;
    }

    /**
     * 从企业 regionId 提取城市编码（前4位）
     */
    private String getEnterpriseCityCode(Enterprise enterprise) {
        if (enterprise.getRegionId() == null) return null;
        try {
            Region region = regionMapper.selectById(enterprise.getRegionId());
            if (region != null && region.getCode() != null && region.getCode().length() >= 4) {
                return region.getCode().substring(0, 4);
            }
        } catch (Exception e) {
            log.debug("查询企业区域失败: regionId={}, error={}", enterprise.getRegionId(), e.getMessage());
        }
        return null;
    }

    /**
     * 从区域编码提取省份编码（前2位），供外部调用
     */
    private String getEnterpriseProvince(Enterprise enterprise) {
        String cityCode = getEnterpriseCityCode(enterprise);
        return cityCode != null && cityCode.length() >= 2 ? cityCode.substring(0, 2) : null;
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
