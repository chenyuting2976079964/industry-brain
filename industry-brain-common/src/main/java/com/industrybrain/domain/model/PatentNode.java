package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 专利-节点关联实体 (patent_node表)
 * @author 何锦鹏
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("patent_node")
public class PatentNode {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 专利ID */
    private Long patentId;

    /** 产业链节点ID */
    private Long nodeId;

    /** 匹配类型: auto / manual */
    private String matchType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
