package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 企业-节点关联实体 (enterprise_node表)
 * @author 何锦鹏
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("enterprise_node")
public class EnterpriseNode {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 企业ID */
    private Long enterpriseId;

    /** 产业链节点ID */
    private Long nodeId;

    /** 匹配类型: auto / manual */
    private String matchType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
