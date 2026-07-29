package com.industrybrain.dto.request;

import lombok.Data;

/**
 * 产业链全景查询请求DTO
 */
@Data
public class IndustryChainQueryRequest {

    /** 产业链ID */
    private Long chainId;

    /** 产业链名称（模糊搜索） */
    private String chainName;

    /** 区域编码 */
    private String regionCode;
}
