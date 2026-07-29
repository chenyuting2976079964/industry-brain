package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 专利-节点映射实体 (patent_node_mapping表)
 * A1算法输出: 专利到产业链节点的智能映射结果
 * @author Developer #2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("patent_node_mapping")
public class PatentNodeMapping {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 专利ID */
    private Long patentId;

    /** 产业链节点ID */
    private Long nodeId;

    /** 映射置信度 0~1.0 */
    private Double mappingConfidence;

    /** 置信度等级: HIGH / MEDIUM / LOW */
    private String confidenceLevel;

    /** 匹配类型: IPC / KEYWORD / VECTOR / PRIOR */
    private String matchType;

    /** 规则通道得分 */
    private Double ruleScore;

    /** 向量通道得分 */
    private Double vectorScore;

    /** 是否为主映射 */
    @TableField("`primary`")
    private Boolean primary;

    /** 匹配证据 */
    private String evidence;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
