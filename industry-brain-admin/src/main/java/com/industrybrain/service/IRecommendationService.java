package com.industrybrain.service;

import com.industrybrain.domain.model.MatchScoreResult;
import com.industrybrain.dto.request.*;
import com.industrybrain.dto.response.*;

import java.util.List;
import java.util.Map;

/**
 * 招商推荐业务接口 — 统一招商推荐、精准招商、招商报告
 * 涵盖：推荐列表、匹配度、技术画像、招商清单、报告CRUD、模板、统计、导出
 */
public interface IRecommendationService {

    // ==================== 招商推荐 ====================

    /** 获取招商推荐列表 */
    RecommendationResponse getRecommendations(RecommendationRequest request);

    /** 计算匹配度得分 */
    MatchScoreResult calculateMatchScore(Long enterpriseId, Long chainId, String demandProvince);

    /** 导出推荐结果 */
    String exportRecommendations(RecommendationRequest request);

    // ==================== 精准招商 ====================

    /** 获取招商匹配推荐列表（多条件筛选+分页） */
    InvestmentMatchResponse getMatchList(InvestmentMatchRequest request);

    /** 获取企业招商详情 */
    EnterpriseInvestmentDetailResponse getEnterpriseDetail(Long enterpriseId, Long nodeId);

    /** 获取招商清单列表 */
    WishlistResponse getWishlist();

    /** 加入招商清单 */
    void addToWishlist(WishlistAddRequest request);

    /** 移出招商清单 */
    void removeFromWishlist(Long id);

    /** 触发重新匹配 */
    String reMatch(Long nodeId);

    // ==================== 招商报告 ====================

    /** 获取已生成报告列表 */
    ReportListResponse getReports(Integer page, Integer size);

    /** 获取报告详情 */
    ReportDetailResponse getReportDetail(Long id);

    /** 生成新报告 */
    String generateReport(ReportGenerateRequest request);

    /** 下载报告 */
    String downloadReport(Long id);

    /** 删除报告 */
    void deleteReport(Long id);

    /** 获取报告模板列表 */
    List<Map<String, Object>> getTemplates();

    /** 获取报告统计 */
    ReportStatsResponse getStats();

    /** 批量导出全部 */
    String exportAll(ExportAllRequest request);
}
