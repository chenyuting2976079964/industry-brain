package com.industrybrain.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * C06: 更新产业链节点请求 (v2.1)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainNodeUpdateRequest {
    private String nodeName;
    private String nodeCode;
    /** 层级: up上游 / mid中游 / down下游 (v2.1) */
    private String layer;
    private Integer sortOrder;
}
