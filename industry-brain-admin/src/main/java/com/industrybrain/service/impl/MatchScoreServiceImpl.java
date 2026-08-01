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

    // ==================== 维度1: 业务契合度（关键词子串命中算法） ====================

    /**
     * 计算业务契合度得分
     * 新算法：关键词子串命中匹配
     *
     * 1. 解析经营范围为业务项列表（按分隔符拆分，去掉"许可项目："等前缀）
     * 2. 从节点名称生成关键词集（2-gram + 单字 + 全称本身）
     * 3. 计算每条关键词是否在任一业务项中出现（子串包含）
     * 4. 命中率 = 命中数 / 总关键词数
     * 5. 命中率分段映射到得分
     */
    private double calculateBusinessFit(Enterprise enterprise, IndustryChainNode node) {
        // 取description列（实际存储经营范围文本）或businessScope扩展字段
        String businessScope = enterprise.getDescription();
        if (businessScope == null || businessScope.trim().isEmpty()) {
            businessScope = enterprise.getBusinessScope();
        }
        if (businessScope == null || businessScope.trim().isEmpty()) {
            log.debug("企业经营范围为空，命中率=0");
            return 0.0;
        }

        String nodeName = node != null ? node.getNodeName() : null;
        if (nodeName == null || nodeName.trim().isEmpty()) {
            log.debug("节点名称为空，命中率=0");
            return 0.0;
        }
        nodeName = nodeName.trim();

        // 1. 解析经营范围为业务项列表
        List<String> businessItems = parseBusinessItems(businessScope);

        // 2. 从节点名称生成关键词集
        Set<String> keywords = generateNodeKeywords(nodeName);

        if (keywords.isEmpty() || businessItems.isEmpty()) {
            return 0.0;
        }

        // 3. 计算命中率
        int hitCount = 0;
        for (String kw : keywords) {
            for (String item : businessItems) {
                if (item.contains(kw)) {
                    hitCount++;
                    break; // 该关键词命中，跳到下一个关键词
                }
            }
        }
        double hitRate = (double) hitCount / keywords.size();

        log.debug("业务契合度: node={}, 业务项={}个, 关键词={}个, 命中={}个, 命中率={}",
                nodeName, businessItems.size(), keywords.size(), hitCount, String.format("%.2f", hitRate));

        // 4. 分段映射到得分
        if (hitRate >= 0.6) {
            return 100.0;
        } else if (hitRate >= 0.3) {
            return 75.0;
        } else if (hitRate > 0) {
            return 40.0;
        } else {
            return 0.0;
        }
    }

    /**
     * 解析经营范围为业务项列表
     * 处理结构: "许可项目：A；B；C 一般项目：D；E；F"
     */
    private List<String> parseBusinessItems(String businessScope) {
        List<String> items = new ArrayList<>();
        if (businessScope == null || businessScope.trim().isEmpty()) return items;

        // 去掉"许可项目："和"一般项目："等前缀标签
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

    /**
     * 从节点名称生成关键词集
     * 包含: 2-gram子串 + 单字 + 全称本身
     * 例: "电力设备" → ["电力","设备","力设","电","力","设","备","电力设备"]
     */
    private Set<String> generateNodeKeywords(String nodeName) {
        Set<String> keywords = new LinkedHashSet<>();
        if (nodeName == null || nodeName.trim().isEmpty()) return keywords;

        String name = nodeName.trim();
        // 全称
        keywords.add(name);

        // 2-gram
        for (int i = 0; i < name.length() - 1; i++) {
            keywords.add(name.substring(i, i + 2));
        }

        // 单字
        for (char c : name.toCharArray()) {
            keywords.add(String.valueOf(c));
        }

        // 去除纯标点/空白关键词
        keywords.removeIf(kw -> kw.matches("[\\s，,、；;。]+"));

        return keywords;
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
