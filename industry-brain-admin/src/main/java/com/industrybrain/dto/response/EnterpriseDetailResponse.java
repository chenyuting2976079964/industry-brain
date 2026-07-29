package com.industrybrain.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 企业详情响应DTO
 */
@Data
public class EnterpriseDetailResponse {

    /** 企业ID */
    private Long id;

    /** 企业名称 */
    private String enterpriseName;

    /** 信用代码 */
    private String creditCode;

    /** 法定代表人 */
    private String legalPerson;

    /** 注册资本（万元） */
    private BigDecimal registeredCapital;

    /** 所属行业 */
    private String industry;

    /** 所属区域 */
    private String regionCode;

    /** 企业地址 */
    private String address;

    /** 员工人数 */
    private Integer employeeCount;

    /** 年营收（万元） */
    private BigDecimal annualRevenue;

    /** 专利总数 */
    private Integer patentCount;

    /** 研发占比（%） */
    private BigDecimal rdRatio;

    /** 标签列表 */
    private String tags;
}
