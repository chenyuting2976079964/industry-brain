package com.industrybrain.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * C05: 创建产业链节点请求 (v2.1)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainNodeCreateRequest {
    @NotNull
    private Long chainId;

    @NotBlank
    private String nodeName;

    @NotBlank
    private String nodeCode;

    /** 层级: up上游 / mid中游 / down下游 (v2.1) */
    private String layer;

    private Integer sortOrder;
}
