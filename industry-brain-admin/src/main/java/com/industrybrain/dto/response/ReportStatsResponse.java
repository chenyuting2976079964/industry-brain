package com.industrybrain.dto.response;

import lombok.Data;

/**
 * 报告统计响应DTO
 * 对应 API: R07 GET /api/reports/stats
 */
@Data
public class ReportStatsResponse {

    /** 本月生成报告数 */
    private Integer monthlyGenerated;

    /** 累计导出清单数 */
    private Integer totalExported;

    /** 覆盖企业数量 */
    private Integer coveredEnterprises;

    /** 本月生成增长率（%） */
    private Double monthlyGrowth;

    /** 累计增长率（%） */
    private Double cumulativeGrowth;
}
