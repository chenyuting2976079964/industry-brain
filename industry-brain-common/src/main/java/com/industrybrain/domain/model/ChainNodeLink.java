package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 产业链节点关联关系实体 (chain_node_link表) — v2.1
 * DB列: source_node_id / target_node_id
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chain_node_link")
public class ChainNodeLink {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("source_node_id")
    private Long sourceNodeId;

    @TableField("target_node_id")
    private Long targetNodeId;

    /** SUPPLIES / DEMANDS */
    private String relationType;

    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
