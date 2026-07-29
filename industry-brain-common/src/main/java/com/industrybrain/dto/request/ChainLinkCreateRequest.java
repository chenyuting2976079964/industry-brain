package com.industrybrain.dto.request;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * C08: 创建节点关联请求 (v2.1 — SUPPLIES / DEMANDS)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainLinkCreateRequest {
    @NotNull
    private Long sourceNodeId;

    @NotNull
    private Long targetNodeId;

    /** 关系类型: SUPPLIES 或 DEMANDS (默认 SUPPLIES) */
    private String relationType;
}
