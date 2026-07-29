package com.industrybrain.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 诊断结果响应DTO
 */
@Data
public class DiagnosisResponse {

    /** 诊断ID */
    private Long id;

    /** 产业链名称 */
    private String chainName;

    /** 诊断年份 */
    private String diagnosisYear;

    /** 上游强度评分 */
    private BigDecimal upstreamStrength;

    /** 中游强度评分 */
    private BigDecimal midstreamStrength;

    /** 下游强度评分 */
    private BigDecimal downstreamStrength;

    /** 整体评级 */
    private String overallRating;

    /** 薄弱环节 */
    private String weakPoints;

    /** 建议措施 */
    private String suggestions;
}
