package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 产业链节点实体 (chain_node表) — v2.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chain_node")
public class ChainNode {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long chainId;

    @TableField("name")
    private String nodeName;

    private Long parentId;

    @TableField(exist = false)
    private String nodeCode;

    /** 层级：up上游 / mid中游 / down下游 (v2.1) */
    private String layer;

    /** 节点描述 (v2.1) */
    private String description;

    /** 代表企业，逗号分隔 (v2.1) */
    private String firms;

    @TableField(exist = false)
    private Integer nodeImportance;

    private Integer sortOrder;

    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
