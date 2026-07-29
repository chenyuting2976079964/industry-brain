package com.industrybrain.service.impl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.industrybrain.domain.model.IndustryChain;
import com.industrybrain.domain.model.IndustryChainNode;
import com.industrybrain.domain.model.InvestmentRecommendation;
import com.industrybrain.domain.model.MatchScoreResult;
import com.industrybrain.domain.model.ReportRecord;
import com.industrybrain.dto.request.*;
import com.industrybrain.dto.response.*;
import com.industrybrain.dto.response.EnterpriseInvestmentDetailResponse.*;
import com.industrybrain.dto.response.InvestmentMatchResponse.InvestmentEnterprise;
import com.industrybrain.dto.response.RecommendationResponse.RecommendedEnterprise;
import com.industrybrain.dto.response.ReportListResponse.ReportItem;
import com.industrybrain.dto.response.WishlistResponse.WishlistItem;
import com.industrybrain.infra.MinioFileService;
import com.industrybrain.mapper.elasticsearch.EnterpriseSearchMapper;
import com.industrybrain.mapper.elasticsearch.PatentSearchMapper;
import com.industrybrain.mapper.mysql.*;
import com.industrybrain.mapper.neo4j.EnterpriseRelationMapper;
import com.industrybrain.mapper.neo4j.IndustryChainGraphMapper;
import com.industrybrain.service.IMatchScoreService;
import com.industrybrain.service.IRecommendationService;
import com.industrybrain.utils.ExcelStyleUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 招商推荐业务实现类 + 招商价值综合评分算法（算法A5）
 *
 * 算法A5流程：
 *   Step1: 专利实力分(0-100) — 总量归一化(30%) + 高价值归一化(40%) + 发明占比归一化(30%)
 *   Step2: 产业带动潜力(0-100) — 规模分 + 年限分 + 技术外溢系数
 *   Step3: 综合分 = 匹配度×0.4 + 专利实力×0.3 + 潜力×0.3
 *   Step4: 评级 — ≥85→A(强烈推荐) / 70-84→B(推荐/关注) / <70→C(观察)
 */
