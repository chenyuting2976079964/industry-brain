package com.industrybrain.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 招商推荐结果响应DTO
 */
@Data
public class RecommendationResponse {

    /** 推荐企业列表 */
    private List<RecommendedEnterprise> enterprises;

    /** 总记录数 */
    private Integer total;

    @Data
    public static class RecommendedEnterprise {
        /** 企业ID */
        private Long enterpriseId;

        /** 企业名称 */
        private String enterpriseName;

        /** 所属行业 */
        private String industry;

        /** 区域编码 */
        private String regionCode;

        /** 业务契合度得分（维度1） */
        private BigDecimal businessFitScore;

        /** 规模实力得分（维度2） */
        private BigDecimal scaleStrengthScore;

        /** 地域协同度得分（维度3） */
        private BigDecimal regionalSynergyScore;

        /** 综合招商价值得分 */
        private BigDecimal overallScore;

        /** 匹配等级 */
        private String matchLevel;

        /** 核心专利列表 */
        private List<String> corePatents;

        /** 推荐理由 */
        private String reason;
    }
}
