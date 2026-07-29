package com.industrybrain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainLinkResponse {
    private Long id;
    private Long sourceNodeId;
    private String sourceNodeName;
    private Long targetNodeId;
    private String targetNodeName;
    private String relationType;
}
