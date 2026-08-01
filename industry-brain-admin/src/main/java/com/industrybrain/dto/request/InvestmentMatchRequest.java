package com.industrybrain.dto.request;

import lombok.Data;

/**
 * 招商匹配列表请求DTO
 * 对应 API: I01 GET /api/investment/match-list
 */
@Data
public class InvestmentMatchRequest {

    /** 薄弱环节筛选 */
    private String weakness;

    /** 最小匹配度阈值 */
    private Integer matchMin;

    /** 目标规模: large/mid/small */
    private String scale;

    /** 排序: match/patent/scale */
    private String sortBy;

    /** 搜索关键词 */
    private String keyword;

    /** 产业链节点ID（来自诊断上下文） */
    private Long nodeId;

    /** 页码（从1开始） */
    private Integer page = 1;

    /** 每页大小 */
    private Integer size = 10;

    /** 子Tab: all/high/weakchain */
    private String tab = "all";
}
