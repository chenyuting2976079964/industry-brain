package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.industrybrain.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 匹配度结果业务对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("match_score_result")
public class MatchScoreResult extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long enterpriseId;

    private String enterpriseName;

    private Long chainId;

    private String chainName;

    /** 业务契合度得分（维度1） */
    private BigDecimal techMatchScore;

    /** 地域协同度得分（维度3） — 数据库字段复用 */
    private BigDecimal industryFitScore;

    /** 规模实力得分（维度2） — 仅内存传递，不写入此表 */
    @TableField(exist = false)
    private BigDecimal scaleStrengthScore;

    /** 综合得分（三维度加权） */
    private BigDecimal overallScore;

    private String matchLevel;

    private String matchDetail;
}
