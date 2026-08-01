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
 * 招商推荐匹配记录实体 (investment_recommendation表 v2)
 * 三维度算法: 业务契合度(50%) + 规模实力(35%) + 地域协同度(15%)
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

    /** 区域ID（可选） */
    private Long regionId;

    /** 目标产业链节点ID */
    private Long targetNodeId;

    /** 推荐目标企业ID */
    private Long enterpriseId;

    // ==================== 三维度得分（与算法维度一一对应） ====================

    /** 业务契合度得分（维度1，权重50%） */
    private BigDecimal businessFitScore;

    /** 规模实力得分（维度2，权重35%） */
    private BigDecimal scaleStrengthScore;

    /** 地域协同度得分（维度3，权重15%） */
    private BigDecimal regionalSynergyScore;

    // ==================== 综合评分 ====================

    /** 综合得分 0-100 */
    private BigDecimal totalScore;

    /** 评级 A/B/C */
    private String rating;

    /** 匹配理由/详情文本 */
    private String matchReasons;

    // ==================== 辅助计算字段 ====================

    /** 需求方省份编码（regionCode前2位） */
    private String demandProvince;

    // ==================== 状态管理 ====================

    /** 状态: pending/contacted/signed/wishlist */
    private String status;

    /** 推荐日期 */
    private LocalDate recommendationDate;

    // ==================== 审计字段 ====================

    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ==================== 关联查询填充字段（非DB列） ====================

    /** 企业名称 */
    @TableField(exist = false)
    private String enterpriseName;

    /** 行业 */
    @TableField(exist = false)
    private String industry;

    /** 区域编码 */
    @TableField(exist = false)
    private String regionCode;

    /** 节点名称 */
    @TableField(exist = false)
    private String nodeName;

    /** 诊断标签（强链/补链/弱链/缺链） */
    @TableField(exist = false)
    private String diagnosisTag;

    /** 更新时间 */
    @TableField(exist = false)
    private Date updateTime;
}
