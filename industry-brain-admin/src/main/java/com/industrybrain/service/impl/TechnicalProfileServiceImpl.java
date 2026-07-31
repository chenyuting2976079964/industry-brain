package com.industrybrain.service.impl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.industrybrain.domain.model.Enterprise;
import com.industrybrain.domain.model.Patent;
import com.industrybrain.domain.model.IpcChainMapping;
import com.industrybrain.dto.response.TechnicalProfileResponse;
import com.industrybrain.dto.response.TechnicalProfileResponse.AdvantageField;
import com.industrybrain.dto.response.TechnicalProfileResponse.TechnologyField;
import com.industrybrain.mapper.elasticsearch.PatentSearchMapper;
import com.industrybrain.mapper.mysql.EnterpriseMapper;
import com.industrybrain.mapper.mysql.PatentMapper;
import com.industrybrain.mapper.mysql.IpcChainMappingMapper;
import com.industrybrain.service.ITechnicalProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 企业技术画像算法实现类（算法A4）
 *
 * 算法流程：
 *   Step1: IPC分布统计 — 查patent表按IPC大类聚合
 *   Step2: 行业基准对比 — 聚合同行业企业IPC分布，计算行业均值
 *   Step3: 优势识别 — 专利数 > 行业均值×1.5 → 标记'优势领域'，取TOP5
 *   Step4: 短板识别 — 产业链关键IPC中专利数<3件 → 标记'短板'
 *   Step5: 方向推断 — 近3年新增IPC + 增长率>30%的领域 → '研发方向'
 *   Step6: 标签化输出
 */
@Service
@ConditionalOnProperty(name = "mysql.enabled", havingValue = "true", matchIfMissing = true)
public class TechnicalProfileServiceImpl implements ITechnicalProfileService {

    private static final Logger log = LoggerFactory.getLogger(TechnicalProfileServiceImpl.class);

    /** 优势识别：企业专利数 > 行业均值 × 倍数阈值 */
    private static final double ADVANTAGE_MULTIPLIER = 1.5;
    /** 短板识别：专利数少于此值标记为短板 */
    private static final int WEAKNESS_THRESHOLD = 3;
    /** 研发方向：增长率阈值 */
    private static final double GROWTH_RATE_THRESHOLD = 0.30;
    /** 研发方向：近N年 */
    private static final int RECENT_YEARS = 3;
    /** 优势领域取TOP N */
    private static final int TOP_ADVANTAGES = 5;

    @Resource
    private EnterpriseMapper enterpriseMapper;

    @Resource
    private PatentMapper patentMapper;

    @Resource
    private PatentSearchMapper patentSearchMapper;

    @Resource
    private IpcChainMappingMapper ipcChainMappingMapper;  // IPC->产业链映射规则表

