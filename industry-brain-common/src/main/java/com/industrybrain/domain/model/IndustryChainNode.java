package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.industrybrain.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 产业链节点实体（业务视图，映射 chain_node 表）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chain_node")
public class IndustryChainNode extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String nodeName;

    @TableField("layer")
    private String level;

    private Long parentId;

    private Long chainId;

    private Integer sortOrder;

    private String description;

    @TableField(exist = false)
    private Integer enterpriseCount;

    @TableField(exist = false)
    private Integer patentCount;

    @TableField(exist = false)
    private String status;
}
