package com.industrybrain.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 招商匹配列表响应DTO
 * 对应 API: I01 GET /api/investment/match-list
 */
@Data
public class InvestmentMatchResponse {

    /** 推荐企业列表 */
    private List<InvestmentEnterprise> enterprises;

    /** 总记录数 */
    private Integer total;

    /** 当前页码 */
    private Integer page;

    /** 每页大小 */
    private Integer size;

    @Data
    public static class InvestmentEnterprise {

        /** 推荐记录ID */
        private Long recommendationId;

        /** 企业ID */
        private Long enterpriseId;

        /** 企业名称 */
        private String enterpriseName;

        /** 企业头像/首字母 */
        private String initial;

        /** 企业标签列表 */
        private List<String> tags;

        /** 标签样式 */
        private List<String> tagStyles;

        /** 企业描述 */
        private String description;

        /** 所在地区 */
        private String location;

        /** 成立年份 */
        private String establishYear;

        /** 企业规模 */
        private String scale;

        /** 注册资本 */
        private String registeredCapital;

        /** 匹配度（百分比值 0-100） */
        private BigDecimal matchScore;

        /** 匹配度等级: high/medium/low */
        private String matchLevel;

        /** 匹配等级显示文本 */
        private String matchLevelText;

        /** 业务契合度得分（0-100） */
        private BigDecimal businessFitScore;

        /** 规模实力得分（0-100） */
        private BigDecimal scaleStrengthScore;

        /** 地域协同度得分（0-100） */
        private BigDecimal regionalSynergyScore;

        /** 综合评分 */
        private BigDecimal totalScore;

        /** 评级: A/B/C */
        private String rating;

        /** 匹配理由列表 */
        private List<String> matchReasons;

        /** 是否已加入清单 */
        private Boolean inWishlist;

        /** 产业链节点名称 */
        private String nodeName;

        /** 诊断标签 */
        private String diagnosisTag;

        /** 优势领域列表 */
        private List<AdvantageField> advantages;

        /** 技术标签 */
        private List<String> techTags;

        /** 技术短板 */
        private String weakness;
    }

    @Data
    public static class AdvantageField {

        /** 领域名称 */
        private String fieldName;

        /** 专利数量 */
        private String patentCount;

        /** 领域描述 */
        private String description;
    }
}
