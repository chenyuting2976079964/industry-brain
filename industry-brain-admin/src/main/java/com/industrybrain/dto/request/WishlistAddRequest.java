package com.industrybrain.dto.request;

import lombok.Data;

/**
 * 招商清单新增请求DTO
 * 对应 API: I04 POST /api/investment/wishlist
 */
@Data
public class WishlistAddRequest {

    /** 企业ID */
    private Long enterpriseId;

    /** 产业链节点ID */
    private Long nodeId;

    /** 推荐记录ID */
    private Long recommendationId;
}
