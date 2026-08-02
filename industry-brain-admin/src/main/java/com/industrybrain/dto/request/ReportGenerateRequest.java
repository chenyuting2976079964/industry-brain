package com.industrybrain.dto.request;

import lombok.Data;

/**
 * 报告生成请求DTO
 * 对应 API: R03 POST /api/reports/generate
 */
@Data
public class ReportGenerateRequest {

    /** 报告类型: investment_list/diagnosis/enterprise_profile/chain_supplement */
    private String type;

    /** 产业链范围: all/weak_nodes/strong_nodes */
    private String scope;

    /** 目标企业数量: top10/top20/all */
    private String count;

    /** 输出格式: PDF/Excel/Word */
    private String format;

    /** 产业链ID */
    private Long chainId;

    /** 具体节点ID（可选，针对单节点报告） */
    private Long nodeId;

    /** 企业ID（可选，针对企业画像报告） */
    private Long enterpriseId;

    // ─── 招商清单导出筛选条件 ───
    /** 薄弱环节筛选 */
    private String weakness;
    /** 最小匹配度 */
    private Integer matchMin;
    /** 企业规模 */
    private String scale;
    /** 搜索关键词 */
    private String keyword;
    /** 排序方式 */
    private String sortBy;
}
