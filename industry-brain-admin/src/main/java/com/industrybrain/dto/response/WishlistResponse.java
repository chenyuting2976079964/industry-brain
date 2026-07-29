package com.industrybrain.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 招商清单响应DTO
 * 对应 API: I03 GET /api/investment/wishlist
 */
@Data
public class WishlistResponse {

    /** 清单列表 */
    private List<WishlistItem> items;

    /** 总数 */
    private Integer total;

    @Data
    public static class WishlistItem {

        /** 清单记录ID */
        private Long id;

        /** 企业ID */
        private Long enterpriseId;

        /** 企业名称 */
        private String enterpriseName;

        /** 企业所属行业 */
        private String industry;

        /** 匹配度 */
        private BigDecimal matchScore;

        /** 评级 */
        private String rating;

        /** 节点名称 */
        private String nodeName;

        /** 诊断标签 */
        private String diagnosisTag;

        /** 加入时间 */
        private Date addedAt;
    }
}
