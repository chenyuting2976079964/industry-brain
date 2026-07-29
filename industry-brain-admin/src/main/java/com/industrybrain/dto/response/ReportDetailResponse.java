package com.industrybrain.dto.response;

import lombok.Data;

import java.util.Date;

/**
 * 报告详情响应DTO
 * 对应 API: R02 GET /api/reports/{id}
 */
@Data
public class ReportDetailResponse {

    /** 报告ID */
    private Long id;

    /** 报告标题 */
    private String title;

    /** 报告类型 */
    private String reportType;

    /** 报告类型显示名称 */
    private String reportTypeName;

    /** 产业链名称 */
    private String chainName;

    /** 报告范围 */
    private String scope;

    /** 企业数量 */
    private Integer enterpriseCount;

    /** 薄弱环节数 */
    private Integer weakNodeCount;

    /** 输出格式 */
    private String format;

    /** 文件下载URL */
    private String fileUrl;

    /** 文件大小 */
    private Long fileSize;

    /** 文件页数 */
    private Integer pageCount;

    /** 生成时间 */
    private Date generatedAt;

    /** 生成耗时（毫秒） */
    private Long generationTime;

    /** 状态 */
    private String status;
}
