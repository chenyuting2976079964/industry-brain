package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 投资推荐实体 (investment_recommendation表)
 * @author 何锦鹏
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("investment_recommendation")
public class InvestmentRecommendation {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 区域ID */
    private Long regionId;

    /** 目标节点ID */
    private Long targetNodeId;

    /** 推荐企业ID */
    private Long enterpriseId;

    /** 企业名称 (关联查询填充) */
    @TableField(exist = false)
    private String enterpriseName;

    /** 行业 (关联查询填充) */
    @TableField(exist = false)
    private String industry;

    /** 区域编码 (关联查询填充) */
    @TableField(exist = false)
    private String regionCode;

    /** 评分等级 A/B/C */
    @TableField(exist = false)
    private String rating;

    /** 节点名称 (关联查询填充) */
    @TableField(exist = false)
    private String nodeName;

    /** 诊断标签 (关联查询填充) */
    @TableField(exist = false)
    private String diagnosisTag;

    /** 匹配理由 */
    @TableField(exist = false)
    private String matchReasons;

    /** 技术匹配度得分 (0-100) */
    private BigDecimal techMatchScore;

    /** 技术实力得分 (0-100) */
    private BigDecimal techStrengthScore;

    /** 活跃度得分 (0-100) */
    private BigDecimal activeScore;

    /** 产业契合度得分 (0-100) */
    private BigDecimal industryFitScore;

    /** 带动潜力得分 (0-100) */
    private BigDecimal drivingPotentialScore;

    /** 产业链覆盖率 (0-100) */
    private BigDecimal chainCoverage;

    /** 综合得分 (0-100) */
    private BigDecimal totalScore;

    /** 距离层级 (本市/本省/邻省/其他) */
    private String distanceLevel;

    /** 状态 (pending/contacted/signed/wishlist) */
    private String status;

    /** 推荐日期 */
    private LocalDate recommendationDate;

    /** 更新时间 (关联查询填充) */
    @TableField(exist = false)
    private Date updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
