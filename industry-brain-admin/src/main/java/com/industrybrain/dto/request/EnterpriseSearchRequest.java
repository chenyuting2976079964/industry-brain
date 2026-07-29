package com.industrybrain.dto.request;

import lombok.Data;

/**
 * 企业搜索请求DTO
 */
@Data
public class EnterpriseSearchRequest {

    /** 企业名称（模糊搜索） */
    private String enterpriseName;

    /** 所属行业 */
    private String industry;

    /** 区域编码 */
    private String regionCode;

    /** 专利数最小值 */
    private Integer minPatentCount;

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 10;
}
