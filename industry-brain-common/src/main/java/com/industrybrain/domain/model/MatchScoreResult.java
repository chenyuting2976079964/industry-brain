package com.industrybrain.domain.model;

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

    private BigDecimal techMatchScore;

    private BigDecimal industryFitScore;

    private BigDecimal overallScore;

    private String matchLevel;

    private String matchDetail;
}
