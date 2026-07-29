package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 企业招商评估结果实体 (enterprise_evaluation表)
 * @author 何锦鹏
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("enterprise_evaluation")
public class EnterpriseEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 企业ID（一对一） */
    private Long enterpriseId;

    /** 综合评级：A / B / C */
    private String evalLevel;

    /** 评级标签：强烈推荐 / 推荐 / 值得关注 */
    private String evalLabel;

    /** 技术匹配度 0-100 */
    private Integer techMatch;

    /** 专利实力评分 0-100 */
    private Integer patentStrength;

    /** 产业带动潜力 0-100 */
    private Integer drivingPotential;

    /** 招商方式 */
    private String investType;

    /** 紧急程度：高 / 中 / 低 */
    private String urgency;

    /** 风险等级 */
    private String riskLevel;

    /** 政策分类 */
    private String policyCategory;

    /** 招商建议 */
    private String suggestion;

    /** 建议落地园区 */
    private String landSuggest;

    /** 技术短板 */
    private String weakness;

    /** 短板详情 */
    private String weaknessDetail;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
