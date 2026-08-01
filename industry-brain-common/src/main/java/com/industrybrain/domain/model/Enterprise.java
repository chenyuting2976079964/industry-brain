package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import com.industrybrain.common.typehandler.EnterpriseTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 企业信息实体 (enterprise表)
 * @author Developer #2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("enterprise")
public class Enterprise {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 外部数据源ID */
    private String externalId;

    /** 企业名称 */
    private String name;

    /** 统一社会信用代码 */
    private String unifiedCode;

    /** 法定代表人 */
    private String legalRepresentative;

    /** 注册资本 */
    private String registeredCapital;

    /** 成立日期 */
    private LocalDate establishmentDate;

    /** 经营状态 */
    private String businessStatus;

    /** 注册地址 */
    private String registeredAddress;

    /** 联系电话 */
    private String contactPhone;

    /** 企业简介 (v2.1) */
    private String description;

    /** 所属园区ID (v2.1) */
    private Long parkId;

    /** 企业类型：龙头企业/专精特新/高新技术企业/科技型中小企业/其他 (v2.1) */
    @TableField(typeHandler = EnterpriseTypeHandler.class)
    private String enterpriseType;

    /** 人员规模 (v2.1) */
    private String employeeScale;

    /** 所属行业 (已废弃，v2.1移除) */
    @TableField(exist = false)
    private String industry;

    /** 所属城市 */
    @TableField(exist = false)
    private String city;

    /** 所属区县 */
    @TableField(exist = false)
    private String district;

    /** 工商注册号 */
    @TableField(exist = false)
    private String registrationNo;

    /** 网址 */
    @TableField(exist = false)
    private String website;

    /** 经营范围 */
    @TableField(exist = false)
    private String businessScope;

    /** 所属区域ID */
    private Long regionId;

    /** 是否本地企业: 0=否 1=是 */
    private Integer isLocal;

    /** 数据版本时间 */
    private LocalDateTime dataVersion;

    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
