package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 专利信息实体 (patent表)
 * @author 何锦鹏
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("patent")
public class Patent {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String patentNumber;
    private String patentName;
    private String applicant;

    /** 关联企业ID (DB: applicant_enterprise_id) */
    @TableField("applicant_enterprise_id")
    private Long enterpriseId;

    /** 向后兼容旧代码 */
    public Long getApplicantEnterpriseId() { return enterpriseId; }
    public void setApplicantEnterpriseId(Long id) { this.enterpriseId = id; }

    /** 主IPC分类号 (DB: ipc_main) */
    @TableField("ipc_main")
    private String ipcCode;

    /** 向后兼容旧代码 */
    public String getIpcMain() { return ipcCode; }
    public void setIpcMain(String v) { this.ipcCode = v; }

    private String patentType;
    private LocalDate applicationDate;
    private String legalStatus;
    private String abstractText;
    private String estimatedValue;
    private Integer maintenanceYears;
    private String dataSource;
    private Long regionId;

    // ---- 业务扩展字段 (非DB列) ----
    @TableField(exist = false) private String title;
    @TableField(exist = false) private String technologyField;
    @TableField(exist = false) private String inventor;
    @TableField(exist = false) private String grantDate;
    @TableField(exist = false) private Integer citationCount;
    @TableField(exist = false) private String chainLink;
    @TableField(exist = false) private Boolean isHighValue;

    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
