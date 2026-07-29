package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 园区实体 (industrial_park表)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("industrial_park")
public class IndustrialPark {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long regionId;

    private String parkLevel;

    private String address;

    private String description;

    /** 园区类型：核心园区/产业聚集区/特色园区/孵化园区 (v2.1) */
    private String parkType;

    /** 企业数量 (v2.1) */
    private Integer enterpriseCount;

    /** 专利数量 (v2.1) */
    private Integer patentCount;

    /** 高价值专利数量 (v2.1) */
    private Integer highValuePatentCount;

    /** 主导产业 (v2.1) */
    private String dominantIndustries;

    /** 重点企业 (v2.1) */
    private String keyFirms;

    /** 产业链覆盖情况 (v2.1) */
    private String chainCoverage;

    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