    @Override
    public TechnicalProfileResponse generateProfile(Long enterpriseId) {
        log.info("=== A4 企业技术画像生成开始: enterpriseId={} ===", enterpriseId);

        // 获取企业信息
        Enterprise enterprise = enterpriseMapper.selectById(enterpriseId);
        if (enterprise == null) {
            log.warn("企业不存在: enterpriseId={}", enterpriseId);
            return new TechnicalProfileResponse();
        }

        TechnicalProfileResponse response = new TechnicalProfileResponse();
        response.setEnterpriseId(enterpriseId);
        response.setEnterpriseName(enterprise.getName());

        // -------------------- Step1: IPC分布统计 --------------------
        List<Map<String, Object>> ipcDistribution = patentMapper.selectIpcDistribution(enterpriseId);
        List<com.industrybrain.domain.model.Patent> allPatents = patentMapper.selectByEnterpriseId(enterpriseId);

        int totalPatents = allPatents.size();
        response.setTotalPatents(totalPatents);

        // 有效专利数（法律状态=有效）
        int validPatents = (int) allPatents.stream()
                .filter(p -> "0".equals(p.getLegalStatus()) || "有效".equals(p.getLegalStatus()))
                .count();
        response.setValidPatents(validPatents);

        log.info("Step1: IPC分布统计完成, 总专利={}, 有效专利={}, IPC分类数={}",
                totalPatents, validPatents, ipcDistribution.size());

        // 构建 technologyFields 列表
        List<TechnologyField> techFields = new ArrayList<>();
        for (Map<String, Object> row : ipcDistribution) {
            TechnologyField field = new TechnologyField();
            field.setIpcCode((String) row.get("ipcCode"));
            field.setPatentCount(((Number) row.get("cnt")).intValue());

            // 查找该IPC最新的专利日期
            String latestDate = allPatents.stream()
                    .filter(p -> field.getIpcCode() != null && field.getIpcCode().equals(p.getIpcCode()))
                    .map(Patent::getApplicationDate)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .map(d -> d.toString())
                    .orElse(null);
            field.setLatestPatentDate(latestDate);
            field.setFieldName(ipcToFieldName(field.getIpcCode()));
            techFields.add(field);
        }
        response.setTechnologyFields(techFields);

        // -------------------- Step2: 行业基准对比 --------------------
        String industry = enterprise.getIndustry();
        Map<String, Double> industryIpcAvg = buildIndustryBenchmark(industry, ipcDistribution);
        log.info("Step2: 行业基准构建完成, 行业={}, 基准IPC数={}", industry, industryIpcAvg.size());

        // 行业整体统计
        Map<String, Object> industryStats = patentMapper.selectIndustryPatentAvg(industry);
        Double industryAvgPatent = industryStats != null && industryStats.get("avgPatentCount") != null
                ? ((Number) industryStats.get("avgPatentCount")).doubleValue() : 0.0;
        log.info("Step2: 行业平均专利数 = {}", String.format("%.2f", industryAvgPatent));

        // -------------------- Step3: 优势识别 --------------------
        // 专利数 > 行业均值 × 1.5 → 标记'优势领域'，取TOP5
        List<AdvantageField> advantages = new ArrayList<>();
        for (Map<String, Object> row : ipcDistribution) {
            String ipcCode = (String) row.get("ipcCode");
            int enterpriseCount = ((Number) row.get("cnt")).intValue();
            Double industryAvg = industryIpcAvg.getOrDefault(ipcCode, 0.0);

            if (industryAvg > 0 && enterpriseCount > industryAvg * ADVANTAGE_MULTIPLIER) {
                AdvantageField adv = new AdvantageField();
                adv.setFieldName(ipcToFieldName(ipcCode));
                adv.setIpcCode(ipcCode);
                adv.setEnterprisePatentCount(enterpriseCount);
                adv.setIndustryAvg(Math.round(industryAvg * 100.0) / 100.0);
                adv.setRatio(Math.round(enterpriseCount / Math.max(industryAvg, 0.01) * 100.0) / 100.0);
                advantages.add(adv);
            }
        }
        // 按比例降序排列取TOP5
        advantages.sort((a, b) -> Double.compare(b.getRatio(), a.getRatio()));
        if (advantages.size() > TOP_ADVANTAGES) {
            advantages = new ArrayList<>(advantages.subList(0, TOP_ADVANTAGES));
        }
        response.setAdvantages(advantages);
        log.info("Step3: 优势识别完成, 优势领域数={}", advantages.size());

        // -------------------- Step4: 短板识别 --------------------
        // 产业链关键IPC中专利数<3件 → 标记'短板'
        // 关键IPC从产业通用技术领域获取
        Set<String> keyIpcSet = getKeyIndustryIpc(industry);
        List<String> weaknesses = new ArrayList<>();
        for (String keyIpc : keyIpcSet) {
            int count = patentMapper.countByIpcAndEnterprise(keyIpc, enterpriseId);
            if (count < WEAKNESS_THRESHOLD) {
                weaknesses.add(ipcToFieldName(keyIpc) + "(" + keyIpc + "): 仅" + count + "件");
            }
        }
        response.setWeaknesses(weaknesses);
        log.info("Step4: 短板识别完成, 短板数={}", weaknesses.size());

        // -------------------- Step5: 方向推断 --------------------
        // 近3年新增IPC + 增长率>30%的领域 → '研发方向'
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        String sinceYear = (currentYear - RECENT_YEARS) + "-01-01";
        List<String> recentIpcCodes = patentMapper.selectRecentIpcCodes(enterpriseId, sinceYear);

        // 统计近3年各IPC专利数 vs 3年前
        Map<String, Long> recentIpcCount = new HashMap<>();
        for (String ipc : recentIpcCodes) {
            recentIpcCount.merge(ipc, 1L, Long::sum);
        }

        // 统计历史IPC专利数
        Map<String, Long> historyIpcCount = new HashMap<>();
        for (com.industrybrain.domain.model.Patent patent : allPatents) {
            if (patent.getIpcCode() != null) {
                historyIpcCount.merge(patent.getIpcCode(), 1L, Long::sum);
            }
        }

        List<String> rdDirections = new ArrayList<>();
        for (Map.Entry<String, Long> entry : recentIpcCount.entrySet()) {
            String ipc = entry.getKey();
            long recentCount = entry.getValue();
            long historyCount = historyIpcCount.getOrDefault(ipc, 1L);

            // 检查是否为近3年新增的IPC，或增长率>30%
            long olderCount = historyCount - recentCount;
            if (olderCount <= 0) {
                // 全新领域
                rdDirections.add(ipcToFieldName(ipc) + "(新兴,近3年" + recentCount + "件)");
            } else if (olderCount > 0 && (double) recentCount / olderCount > GROWTH_RATE_THRESHOLD) {
                // 快速增长
                double growth = (double) recentCount / olderCount * 100;
                rdDirections.add(ipcToFieldName(ipc) + "(增长" + String.format("%.0f%%", growth) + ")");
            }
        }
        response.setRdDirections(rdDirections);
        log.info("Step5: 方向推断完成, 研发方向数={}", rdDirections.size());

        // -------------------- Step6: 标签化输出 --------------------
        // 研发能力评级
        String rdLevel = evaluateRdLevel(totalPatents, validPatents, advantages.size());
        response.setRdCapabilityLevel(rdLevel);

        // 技术新颖度评分（基于近3年新增IPC占比）
        double noveltyRatio = allPatents.size() > 0
                ? (double) recentIpcCodes.size() / allPatents.size() : 0.0;
        BigDecimal noveltyScore = BigDecimal.valueOf(Math.min(noveltyRatio * 100, 100))
                .setScale(2, RoundingMode.HALF_UP);
        response.setNoveltyScore(noveltyScore);

        // 技术影响力评分（基于高价值专利占比和被引次数）
        long highValueCount = patentMapper.selectHighValueCountByEnterprise(enterpriseId);
        int totalCitations = allPatents.stream()
                .mapToInt(p -> p.getCitationCount() != null ? p.getCitationCount() : 0)
                .sum();
        double influenceRatio = totalPatents > 0
                ? (highValueCount * 0.7 / totalPatents + Math.min(totalCitations / (double) Math.max(totalPatents, 1) / 10.0, 0.3))
                : 0.0;
        BigDecimal influenceScore = BigDecimal.valueOf(Math.min(influenceRatio * 100, 100))
                .setScale(2, RoundingMode.HALF_UP);
        response.setInfluenceScore(influenceScore);

        log.info("=== A4 企业技术画像生成完成: enterprise={}, 评级={}, 新颖度={}, 影响力={} ===",
                enterprise.getName(), rdLevel, noveltyScore, influenceScore);

        return response;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建行业IPC基准分布
     * 从同行业企业汇总IPC分布，计算每个IPC的行业均值
     */
    private Map<String, Double> buildIndustryBenchmark(String industry, List<Map<String, Object>> enterpriseIpcDist) {
        Map<String, Double> benchmark = new LinkedHashMap<>();

        if (industry == null || industry.isEmpty()) {
            return benchmark;
        }

        try {
            // 获取同行业IPC分布（TOP企业聚合）
            List<Map<String, Object>> industryIpcList = patentMapper.selectIndustryBenchmarkIpc(industry, 50);

            if (!industryIpcList.isEmpty()) {
                // 计算每个IPC的平均专利数
                for (Map<String, Object> row : industryIpcList) {
                    String ipcCode = (String) row.get("ipcCode");
                    long totalCnt = ((Number) row.get("cnt")).longValue();
                    // 粗略估算：总专利数 / 企业数(用当前企业IPC分布数作为参考)
                    // 这里用TOP50企业作为样本
                    double avgCnt = totalCnt / Math.max(industryIpcList.size() / 5.0, 1.0);
                    benchmark.put(ipcCode, avgCnt);
                }
            }

            // 补充：如果企业的IPC在行业基准中不存在，使用行业整体均值
            if (!benchmark.isEmpty()) {
                double globalAvg = benchmark.values().stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(1.0);
                for (Map<String, Object> row : enterpriseIpcDist) {
                    String ipcCode = (String) row.get("ipcCode");
                    benchmark.putIfAbsent(ipcCode, globalAvg);
                }
            }
        } catch (Exception e) {
            log.warn("构建行业基准失败: {}", e.getMessage());
        }

        return benchmark;
    }

    /**
     * 获取行业关键IPC分类（从 ipc_chain_mapping 表查询）
     */
    private Set<String> getKeyIndustryIpc(String industry) {
        Set<String> keyIpcSet = new LinkedHashSet<>();

        try {
            // 从 ipc_chain_mapping 表查询所有IPC映射
            List<IpcChainMapping> mappings = ipcChainMappingMapper.selectList(null);
            if (mappings != null && !mappings.isEmpty()) {
                for (IpcChainMapping mapping : mappings) {
                    if (mapping.getIpcCode() != null) {
                        keyIpcSet.add(mapping.getIpcCode());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("从ipc_chain_mapping表查询行业IPC失败: {}", e.getMessage());
        }

        // 如果表中无数据，尝试从该行业企业实际使用的IPC中取Top
        if (keyIpcSet.isEmpty() && industry != null) {
            try {
                List<Map<String, Object>> industryIpc = patentMapper.selectIndustryBenchmarkIpc(industry, 20);
                if (industryIpc != null) {
                    for (Map<String, Object> row : industryIpc) {
                        String ipc = (String) row.get("ipcCode");
                        if (ipc != null) keyIpcSet.add(ipc);
                    }
                }
            } catch (Exception e) {
                log.debug("从专利表查询行业IPC失败: {}", e.getMessage());
            }
        }

        return keyIpcSet;
    }

    /**
     * IPC大类代码 → 技术领域中文名称（从 ipc_chain_mapping 表查）
     */
    private String ipcToFieldName(String ipcCode) {
        if (ipcCode == null) return "未知";

        try {
            // 从 ipc_chain_mapping 表查描述
            List<IpcChainMapping> mappings = ipcChainMappingMapper.selectList(null);
            if (mappings != null) {
                for (IpcChainMapping mapping : mappings) {
                    if (ipcCode.equals(mapping.getIpcCode()) && mapping.getDescription() != null) {
                        return mapping.getDescription();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("从ipc_chain_mapping查IPC名称失败: {}", e.getMessage());
        }

        return "IPC-" + ipcCode;
    }

    /**
     * 评估研发能力等级
     */
    private String evaluateRdLevel(int totalPatents, int validPatents, int advantageCount) {
        int score = 0;
        if (totalPatents >= 100) score += 40;
        else if (totalPatents >= 50) score += 30;
        else if (totalPatents >= 20) score += 20;
        else if (totalPatents >= 10) score += 10;

        double validRatio = totalPatents > 0 ? (double) validPatents / totalPatents : 0;
        if (validRatio >= 0.9) score += 30;
        else if (validRatio >= 0.7) score += 20;
        else if (validRatio >= 0.5) score += 10;

        if (advantageCount >= 3) score += 30;
        else if (advantageCount >= 2) score += 20;
        else if (advantageCount >= 1) score += 10;

        if (score >= 80) return "A";
        if (score >= 60) return "B";
        if (score >= 40) return "C";
        return "D";
    }
}
