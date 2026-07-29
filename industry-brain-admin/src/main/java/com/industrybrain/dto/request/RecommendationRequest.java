package com.industrybrain.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 招商推荐请求DTO
 */
@Data
public class RecommendationRequest {

    /** 产业链ID */
    private Long chainId;

    /** 目标环节列表 */
    private List<String> targetLinks;

    /** 区域编码 */
    private String regionCode;

    /** 最小匹配度阈值（0-100） */
    private Integer minThreshold = 60;

    /** 返回条数上限 */
    private Integer limit = 20;
}
