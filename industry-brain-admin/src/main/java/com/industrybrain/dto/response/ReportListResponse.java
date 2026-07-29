package com.industrybrain.dto.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 报告列表响应DTO
 * 对应 API: R01 GET /api/reports
 */
@Data
public class ReportListResponse {

    /** 报告列表 */
    private List<ReportItem> reports;

    /** 总记录数 */
    private Integer total;

    /** 当前页码 */
    private Integer page;

    /** 每页大小 */
    private Integer size;

    @Data
    public static class ReportItem {

        /** 报告ID */
        private Long id;

        /** 报告标题 */
        private String title;

        /** 报告类型 */
        private String reportType;

        /** 报告类型图标 */
        private String icon;

        /** 报告类型图标颜色 */
        private String iconColor;

        /** 报告日期 */
        private Date reportDate;

        /** 企业数量 */
        private Integer enterpriseCount;

        /** 节点数量 */
        private Integer nodeCount;

        /** 薄弱环节数 */
        private Integer weakNodeCount;

        /** 输出格式 */
        private String format;

        /** 文件大小 */
        private Long fileSize;

        /** 状态 */
        private String status;
    }
}