@Service
@ConditionalOnProperty(name = "mysql.enabled", havingValue = "true", matchIfMissing = true)
public class RecommendationServiceImpl implements IRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    // ==================== A5 权重常量 ====================
    /** 匹配度权重 */
    private static final double MATCH_WEIGHT = 0.4;
    /** 专利实力权重 */
    private static final double PATENT_STRENGTH_WEIGHT = 0.3;
    /** 产业带动潜力权重 */
    private static final double POTENTIAL_WEIGHT = 0.3;

    // ==================== 专利实力子权重 ====================
    /** 总量归一化权重 */
    private static final double PATENT_TOTAL_WEIGHT = 0.30;
    /** 高价值归一化权重 */
    private static final double HIGH_VALUE_WEIGHT = 0.40;
    /** 发明占比归一化权重 */
    private static final double INVENTION_RATIO_WEIGHT = 0.30;

    // ==================== 评级阈值 ====================
    private static final double RATING_A_THRESHOLD = 85.0;
    private static final double RATING_B_THRESHOLD = 70.0;

    // ==================== 规模分 ====================
    private static final int LARGE_SCALE_SCORE = 90;
    private static final int MEDIUM_SCALE_SCORE = 70;
    private static final int SMALL_SCALE_SCORE = 50;

    // ==================== 年限分 ====================
    private static final int OLD_ENTERPRISE_SCORE = 90;   // >15年
    private static final int MID_ENTERPRISE_SCORE = 70;   // 5-15年
    private static final int NEW_ENTERPRISE_SCORE = 50;   // <5年
    private static final int OLD_YEARS = 15;
    private static final int MID_YEARS = 5;

    // ==================== 技术外溢 ====================
    private static final double SPILLOVER_MIN_BONUS = 5.0;
    private static final double SPILLOVER_MAX_BONUS = 15.0;

    @Resource
    private EnterpriseSearchMapper enterpriseSearchMapper;

    @Resource
    private PatentSearchMapper patentSearchMapper;

    @Resource
    private EnterpriseMapper enterpriseMapper;

    @Resource
    private ChainInfoMapper chainInfoMapper;

    @Resource
    private PatentMapper patentMapper;

    @Resource
    private IndustryChainNodeMapper industryChainNodeMapper;

    @Resource
    private IndustryChainGraphMapper industryChainGraphMapper;

    @Resource
    private EnterpriseRelationMapper enterpriseRelationMapper;

    @Resource
    private InvestmentRecommendationMapper recommendationMapper;

    @Resource
    private IMatchScoreService matchScoreService;

    @Resource
    private MinioFileService minioFileService;

    @Resource
    private ReportMapper reportMapper;

    // ==================== A5 公开方法 ====================

    @Override
    public RecommendationResponse getRecommendations(RecommendationRequest request) {
        log.info("=== A5 招商推荐开始: chainId={}, threshold={}, limit={} ===",
                request.getChainId(), request.getMinThreshold(), request.getLimit());

        RecommendationResponse response = new RecommendationResponse();
        List<RecommendedEnterprise> recommendedList = new ArrayList<>();

        try {
            // 1. 从数据库检索候选企业（按行业/区域过滤）
            String industry = getIndustryByChainId(request.getChainId());
            List<com.industrybrain.domain.model.Enterprise> entList;
            if (industry != null && !industry.isEmpty()) {
                entList = enterpriseMapper.searchByName(industry, request.getLimit() != null ? request.getLimit() * 3 : 60);
            } else {
                entList = enterpriseMapper.selectList(null);
            }
            // Convert to Map format for downstream processing
            List<Map<String, Object>> candidateEnterprises = entList.stream().map(e -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", e.getId());
                map.put("name", e.getName());
                return map;
            }).collect(Collectors.toList());

            if (candidateEnterprises == null || candidateEnterprises.isEmpty()) {
                log.info("未检索到候选企业");
                response.setEnterprises(new ArrayList<>());
                response.setTotal(0);
                return response;
            }

            log.info("Step1: 检索到 {} 家候选企业", candidateEnterprises.size());

            // 2. 对每个候选企业运行 A3 + A5 评分
            for (Map<String, Object> candidate : candidateEnterprises) {
                Long enterpriseId = toLong(candidate.get("id"));
                if (enterpriseId == null) continue;

                try {
                    // 调用A3匹配度计算 + A5综合评分
                    MatchScoreResult scoredResult = calculateMatchScore(enterpriseId, request.getChainId());

                    // 过滤低于阈值的
                    double finalScore = scoredResult.getOverallScore() != null
                            ? scoredResult.getOverallScore().doubleValue() : 0.0;
                    if (finalScore < (request.getMinThreshold() != null ? request.getMinThreshold() : 60)) {
                        continue;
                    }

                    RecommendedEnterprise rec = buildRecommendedEnterprise(enterpriseId, scoredResult);
                    recommendedList.add(rec);

                } catch (Exception e) {
                    log.error("企业评分失败: enterpriseId={}, error={}", enterpriseId, e.getMessage());
                }
            }

            // 3. 按综合分降序排列
            recommendedList.sort((a, b) -> {
                double sa = a.getOverallScore() != null ? a.getOverallScore().doubleValue() : 0;
                double sb = b.getOverallScore() != null ? b.getOverallScore().doubleValue() : 0;
                return Double.compare(sb, sa);
            });

            // 4. 截取Top N
            int limit = request.getLimit() != null ? request.getLimit() : 20;
            if (recommendedList.size() > limit) {
                recommendedList = new ArrayList<>(recommendedList.subList(0, limit));
            }

            response.setEnterprises(recommendedList);
            response.setTotal(recommendedList.size());

        } catch (Exception e) {
            log.error("招商推荐查询异常: {}", e.getMessage(), e);
            response.setEnterprises(new ArrayList<>());
            response.setTotal(0);
        }

        log.info("=== A5 招商推荐完成: {} 条推荐 ===", response.getTotal());
        return response;
    }

    @Override
    public MatchScoreResult calculateMatchScore(Long enterpriseId, Long chainId) {
        log.info("=== A5 综合评分计算: enterpriseId={}, chainId={} ===", enterpriseId, chainId);

        // ---- 调用 A3 获取匹配度 ----
        MatchScoreResult matchResult = matchScoreService.calculateMatch(enterpriseId, chainId);
        double matchScore = matchResult.getTechMatchScore() != null
                ? matchResult.getTechMatchScore().doubleValue() : 0.0;

        // ---- A5 Step1: 专利实力分(0-100) ----
        double patentStrengthScore = calculatePatentStrength(enterpriseId);
        log.info("A5-Step1: 专利实力分 = {}", String.format("%.2f", patentStrengthScore));

        // ---- A5 Step2: 产业带动潜力(0-100) ----
        double potentialScore = calculateDrivingPotential(enterpriseId);
        log.info("A5-Step2: 产业带动潜力 = {}", String.format("%.2f", potentialScore));

        // ---- A5 Step3: 综合分 ----
        double compositeScore = matchScore * MATCH_WEIGHT
                              + patentStrengthScore * PATENT_STRENGTH_WEIGHT
                              + potentialScore * POTENTIAL_WEIGHT;
        compositeScore = Math.min(compositeScore, 100.0);
        compositeScore = Math.max(compositeScore, 0.0);
        log.info("A5-Step3: 综合分 = {} (匹配度={}×0.4 + 专利实力={}×0.3 + 潜力={}×0.3)",
                String.format("%.2f", compositeScore), String.format("%.2f", matchScore),
                String.format("%.2f", patentStrengthScore), String.format("%.2f", potentialScore));

        // ---- A5 Step4: 评级 ----
        String rating;
        if (compositeScore >= RATING_A_THRESHOLD) {
            rating = "A";
        } else if (compositeScore >= RATING_B_THRESHOLD) {
            rating = "B";
        } else {
            rating = "C";
        }
        log.info("A5-Step4: 评级 = {}", rating);

        // 更新结果
        matchResult.setOverallScore(BigDecimal.valueOf(compositeScore).setScale(2, RoundingMode.HALF_UP));
        matchResult.setMatchLevel(rating);

        // 更新匹配详情
        String existingDetail = matchResult.getMatchDetail() != null ? matchResult.getMatchDetail() : "";
        String a5Detail = String.format("专利实力=%.1f, 产业带动=%.1f, 综合=%.1f, 评级=%s",
                patentStrengthScore, potentialScore, compositeScore, rating);
        matchResult.setMatchDetail(existingDetail + (existingDetail.isEmpty() ? "" : " | ") + a5Detail);

        // 持久化更新
        try {
            InvestmentRecommendation rec = recommendationMapper.selectByEnterpriseAndNode(enterpriseId, chainId);
            if (rec != null) {
                rec.setTechMatchScore(matchResult.getTechMatchScore());
                rec.setTechStrengthScore(BigDecimal.valueOf(patentStrengthScore).setScale(2, RoundingMode.HALF_UP));
                rec.setDrivingPotentialScore(BigDecimal.valueOf(potentialScore).setScale(2, RoundingMode.HALF_UP));
                rec.setTotalScore(BigDecimal.valueOf(compositeScore).setScale(2, RoundingMode.HALF_UP));
                rec.setRating(rating);
                rec.setMatchReasons(matchResult.getMatchDetail());
                rec.setStatus("pending");
                rec.setRecommendationDate(LocalDate.now());
                recommendationMapper.update(rec);
            } else {
                InvestmentRecommendation newRec = new InvestmentRecommendation();
                newRec.setEnterpriseId(enterpriseId);
                newRec.setTargetNodeId(chainId);
                newRec.setTechMatchScore(matchResult.getTechMatchScore());
                newRec.setTechStrengthScore(BigDecimal.valueOf(patentStrengthScore).setScale(2, RoundingMode.HALF_UP));
                newRec.setDrivingPotentialScore(BigDecimal.valueOf(potentialScore).setScale(2, RoundingMode.HALF_UP));
                newRec.setIndustryFitScore(matchResult.getTechMatchScore());
                newRec.setTotalScore(BigDecimal.valueOf(compositeScore).setScale(2, RoundingMode.HALF_UP));
                newRec.setRating(rating);
                newRec.setMatchReasons(matchResult.getMatchDetail());
                newRec.setStatus("pending");
                newRec.setRecommendationDate(LocalDate.now());
                recommendationMapper.insert(newRec);
            }
        } catch (Exception e) {
            log.warn("A5评分持久化失败: {}", e.getMessage());
        }

        log.info("=== A5 综合评分完成: enterpriseId={}, composite={}, rating={} ===",
                enterpriseId, String.format("%.2f", compositeScore), rating);

        return matchResult;
    }

    @Override
    public String exportRecommendations(RecommendationRequest request) {
        log.info("导出招商推荐报告: chainId={}", request.getChainId());

        try {
            // 1. 获取推荐列表
            RecommendationResponse recommendations = getRecommendations(request);

            // 2. 生成 Excel 文件
            byte[] excelData = buildRecommendationExcel(recommendations, request);

            // 3. 上传到 MinIO
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "investment_recommendation_" + timestamp + ".xlsx";
            String contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            String url = minioFileService.uploadToCategory("reports", fileName, excelData, contentType);

            if (url == null) {
                log.warn("推荐报告上传 MinIO 失败，返回本地文件名");
                return fileName;
            }

            log.info("招商推荐报告导出成功: url={}, 企业数={}", url, recommendations.getTotal());
            return url;

        } catch (Exception e) {
            log.error("导出招商推荐报告失败: chainId={}, error={}", request.getChainId(), e.getMessage(), e);
            return null;
        }
    }

    // ==================== A5 私有算法方法 ====================

    /**
     * A5 Step1: 计算专利实力分(0-100)
     *
     * 总量归一化(30%) + 高价值归一化(40%) + 发明占比归一化(30%)
     */
    private double calculatePatentStrength(Long enterpriseId) {
        // 企业专利数据
        int totalPatents = patentMapper.countByEnterpriseId(enterpriseId);
        long highValuePatents = patentMapper.selectHighValueCount(enterpriseId);
        int inventionPatents = patentMapper.selectInventionCount(enterpriseId);

        // 全局最大专利数（用于归一化）
        int maxPatentCount = patentMapper.selectMaxPatentCount();
        maxPatentCount = Math.max(maxPatentCount, 1); // 避免除零

        // ---- 总量归一化 (0-100) ----
        double totalNorm = Math.min((double) totalPatents / maxPatentCount, 1.0) * 100.0;

        // ---- 高价值归一化 (0-100) ----
        double highValueNorm = Math.min((double) highValuePatents / maxPatentCount, 1.0) * 100.0;

        // ---- 发明占比归一化 (0-100) ----
        double inventionRatio = totalPatents > 0 ? (double) inventionPatents / totalPatents : 0.0;
        double inventionNorm = inventionRatio * 100.0;

        // 加权融合
        double patentStrength = totalNorm * PATENT_TOTAL_WEIGHT
                              + highValueNorm * HIGH_VALUE_WEIGHT
                              + inventionNorm * INVENTION_RATIO_WEIGHT;

        log.debug("专利实力: 总量={}→{}, 高价值={}→{}, 发明占比={}→{}, 综合={}",
                totalPatents, String.format("%.1f", totalNorm), highValuePatents, String.format("%.1f", highValueNorm),
                String.format("%.2f", inventionRatio), String.format("%.1f", inventionNorm), String.format("%.1f", patentStrength));

        return Math.min(patentStrength, 100.0);
    }

    /**
     * A5 Step2: 计算产业带动潜力(0-100)
     *
     * 规模分(大型90/中型70/小型50) + 年限分(>15年90/5-15年70/<5年50) + 技术外溢系数(+5~15)
     */
    private double calculateDrivingPotential(Long enterpriseId) {
        com.industrybrain.domain.model.Enterprise modelEnt = enterpriseMapper.selectById(enterpriseId);
        if (modelEnt == null) {
            return 0.0;
        }

        // ---- 规模分 ----
        double scaleScore = SMALL_SCALE_SCORE; // 默认小型

        // 用注册资本辅助判断规模 (model uses String like "10000万元")
        String capitalStr = modelEnt.getRegisteredCapital();
        if (capitalStr != null) {
            double capital = parseCapitalToWan(capitalStr);
            if (capital >= 100000) { // >= 10亿(10亿=100000万)
                scaleScore = LARGE_SCALE_SCORE;
            } else if (capital >= 10000) { // >= 1亿
                scaleScore = LARGE_SCALE_SCORE;
            } else if (capital >= 1000) { // >= 1千万
                scaleScore = MEDIUM_SCALE_SCORE;
            }
        }

        // ---- 年限分 ----
        double yearsScore = NEW_ENTERPRISE_SCORE;
        LocalDate estDate = modelEnt.getEstablishmentDate();
        if (estDate != null) {
            int currentYear = LocalDate.now().getYear();
            int age = currentYear - estDate.getYear();

            if (age > OLD_YEARS) {
                yearsScore = OLD_ENTERPRISE_SCORE;
            } else if (age >= MID_YEARS) {
                yearsScore = MID_ENTERPRISE_SCORE;
            }
            log.debug("企业成立年限: {}年 → 年限分={}", age, yearsScore);
        }

        // ---- 技术外溢系数（Neo4j关联企业数 → +5~15分） ----
        double spilloverBonus = SPILLOVER_MIN_BONUS;
        try {
            long relatedCount = getRelatedEnterpriseCount(enterpriseId);
            // 关联企业数映射到5~15分
            // relatedCount = 0 → 5分, relatedCount >= 10 → 15分
            spilloverBonus = SPILLOVER_MIN_BONUS
                    + Math.min(relatedCount / 10.0, 1.0) * (SPILLOVER_MAX_BONUS - SPILLOVER_MIN_BONUS);
            log.debug("技术外溢: Neo4j关联企业数={}, 加分={}", relatedCount, String.format("%.1f", spilloverBonus));
        } catch (Exception e) {
            // Neo4j可能不可用，使用默认值
            log.debug("Neo4j查询失败，使用默认外溢系数={}", spilloverBonus);
        }

        // 综合潜力分（规模30% + 年限30% + 外溢40%）
        double potential = scaleScore * 0.30 + yearsScore * 0.30 + spilloverBonus / SPILLOVER_MAX_BONUS * 100.0 * 0.40;

        log.debug("产业带动潜力: 规模={}×0.3 + 年限={}×0.3 + 外溢={}×0.4 = {}",
                String.format("%.1f", scaleScore), String.format("%.1f", yearsScore),
                String.format("%.1f", spilloverBonus / SPILLOVER_MAX_BONUS * 100.0), String.format("%.1f", potential));

        return Math.min(potential, 100.0);
    }

    /**
     * 解析注册资本字符串为万元数值
     * 支持格式: "10000万元", "5000万", "1亿", "1000.5万"
     */
    private double parseCapitalToWan(String capitalStr) {
        try {
            String cleaned = capitalStr.replace("万元", "").replace("万", "");
            if (cleaned.contains("亿")) {
                return Double.parseDouble(cleaned.replace("亿", "")) * 10000.0;
            }
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 获取关联企业数（优先查Neo4j，失败则回退）
     */
    private long getRelatedEnterpriseCount(Long enterpriseId) {
        // 尝试通过Neo4j查询
        try {
            return enterpriseRelationMapper.findUpstreamDownstream(enterpriseId, 2).size();
        } catch (Exception e) {
            // Neo4j不可用时返回默认值
            return 1;
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建推荐企业DTO
     */
    private RecommendedEnterprise buildRecommendedEnterprise(Long enterpriseId, MatchScoreResult scored) {
        RecommendedEnterprise rec = new RecommendedEnterprise();
        rec.setEnterpriseId(enterpriseId);
        rec.setEnterpriseName(scored.getEnterpriseName());

        // 补充企业信息
        com.industrybrain.domain.model.Enterprise modelEnt = enterpriseMapper.selectById(enterpriseId);
        if (modelEnt != null) {
            rec.setIndustry(modelEnt.getIndustry());
            rec.setRegionCode(String.valueOf(modelEnt.getRegionId()));
        }

        rec.setTechMatchScore(scored.getTechMatchScore());
        rec.setOverallScore(scored.getOverallScore());
        rec.setMatchLevel(scored.getMatchLevel());
        rec.setReason(scored.getMatchDetail());
        return rec;
    }

    // ==================== 导出辅助方法 ====================

    /**
     * 构建招商推荐 Excel 文件
     */
    private byte[] buildRecommendationExcel(RecommendationResponse recommendations, RecommendationRequest request) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            // 样式
            CellStyle titleStyle = ExcelStyleUtils.createTitleStyle(wb);
            CellStyle headerStyle = ExcelStyleUtils.createHeaderStyle(wb);
            CellStyle dataStyle = ExcelStyleUtils.createDataStyle(wb);
            CellStyle aStyle = ExcelStyleUtils.createHighlightStyle(wb, IndexedColors.GREEN);
            CellStyle bStyle = ExcelStyleUtils.createHighlightStyle(wb, IndexedColors.LIGHT_BLUE);
            CellStyle cStyle = ExcelStyleUtils.createHighlightStyle(wb, IndexedColors.GREY_40_PERCENT);

            Sheet sheet = wb.createSheet("招商推荐企业");

            int rowIdx = 0;
            // 标题
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("产业链智能招商推荐报告");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));
            rowIdx++;

            // 筛选条件摘要
            Row filterRow = sheet.createRow(rowIdx++);
            filterRow.createCell(0).setCellValue("筛选条件: 产业链ID=" + request.getChainId()
                    + ", 最低阈值=" + (request.getMinThreshold() != null ? request.getMinThreshold() : 60)
                    + ", 生成时间=" + LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 8));
            rowIdx++;

            // 表头
            Row hRow = sheet.createRow(rowIdx++);
            String[] headers = {"序号", "企业名称", "行业", "区域", "技术匹配度",
                    "综合评分", "匹配等级", "评级", "推荐理由"};
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // 数据行
            List<RecommendationResponse.RecommendedEnterprise> enterprises = recommendations.getEnterprises();
            if (enterprises != null) {
                int seq = 1;
                for (RecommendationResponse.RecommendedEnterprise ent : enterprises) {
                    Row r = sheet.createRow(rowIdx++);
                    r.createCell(0).setCellValue(seq++);
                    r.createCell(1).setCellValue(ent.getEnterpriseName() != null ? ent.getEnterpriseName() : "");
                    r.createCell(2).setCellValue(ent.getIndustry() != null ? ent.getIndustry() : "");
                    r.createCell(3).setCellValue(ent.getRegionCode() != null ? ent.getRegionCode() : "");
                    r.createCell(4).setCellValue(ent.getTechMatchScore() != null ? ent.getTechMatchScore().doubleValue() : 0);
                    r.createCell(5).setCellValue(ent.getOverallScore() != null ? ent.getOverallScore().doubleValue() : 0);
                    r.createCell(6).setCellValue(getMatchLevelLabel(ent.getMatchLevel()));
                    r.createCell(7).setCellValue(ent.getMatchLevel() != null ? ent.getMatchLevel() : "");
                    r.createCell(8).setCellValue(ent.getReason() != null ? ent.getReason() : "");

                    // 评级列着色
                    String level = ent.getMatchLevel();
                    CellStyle ratingStyle;
                    if ("A".equals(level)) ratingStyle = aStyle;
                    else if ("B".equals(level)) ratingStyle = bStyle;
                    else ratingStyle = cStyle;

                    for (int i = 0; i < headers.length; i++) {
                        if (i == 7) r.getCell(i).setCellStyle(ratingStyle);
                        else r.getCell(i).setCellStyle(dataStyle);
                    }
                }
            }

            // 列宽
            int[] widths = {6, 28, 16, 10, 12, 12, 12, 8, 44};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    private String getMatchLevelLabel(String level) {
        if ("A".equals(level)) return "高度推荐";
        if ("B".equals(level)) return "推荐关注";
        if ("C".equals(level)) return "持续观察";
        return level != null ? level : "-";
    }

    /**
     * 根据产业链ID推断行业类型（从数据库查询节点所属产业链名称）
     */
    private String getIndustryByChainId(Long chainId) {
        if (chainId == null) return null;
        try {
            // 查询产业链名称
            List<IndustryChain> chains = chainInfoMapper.selectAllOrdered();
            if (chains != null) {
                for (IndustryChain chain : chains) {
                    if (chainId.equals(chain.getId()) && chain.getName() != null) {
                        return chain.getName();
                    }
                }
            }
            // 如果直接匹配不到，通过链节点查询
            IndustryChainNode node = industryChainNodeMapper.selectById(chainId);
            if (node != null && node.getNodeName() != null) {
                return node.getNodeName();
            }
        } catch (Exception e) {
            log.debug("查询产业链节点失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 安全转换 Object → Long
     */
    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==================== 精准招商方法 (原 InvestmentServiceImpl) ====================

    @Override
    public InvestmentMatchResponse getMatchList(InvestmentMatchRequest request) {
        log.info("查询招商匹配列表, weakness={}, matchMin={}, keyword={}, tab={}",
                request.getWeakness(), request.getMatchMin(), request.getKeyword(), request.getTab());

        int page = request.getPage() != null ? request.getPage() : 1;
        int size = request.getSize() != null ? request.getSize() : 10;
        int offset = (page - 1) * size;

        List<InvestmentRecommendation> list = recommendationMapper.selectMatchList(
                null, request.getWeakness(), request.getMatchMin(),
                request.getScale(), request.getSortBy(), request.getKeyword(),
                request.getNodeId(), request.getTab(), offset, size);

        int total = recommendationMapper.countMatchList(
                null, request.getWeakness(), request.getMatchMin(),
                request.getScale(), request.getSortBy(), request.getKeyword(),
                request.getNodeId(), request.getTab());

        InvestmentMatchResponse response = new InvestmentMatchResponse();
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        response.setEnterprises(list.stream()
                .map(this::toInvestmentEnterprise)
                .collect(Collectors.toList()));

        return response;
    }

    @Override
    public EnterpriseInvestmentDetailResponse getEnterpriseDetail(Long enterpriseId, Long nodeId) {
        log.info("查询企业招商详情, enterpriseId={}, nodeId={}", enterpriseId, nodeId);

        InvestmentRecommendation rec = recommendationMapper.selectEnterpriseDetail(enterpriseId, nodeId);
        if (rec == null) {
            log.warn("未找到企业招商详情, enterpriseId={}", enterpriseId);
            return new EnterpriseInvestmentDetailResponse();
        }

        EnterpriseInvestmentDetailResponse response = new EnterpriseInvestmentDetailResponse();

        EnterpriseInfo info = new EnterpriseInfo();
        info.setEnterpriseId(rec.getEnterpriseId());
        info.setName(rec.getEnterpriseName());
        info.setInitial(firstChar(rec.getEnterpriseName()));
        info.setDescription(rec.getIndustry() != null ? rec.getIndustry() : "");
        info.setLocation(rec.getRegionCode());
        info.setPatentCount(patentMapper.countByEnterpriseId(enterpriseId));
        info.setTags(buildDetailTags(rec.getDiagnosisTag(), rec.getRating()));
        info.setTagStyles(buildDetailTagStyles(rec.getDiagnosisTag(), rec.getRating()));
        response.setEnterprise(info);

        StatCards stats = new StatCards();
        stats.setTotalPatents(patentMapper.countByEnterpriseId(enterpriseId));
        stats.setHighValuePatents((int) patentMapper.selectHighValueCount(enterpriseId));
        stats.setInventionCount(patentMapper.selectInventionCount(enterpriseId));
        response.setStats(stats);

        ScoreBreakdown scores = new ScoreBreakdown();
        scores.setMatchScore(rec.getTechMatchScore());
        scores.setPatentStrength(rec.getTechStrengthScore());
        scores.setIndustryFit(rec.getIndustryFitScore());
        scores.setDrivingPotential(rec.getDrivingPotentialScore());
        scores.setTotalScore(rec.getTotalScore());
        scores.setRating(rec.getRating());
        scores.setMatchLevel(getMatchLevelText(rec.getTotalScore()));
        response.setScores(scores);

        if (rec.getMatchReasons() != null) {
            response.setMatchReasons(Arrays.asList(rec.getMatchReasons().split(",")));
        }

        response.setEvaluation(buildEvaluation(rec.getRating()));

        return response;
    }

    @Override
    public WishlistResponse getWishlist() {
        log.info("查询招商清单");
        List<InvestmentRecommendation> list = recommendationMapper.selectWishlist(null);

        WishlistResponse response = new WishlistResponse();
        response.setTotal(list.size());
        response.setItems(list.stream()
                .map(this::toWishlistItem)
                .collect(Collectors.toList()));

        return response;
    }

    @Override
    public void addToWishlist(WishlistAddRequest request) {
        log.info("加入清单, enterpriseId={}, nodeId={}", request.getEnterpriseId(), request.getNodeId());

        InvestmentRecommendation existing = recommendationMapper.selectByEnterpriseAndNode(
                request.getEnterpriseId(), request.getNodeId());

        if (existing != null) {
            recommendationMapper.addToWishlist(request.getEnterpriseId(), request.getNodeId());
        } else if (request.getRecommendationId() != null) {
            InvestmentRecommendation rec = recommendationMapper.selectById(request.getRecommendationId());
            if (rec != null) {
                rec.setStatus("wishlist");
                recommendationMapper.update(rec);
            }
        }
    }

    @Override
    public void removeFromWishlist(Long id) {
        log.info("移出清单, id={}", id);
        recommendationMapper.removeFromWishlist(id);
    }

    @Override
    public String reMatch(Long nodeId) {
        log.info("=== 触发重新匹配: nodeId={} ===", nodeId);
        String taskId = "REMATCH_" + UUID.randomUUID().toString().substring(0, 8);

        try {
            if (nodeId != null) {
                List<InvestmentRecommendation> existingRecs = recommendationMapper.selectByNodeId(nodeId);
                if (!existingRecs.isEmpty()) {
                    List<Long> enterpriseIds = existingRecs.stream()
                            .map(InvestmentRecommendation::getEnterpriseId)
                            .distinct()
                            .collect(Collectors.toList());

                    log.info("重新计算 {} 家企业的匹配度", enterpriseIds.size());
                    matchScoreService.batchCalculate(enterpriseIds, nodeId);

                    for (InvestmentRecommendation rec : existingRecs) {
                        try {
                            MatchScoreResult scored = matchScoreService.calculateMatch(rec.getEnterpriseId(), nodeId);
                            double composite = scored.getOverallScore() != null ? scored.getOverallScore().doubleValue() : 0;
                            rec.setTechMatchScore(scored.getTechMatchScore());
                            rec.setTotalScore(scored.getOverallScore());
                            rec.setMatchReasons(scored.getMatchDetail());
                            rec.setRating(mapScoreToRating(composite));
                            rec.setUpdateTime(new Date());
                            recommendationMapper.update(rec);
                        } catch (Exception e) {
                            log.error("重新评分失败: enterpriseId={}, error={}", rec.getEnterpriseId(), e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("重新匹配执行失败: {}", e.getMessage(), e);
        }

        log.info("重新匹配任务已提交, taskId={}", taskId);
        return taskId;
    }

    // ==================== 招商报告方法 (原 ReportServiceImpl) ====================

    private static final String EXCEL_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String DATE_FMT = "yyyyMMdd_HHmmss";

    private static final Map<String, String[]> TYPE_ICON_MAP = new LinkedHashMap<>();
    static {
        TYPE_ICON_MAP.put("investment_list", new String[]{"file-lines", "emerald"});
        TYPE_ICON_MAP.put("diagnosis", new String[]{"chart-bar", "blue"});
        TYPE_ICON_MAP.put("enterprise_profile", new String[]{"building", "purple"});
        TYPE_ICON_MAP.put("chain_supplement", new String[]{"lightbulb", "amber"});
    }

    @Override
    public ReportListResponse getReports(Integer page, Integer size) {
        log.info("查询报告列表, page={}, size={}", page, size);

        int pg = page != null ? page : 1;
        int sz = size != null ? size : 10;
        int offset = (pg - 1) * sz;

        List<ReportRecord> records = reportMapper.selectList(offset, sz);
        int total = reportMapper.countAll();

        ReportListResponse response = new ReportListResponse();
        response.setTotal(total);
        response.setPage(pg);
        response.setSize(sz);
        response.setReports(records.stream().map(this::convertToItem).collect(Collectors.toList()));

        return response;
    }

    @Override
    public ReportDetailResponse getReportDetail(Long id) {
        log.info("查询报告详情, id={}", id);

        ReportRecord record = reportMapper.selectById(id);
        if (record == null) {
            return null;
        }

        ReportDetailResponse response = new ReportDetailResponse();
        response.setId(record.getId());
        response.setTitle(record.getTitle());
        response.setReportType(record.getReportType());
        response.setReportTypeName(getReportTypeName(record.getReportType()));
        response.setChainName(null);   // chainName is not stored in report table, fetched from chain_id via JOIN or external lookup
        response.setScope(record.getScope());
        response.setEnterpriseCount(record.getEnterpriseCount());
        response.setFormat(record.getOutputFormat());
        response.setFileUrl(record.getFileUrl());
        response.setFileSize(record.getFileSize());
        response.setGeneratedAt(record.getCreatedAt());
        response.setGenerationTime(record.getGenerationTime());
        response.setStatus(record.getStatus());

        return response;
    }

    @Override
    public String generateReport(ReportGenerateRequest request) {
        log.info("生成新报告, type={}, format={}, scope={}", request.getType(), request.getFormat(), request.getScope());

        String title = buildReportTitle(request);

        ReportRecord record = new ReportRecord();
        record.setTitle(title);
        record.setReportType(request.getType());
        record.setScope(request.getScope());
        record.setOutputFormat(request.getFormat() != null ? request.getFormat() : "Excel");
        record.setStatus("pending");

        reportMapper.insert(record);

        String taskId = "GEN_" + record.getId();
        log.info("报告记录已创建, reportId={}, taskId={}", record.getId(), taskId);

        generateReportAsync(record.getId(), request);

        return taskId;
    }

    @Override
    public String downloadReport(Long id) {
        log.info("下载报告, id={}", id);

        ReportRecord record = reportMapper.selectById(id);
        if (record == null || record.getFileUrl() == null) {
            log.warn("报告文件不存在, id={}", id);
            return null;
        }

        return record.getFileUrl();
    }

    @Override
    public void deleteReport(Long id) {
        log.info("删除报告, id={}", id);

        ReportRecord record = reportMapper.selectById(id);
        if (record != null && record.getFileUrl() != null) {
            try {
                String url = record.getFileUrl();
                int bucketIdx = url.indexOf("/industry-brain/");
                if (bucketIdx > 0) {
                    String objectName = url.substring(bucketIdx + "/industry-brain/".length());
                    int queryIdx = objectName.indexOf('?');
                    if (queryIdx > 0) {
                        objectName = objectName.substring(0, queryIdx);
                    }
                    minioFileService.delete(objectName);
                }
            } catch (Exception e) {
                log.warn("删除 MinIO 文件失败: {}", e.getMessage());
            }
        }

        reportMapper.deleteById(id);
    }

    @Override
    public List<Map<String, Object>> getTemplates() {
        log.info("查询报告模板列表");
        return Collections.emptyList();
    }

    @Override
    public ReportStatsResponse getStats() {
        log.info("查询报告统计");

        ReportStatsResponse response = new ReportStatsResponse();
        response.setMonthlyGenerated(reportMapper.countMonthly());
        response.setTotalExported(reportMapper.countTotal());
        response.setCoveredEnterprises(reportMapper.countCoveredEnterprises());

        int lastMonth = reportMapper.countLastMonth();
        int thisMonth = reportMapper.countMonthly();
        if (lastMonth > 0) {
            response.setMonthlyGrowth((double) (thisMonth - lastMonth) / lastMonth * 100);
        } else {
            response.setMonthlyGrowth(thisMonth > 0 ? 100.0 : 0.0);
        }
        response.setCumulativeGrowth(0.0);

        return response;
    }

    @Override
    public String exportAll(ExportAllRequest request) {
        log.info("批量导出, format={}", request.getFormat());

        try {
            String format = request.getFormat() != null ? request.getFormat() : "Excel";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FMT));
            String taskId = "EXPORT_ALL_" + UUID.randomUUID().toString().substring(0, 8);

            exportAllAsync(taskId, format, timestamp);

            log.info("批量导出任务已提交, taskId={}", taskId);
            return taskId;

        } catch (Exception e) {
            log.error("批量导出失败: {}", e.getMessage(), e);
            return null;
        }
    }

    // ==================== 报告异步方法 ====================

    @Async("reportTaskExecutor")
    public void generateReportAsync(Long reportId, ReportGenerateRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("异步报告生成开始: reportId={}, type={}", reportId, request.getType());

        ReportRecord record = reportMapper.selectById(reportId);
        if (record == null) {
            log.error("报告记录不存在: reportId={}", reportId);
            return;
        }

        try {
            record.setStatus("processing");
            reportMapper.update(record);

            byte[] excelData;
            int enterpriseCount;

            if ("investment_list".equals(request.getType())) {
                List<InvestmentRecommendation> enterprises = recommendationMapper.selectMatchList(
                        null, null, null, null, null, null,
                        request.getNodeId(), null, 0, 200);
                enterpriseCount = enterprises.size();
                excelData = buildInvestmentListExcel(enterprises, record.getTitle());
            } else {
                enterpriseCount = 0;
                excelData = buildGenericReportExcel(record);
            }

            String format = request.getFormat() != null ? request.getFormat() : "Excel";
            String fileName = "report_" + reportId + "_" + timestamp() + "." + format.toLowerCase();
            String url = minioFileService.uploadToCategory("reports", fileName, excelData, EXCEL_MIME);

            if (url == null) {
                log.error("报告文件上传 MinIO 失败: reportId={}", reportId);
                record.setStatus("failed");
                reportMapper.update(record);
                return;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            record.setFileUrl(url);
            record.setFileSize((long) excelData.length);
            record.setEnterpriseCount(enterpriseCount);
            record.setOutputFormat(format);
            record.setStatus("completed");
            record.setGenerationTime(elapsed);
            reportMapper.update(record);

            log.info("异步报告生成完成: reportId={}, 企业数={}, 耗时={}ms", reportId, enterpriseCount, elapsed);

        } catch (Exception e) {
            log.error("异步报告生成失败: reportId={}, error={}", reportId, e.getMessage(), e);
            try {
                record.setStatus("failed");
                reportMapper.update(record);
            } catch (Exception ex) {
                log.error("更新报告失败状态异常: {}", ex.getMessage());
            }
        }
    }

    @Async("reportTaskExecutor")
    public void exportAllAsync(String taskId, String format, String timestamp) {
        log.info("异步批量导出开始: taskId={}, format={}", taskId, format);

        try {
            List<ReportRecord> allReports = reportMapper.selectList(0, 1000);
            List<ReportRecord> completedReports = allReports.stream()
                    .filter(r -> "completed".equals(r.getStatus()))
                    .collect(Collectors.toList());

            byte[] excelData = buildBatchExportExcel(completedReports);

            String fileName = "batch_export_" + taskId + "_" + timestamp + ".xlsx";
            String url = minioFileService.uploadToCategory("exports", fileName, excelData, EXCEL_MIME);

            if (url != null) {
                log.info("批量导出完成: taskId={}, 报告数={}, url={}", taskId, completedReports.size(), url);
            } else {
                log.warn("批量导出上传 MinIO 失败: taskId={}", taskId);
            }

        } catch (Exception e) {
            log.error("批量导出失败: taskId={}, error={}", taskId, e.getMessage(), e);
        }
    }

    // ==================== Excel 生成方法 ====================

    private byte[] buildInvestmentListExcel(List<InvestmentRecommendation> enterprises, String title) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle titleStyle = ExcelStyleUtils.createTitleStyle(wb);
            CellStyle headerStyle = ExcelStyleUtils.createHeaderStyle(wb);
            CellStyle dataStyle = ExcelStyleUtils.createDataStyle(wb);
            CellStyle aStyle = ExcelStyleUtils.createHighlightStyle(wb, IndexedColors.GREEN);
            CellStyle bStyle = ExcelStyleUtils.createHighlightStyle(wb, IndexedColors.LIGHT_BLUE);
            CellStyle cStyle = ExcelStyleUtils.createHighlightStyle(wb, IndexedColors.GREY_40_PERCENT);

            Sheet sheet = wb.createSheet("招商推荐清单");

            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));
            rowIdx++;

            Row hRow = sheet.createRow(rowIdx++);
            String[] headers = {"序号", "企业名称", "行业", "区域", "节点名称", "诊断标签",
                    "匹配度", "专利实力", "产业适配", "带动潜力", "综合评分", "评级"};
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int seq = 1;
            for (InvestmentRecommendation ent : enterprises) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(seq++);
                r.createCell(1).setCellValue(ent.getEnterpriseName() != null ? ent.getEnterpriseName() : "");
                r.createCell(2).setCellValue(ent.getIndustry() != null ? ent.getIndustry() : "");
                r.createCell(3).setCellValue(ent.getRegionCode() != null ? ent.getRegionCode() : "");
                r.createCell(4).setCellValue(ent.getNodeName() != null ? ent.getNodeName() : "");
                r.createCell(5).setCellValue(ent.getDiagnosisTag() != null ? ent.getDiagnosisTag() : "");
                r.createCell(6).setCellValue(ent.getTechMatchScore() != null ? ent.getTechMatchScore().doubleValue() : 0);
                r.createCell(7).setCellValue(ent.getTechStrengthScore() != null ? ent.getTechStrengthScore().doubleValue() : 0);
                r.createCell(8).setCellValue(ent.getIndustryFitScore() != null ? ent.getIndustryFitScore().doubleValue() : 0);
                r.createCell(9).setCellValue(ent.getDrivingPotentialScore() != null ? ent.getDrivingPotentialScore().doubleValue() : 0);
                r.createCell(10).setCellValue(ent.getTotalScore() != null ? ent.getTotalScore().doubleValue() : 0);

                String rating = ent.getRating();
                r.createCell(11).setCellValue(rating != null ? rating : "");
                CellStyle ratingStyle = dataStyle;
                if ("A".equals(rating)) ratingStyle = aStyle;
                else if ("B".equals(rating)) ratingStyle = bStyle;
                else if ("C".equals(rating)) ratingStyle = cStyle;

                for (int i = 0; i < headers.length; i++) {
                    if (i == 11) r.getCell(i).setCellStyle(ratingStyle);
                    else r.getCell(i).setCellStyle(dataStyle);
                }
            }

            int[] widths = {6, 28, 16, 10, 14, 10, 10, 10, 10, 10, 10, 8};
            for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    private byte[] buildGenericReportExcel(ReportRecord record) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle titleStyle = ExcelStyleUtils.createTitleStyle(wb);
            CellStyle headerStyle = ExcelStyleUtils.createHeaderStyle(wb);
            CellStyle dataStyle = ExcelStyleUtils.createDataStyle(wb);
            CellStyle labelStyle = ExcelStyleUtils.createLabelStyle(wb);

            Sheet sheet = wb.createSheet("报告摘要");
            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(record.getTitle());
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            rowIdx++;

            ExcelStyleUtils.addKvRow(sheet, rowIdx++, "报告类型", getReportTypeName(record.getReportType()), labelStyle, dataStyle);
            ExcelStyleUtils.addKvRow(sheet, rowIdx++, "产业链", record.getChainName() != null ? record.getChainName() : "-", labelStyle, dataStyle);
            ExcelStyleUtils.addKvRow(sheet, rowIdx++, "范围", record.getScope() != null ? record.getScope() : "全部", labelStyle, dataStyle);
            ExcelStyleUtils.addKvRow(sheet, rowIdx++, "输出格式", record.getOutputFormat() != null ? record.getOutputFormat() : "Excel", labelStyle, dataStyle);
            ExcelStyleUtils.addKvRow(sheet, rowIdx++, "生成时间", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), labelStyle, dataStyle);
            rowIdx++;

            Row noteRow = sheet.createRow(rowIdx);
            Cell noteCell = noteRow.createCell(0);
            noteCell.setCellValue("此报告为自动生成，具体企业详情请通过招商推荐模块查看。");
            noteCell.setCellStyle(dataStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 3));

            sheet.setColumnWidth(0, 16 * 256);
            sheet.setColumnWidth(1, 40 * 256);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    private byte[] buildBatchExportExcel(List<ReportRecord> reports) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle titleStyle = ExcelStyleUtils.createTitleStyle(wb);
            CellStyle headerStyle = ExcelStyleUtils.createHeaderStyle(wb);
            CellStyle dataStyle = ExcelStyleUtils.createDataStyle(wb);

            Sheet sheet = wb.createSheet("批量导出汇总");
            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("招商报告批量导出汇总");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
            rowIdx++;

            Row hRow = sheet.createRow(rowIdx++);
            String[] headers = {"序号", "报告标题", "类型", "企业数", "格式", "状态", "生成时间"};
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int seq = 1;
            for (ReportRecord r : reports) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(seq++);
                row.createCell(1).setCellValue(r.getTitle() != null ? r.getTitle() : "");
                row.createCell(2).setCellValue(getReportTypeName(r.getReportType()));
                row.createCell(3).setCellValue(r.getEnterpriseCount() != null ? r.getEnterpriseCount() : 0);
                row.createCell(4).setCellValue(r.getOutputFormat() != null ? r.getOutputFormat() : "");
                row.createCell(5).setCellValue("completed".equals(r.getStatus()) ? "已完成" : r.getStatus());
                row.createCell(6).setCellValue(r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
                for (int i = 0; i < headers.length; i++) row.getCell(i).setCellStyle(dataStyle);
            }

            int[] widths = {6, 36, 18, 10, 10, 10, 20};
            for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    // ==================== 招商报告辅助方法 ====================

    private ReportItem convertToItem(ReportRecord record) {
        ReportItem item = new ReportItem();
        item.setId(record.getId());
        item.setTitle(record.getTitle());
        item.setReportType(record.getReportType());
        item.setReportDate(record.getCreatedAt());
        item.setEnterpriseCount(record.getEnterpriseCount());
        item.setFormat(record.getOutputFormat());
        item.setStatus(record.getStatus());

        String[] iconInfo = TYPE_ICON_MAP.getOrDefault(record.getReportType(),
                new String[]{"file-lines", "slate"});
        item.setIcon(iconInfo[0]);
        item.setIconColor(iconInfo[1]);

        return item;
    }

    private String getReportTypeName(String type) {
        if (type == null) return "未知类型";
        switch (type) {
            case "investment_list": return "招商推荐清单";
            case "diagnosis": return "产业链诊断报告";
            case "enterprise_profile": return "企业画像报告";
            case "chain_supplement": return "补链建议方案";
            default: return type;
        }
    }

    private String buildReportTitle(ReportGenerateRequest request) {
        String typeName = getReportTypeName(request.getType());
        String dateStr = java.time.LocalDate.now().toString();
        return typeName + "（" + dateStr + "）";
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FMT));
    }


    // ==================== 投资辅助方法 ====================

    private InvestmentEnterprise toInvestmentEnterprise(InvestmentRecommendation rec) {
        InvestmentEnterprise ent = new InvestmentEnterprise();
        ent.setRecommendationId(rec.getId());
        ent.setEnterpriseId(rec.getEnterpriseId());
        ent.setEnterpriseName(rec.getEnterpriseName());
        ent.setInitial(firstChar(rec.getEnterpriseName()));
        ent.setDescription(rec.getIndustry());
        ent.setLocation(rec.getRegionCode());
        ent.setMatchScore(rec.getTotalScore() != null
                ? rec.getTotalScore().setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        ent.setMatchLevel(getMatchLevelKey(rec.getTotalScore()));
        ent.setMatchLevelText(getMatchLevelText(rec.getTotalScore()));
        ent.setPatentStrengthScore(rec.getTechStrengthScore());
        ent.setIndustryFitScore(rec.getIndustryFitScore());
        ent.setDrivingPotentialScore(rec.getDrivingPotentialScore());
        ent.setTotalScore(rec.getTotalScore());
        ent.setRating(rec.getRating());
        ent.setNodeName(rec.getNodeName());
        ent.setDiagnosisTag(rec.getDiagnosisTag());

        if (rec.getMatchReasons() != null) {
            ent.setMatchReasons(Arrays.asList(rec.getMatchReasons().split(",")));
        }

        ent.setTags(buildTags(rec));
        ent.setTagStyles(buildTagStyles(rec));
        ent.setInWishlist("wishlist".equals(rec.getStatus()));

        return ent;
    }

    private WishlistItem toWishlistItem(InvestmentRecommendation rec) {
        WishlistItem item = new WishlistItem();
        item.setId(rec.getId());
        item.setEnterpriseId(rec.getEnterpriseId());
        item.setEnterpriseName(rec.getEnterpriseName());
        item.setIndustry(rec.getIndustry());
        item.setMatchScore(rec.getTotalScore());
        item.setRating(rec.getRating());
        item.setNodeName(rec.getNodeName());
        item.setDiagnosisTag(rec.getDiagnosisTag());
        item.setAddedAt(rec.getUpdateTime());
        return item;
    }

    private List<String> buildTags(InvestmentRecommendation rec) {
        List<String> tags = new ArrayList<>();
        String rating = rec.getRating();
        if ("A".equals(rating)) tags.add("强烈推荐");
        else if ("B".equals(rating)) tags.add("推荐关注");
        String diagnosisTag = rec.getDiagnosisTag();
        if (diagnosisTag != null) tags.add(diagnosisTag);
        return tags;
    }

    private List<String> buildTagStyles(InvestmentRecommendation rec) {
        List<String> styles = new ArrayList<>();
        String rating = rec.getRating();
        if ("A".equals(rating)) styles.add("bg-green-50 text-green-600");
        else if ("B".equals(rating)) styles.add("bg-blue-50 text-blue-600");
        String diagnosisTag = rec.getDiagnosisTag();
        if ("弱链".equals(diagnosisTag)) styles.add("bg-orange-50 text-orange-600");
        else if ("缺链".equals(diagnosisTag)) styles.add("bg-red-50 text-red-600");
        else if (diagnosisTag != null) styles.add("status-strong-bg");
        return styles;
    }

    private List<String> buildDetailTags(String diagnosisTag, String rating) {
        List<String> tags = new ArrayList<>();
        if ("强链".equals(diagnosisTag)) tags.add("强链节点");
        else if ("缺链".equals(diagnosisTag)) tags.add("缺链节点");
        if ("A".equals(rating)) tags.add("强烈推荐");
        else if ("B".equals(rating)) tags.add("推荐关注");
        return tags;
    }

    private List<String> buildDetailTagStyles(String diagnosisTag, String rating) {
        List<String> styles = new ArrayList<>();
        if ("强链".equals(diagnosisTag)) styles.add("status-strong-bg");
        else if ("缺链".equals(diagnosisTag)) styles.add("bg-red-50 text-red-600");
        if ("A".equals(rating)) styles.add("bg-green-50 text-green-600");
        else if ("B".equals(rating)) styles.add("bg-blue-50 text-blue-600");
        return styles;
    }

    private InvestmentEvaluation buildEvaluation(String rating) {
        InvestmentEvaluation eval = new InvestmentEvaluation();
        if ("A".equals(rating)) {
            eval.setOverallAssessment("该企业技术实力突出，与目标产业链环节高度匹配，产业带动能力较强，建议优先对接招商。");
            eval.setRecommendationLevel("强烈推荐");
        } else if ("B".equals(rating)) {
            eval.setOverallAssessment("该企业具备较好技术基础，与产业链有一定匹配度，可作为重点关注目标。");
            eval.setRecommendationLevel("推荐关注");
        } else {
            eval.setOverallAssessment("该企业与目标环节匹配度一般，建议持续观察跟踪。");
            eval.setRecommendationLevel("观察");
        }
        return eval;
    }

    private String getMatchLevelKey(BigDecimal score) {
        if (score == null) return "low";
        double s = score.doubleValue();
        if (s >= 85) return "high";
        if (s >= 70) return "medium";
        return "low";
    }

    private String getMatchLevelText(BigDecimal score) {
        if (score == null) return "低度匹配";
        double s = score.doubleValue();
        if (s >= 85) return "高度匹配";
        if (s >= 70) return "中度匹配";
        return "低度匹配";
    }

    private String firstChar(String str) {
        return str != null && !str.isEmpty() ? str.substring(0, 1) : "?";
    }

    private String mapScoreToRating(double score) {
        if (score >= 85) return "A";
        if (score >= 70) return "B";
        return "C";
    }
}
