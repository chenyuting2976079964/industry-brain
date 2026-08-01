package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 报告记录实体 — 映射到 init.sql 中的 report 表
 */
@Data
@TableName("report")
public class ReportRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 报告标题 */
    private String title;

    /** 报告类型 */
    @TableField("report_type")
    private String reportType;

    /** 产业链ID (report表无此列，SQL查询时通过JOIN或额外字段填充) */
    @TableField(exist = false)
    private Long chainId;

    /** 产业链名称 (SQL查询时从industry_chain表JOIN获取) */
    @TableField(exist = false)
    private String chainName;

    /** 节点ID (report表无此列) */
    @TableField(exist = false)
    private Long nodeId;

    /** 报告范围 */
    private String scope;

    /** 覆盖企业数量 */
    @TableField("enterprise_count")
    private Integer enterpriseCount;

    /** 输出格式 PDF/XLSX，映射到 format 列 */
    @TableField("format")
    private String outputFormat;

    /** 文件URL */
    @TableField("file_url")
    private String fileUrl;

    /** 文件大小 */
    @TableField("file_size")
    private Long fileSize;

    /** 状态：pending/processing/completed/failed */
    private String status;

    /** 生成耗时(ms) */
    @TableField("generation_time_ms")
    private Long generationTime;

    /** 所属区域ID */
    @TableField("region_id")
    private Long regionId;

    /** 创建人ID */
    @TableField("user_id")
    private Long userId;

    /** 关联附件ID */
    @TableField("file_id")
    private Long fileId;

    /** 创建时间 */
    @TableField("created_at")
    private Date createdAt;

    /** 更新时间 */
    @TableField("updated_at")
    private Date updatedAt;
}
