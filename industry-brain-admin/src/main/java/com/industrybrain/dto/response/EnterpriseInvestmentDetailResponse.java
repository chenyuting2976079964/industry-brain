package com.industrybrain.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 企业招商详情响应DTO
 * 对应 API: I02 GET /api/investment/enterprise/{id}
 */
@Data
public class EnterpriseInvestmentDetailResponse {

    /** 企业基本信息 */
    private EnterpriseInfo enterprise;

    /** 统计卡片 */
    private StatCards stats;

    /** 三维评分 */
    private ScoreBreakdown scores;

    /** 优势领域 */
    private List<AdvantageField> advantages;

    /** 技术标签云 */
    private List<String> techTags;

    /** 技术短板 */
    private String weakness;

    /** 匹配理由 */
    private List<String> matchReasons;

    /** 招商评估 */
    private InvestmentEvaluation evaluation;

    // ---- 内嵌类 ----

    @Data
    public static class EnterpriseInfo {
        private Long enterpriseId;
        private String name;
        private String initial;
        private String color;
        private List<String> tags;
        private List<String> tagStyles;
        private String description;
        private String location;
        private String establishYear;
        private String scale;
        private String registeredCapital;
        private Integer patentCount;
        private Integer highValuePatents;
        private Integer inventionPatents;
    }

    @Data
    public static class StatCards {
        private Integer totalPatents;
        private Integer highValuePatents;
        private Integer inventionCount;
        private String industryRank;
    }

    @Data
    public static class ScoreBreakdown {
        private BigDecimal matchScore;
        private String matchLevel;
        private BigDecimal businessFitScore;
        private BigDecimal scaleStrengthScore;
        private BigDecimal regionalSynergyScore;
        private BigDecimal totalScore;
        private String rating;
    }

    @Data
    public static class AdvantageField {
        private String fieldName;
        private String patentCount;
        private String description;
    }

    @Data
    public static class InvestmentEvaluation {
        private String overallAssessment;
        private String recommendationLevel;
        private List<String> keyHighlights;
        private List<String> riskFactors;
        private String suggestedApproach;
    }
}
