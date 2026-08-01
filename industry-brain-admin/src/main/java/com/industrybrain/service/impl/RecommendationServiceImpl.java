package com.industrybrain.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Autowired;

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
 * 招商推荐业务实现类 + 企业匹配度综合评分算法（三维度）
 *
 * 算法流程：
 *   Dimension 1: 业务契合度得分 (权重50%) — 经营范围文本 × 节点需求文本 → 余弦相似度 → 分段映射
 *   Dimension 2: 规模实力得分     (权重35%) — 团队规模得分×20 + 资本实力得分×15 ÷ 35
 *   Dimension 3: 地域协同度得分   (权重15%) — 企业省份 vs 需求方省份 → 省内100/省外60
 *
 * 综合得分 = 业务契合度×0.5 + 规模实力×0.35 + 地域协同度×0.15
 * 评级 — ≥85→A(强烈推荐) / 70-84→B(推荐/关注) / <70→C(观察)
 */
@Service
@ConditionalOnProperty(name = "mysql.enabled", havingValue = "true", matchIfMissing = true)
public class RecommendationServiceImpl implements IRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    // ==================== 评级阈值 ====================
    private static final double RATING_A_THRESHOLD = 85.0;
    private static final double RATING_B_THRESHOLD = 70.0;

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

    @Autowired(required = false)
    private MinioFileService minioFileService;

    @Resource
    private ReportMapper reportMapper;

    // ==================== 公开方法 ====================

    @Override
    public RecommendationResponse getRecommendations(RecommendationRequest request) {
        log.info("=== 三维度招商推荐开始: chainId={}, threshold={}, limit={} ===",
                request.getChainId(), request.getMinThreshold(), request.getLimit());

        // 从 regionCode 提取需求方省份（前2位）
        String demandProvince = extractDemandProvince(request.getRegionCode());

        RecommendationResponse response = new RecommendationResponse();
        List<RecommendedEnterprise> recommendedList = new ArrayList<>();

        try {
            // 1. 从数据库检索候选企业（不按经营状态过滤）
            String industry = getIndustryByChainId(request.getChainId());
            List<com.industrybrain.domain.model.Enterprise> entList;
            if (industry != null && !industry.isEmpty()) {
                entList = enterpriseMapper.searchByName(industry, request.getLimit() != null ? request.getLimit() * 3 : 60);
            } else {
                entList = enterpriseMapper.selectList(null);
            }
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

            // 2. 对每个候选企业运行三维度评分
            for (Map<String, Object> candidate : candidateEnterprises) {
                Long enterpriseId = toLong(candidate.get("id"));
                if (enterpriseId == null) continue;

                try {
                    MatchScoreResult scoredResult = calculateMatchScore(enterpriseId, request.getChainId(), demandProvince);

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
    public MatchScoreResult calculateMatchScore(Long enterpriseId, Long chainId, String demandProvince) {
        log.info("=== 三维度综合评分计算: enterpriseId={}, chainId={}, demandProvince={} ===",
                enterpriseId, chainId, demandProvince);

        // 调用三维度匹配度计算
        MatchScoreResult matchResult = matchScoreService.calculateMatch(enterpriseId, chainId, demandProvince);

        // 从三维度结果中提取得分
        double businessFit = matchResult.getBusinessFitScore() != null
                ? matchResult.getBusinessFitScore().doubleValue() : 0.0;
        double scaleStrength = matchResult.getScaleStrengthScore() != null
                ? matchResult.getScaleStrengthScore().doubleValue() : 0.0;
        double regionalSynergy = matchResult.getRegionalSynergyScore() != null
                ? matchResult.getRegionalSynergyScore().doubleValue() : 0.0;

        double compositeScore = matchResult.getOverallScore() != null
                ? matchResult.getOverallScore().doubleValue() : 0.0;

        // 评级
        String rating;
        if (compositeScore >= RATING_A_THRESHOLD) {
            rating = "A";
        } else if (compositeScore >= RATING_B_THRESHOLD) {
            rating = "B";
        } else {
            rating = "C";
        }

        // 更新匹配详情
        matchResult.setMatchLevel(rating);

        // 持久化到investment_recommendation表（三维度 v2）
        try {
            InvestmentRecommendation rec = recommendationMapper.selectByEnterpriseAndNode(enterpriseId, chainId);
            if (rec != null) {
                rec.setBusinessFitScore(matchResult.getBusinessFitScore());
                rec.setScaleStrengthScore(BigDecimal.valueOf(scaleStrength).setScale(2, RoundingMode.HALF_UP));
                rec.setRegionalSynergyScore(matchResult.getRegionalSynergyScore());
                rec.setTotalScore(BigDecimal.valueOf(compositeScore).setScale(2, RoundingMode.HALF_UP));
                rec.setRating(rating);
                rec.setMatchReasons(matchResult.getMatchDetail());
                rec.setDemandProvince(demandProvince);
                rec.setStatus("pending");
                rec.setRecommendationDate(LocalDate.now());
                recommendationMapper.update(rec);
            } else {
                InvestmentRecommendation newRec = new InvestmentRecommendation();
                newRec.setEnterpriseId(enterpriseId);
                newRec.setTargetNodeId(chainId);
                newRec.setBusinessFitScore(matchResult.getBusinessFitScore());
                newRec.setScaleStrengthScore(BigDecimal.valueOf(scaleStrength).setScale(2, RoundingMode.HALF_UP));
                newRec.setRegionalSynergyScore(matchResult.getRegionalSynergyScore());
                newRec.setTotalScore(BigDecimal.valueOf(compositeScore).setScale(2, RoundingMode.HALF_UP));
                newRec.setRating(rating);
                newRec.setMatchReasons(matchResult.getMatchDetail());
                newRec.setDemandProvince(demandProvince);
                newRec.setStatus("pending");
                newRec.setRecommendationDate(LocalDate.now());
                recommendationMapper.insert(newRec);
            }
        } catch (Exception e) {
            log.warn("三维度评分持久化失败: {}", e.getMessage());
        }

        log.info("=== 三维度综合评分完成: enterpriseId={}, composite={}, rating={} ===",
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

            if (minioFileService == null) {
                log.warn("MinIO 未启用，返回本地文件名");
                return fileName;
            }
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

    // ==================== 私有辅助方法 ====================

    /**
     * 从区域编码提取需求方省份（前2位）
     */
    private String extractDemandProvince(String regionCode) {
        if (regionCode != null && regionCode.length() >= 2) {
            return regionCode.substring(0, 2);
        }
        return null;
    }

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

        rec.setBusinessFitScore(scored.getBusinessFitScore());
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
                    r.createCell(4).setCellValue(ent.getBusinessFitScore() != null ? ent.getBusinessFitScore().doubleValue() : 0);
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
        stats.setHighValuePatents((int) patentMapper.selectHighValueCountByEnterprise(enterpriseId));
        stats.setInventionCount(patentMapper.selectInventionCount(enterpriseId));
        response.setStats(stats);

        ScoreBreakdown scores = new ScoreBreakdown();
        scores.setMatchScore(rec.getTotalScore());
        scores.setBusinessFitScore(rec.getBusinessFitScore());
        scores.setScaleStrengthScore(rec.getScaleStrengthScore());
        scores.setRegionalSynergyScore(rec.getRegionalSynergyScore());
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
                    // 从第一条记录获取需求方省份（regionCode前2位）
                    String demandProvince = null;
                    InvestmentRecommendation first = existingRecs.get(0);
                    if (first.getRegionCode() != null && first.getRegionCode().length() >= 2) {
                        demandProvince = first.getRegionCode().substring(0, 2);
                    }

                    List<Long> enterpriseIds = existingRecs.stream()
                            .map(InvestmentRecommendation::getEnterpriseId)
                            .distinct()
                            .collect(Collectors.toList());

                    log.info("重新计算 {} 家企业的匹配度, demandProvince={}", enterpriseIds.size(), demandProvince);
                    matchScoreService.batchCalculate(enterpriseIds, nodeId, demandProvince);

                    for (InvestmentRecommendation rec : existingRecs) {
                        try {
                            MatchScoreResult scored = matchScoreService.calculateMatch(rec.getEnterpriseId(), nodeId, demandProvince);
                            double composite = scored.getOverallScore() != null ? scored.getOverallScore().doubleValue() : 0;
                            rec.setBusinessFitScore(scored.getBusinessFitScore());
                            rec.setScaleStrengthScore(scored.getScaleStrengthScore());
                            rec.setRegionalSynergyScore(scored.getRegionalSynergyScore());
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
        record.setUserId(1L); // 默认系统用户

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
                    if (minioFileService != null) {
                        minioFileService.delete(objectName);
                    }
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
            if (minioFileService == null) {
                log.warn("MinIO 未启用，跳过报告上传");
                record.setStatus("failed");
                reportMapper.update(record);
                return;
            }
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
            if (minioFileService == null) {
                log.warn("MinIO 未启用，跳过批量导出上传");
                return;
            }
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
                r.createCell(6).setCellValue(ent.getBusinessFitScore() != null ? ent.getBusinessFitScore().doubleValue() : 0);
                r.createCell(7).setCellValue(ent.getScaleStrengthScore() != null ? ent.getScaleStrengthScore().doubleValue() : 0);
                r.createCell(8).setCellValue(ent.getRegionalSynergyScore() != null ? ent.getRegionalSynergyScore().doubleValue() : 0);
                r.createCell(9).setCellValue(ent.getTotalScore() != null ? ent.getTotalScore().doubleValue() : 0);

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
        ent.setScale(rec.getEmployeeScale());
        ent.setMatchScore(rec.getTotalScore() != null
                ? rec.getTotalScore().setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        ent.setMatchLevel(getMatchLevelKey(rec.getTotalScore()));
        ent.setMatchLevelText(getMatchLevelText(rec.getTotalScore()));
        ent.setBusinessFitScore(rec.getBusinessFitScore());
        ent.setScaleStrengthScore(rec.getScaleStrengthScore());
        ent.setRegionalSynergyScore(rec.getRegionalSynergyScore());
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
