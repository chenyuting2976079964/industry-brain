package com.industrybrain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainResponse {
    private Long id;
    private String chainName;
    private String chainCode;
    private String description;
    private String icon;
    private Integer chainLevel;
    private Integer sortOrder;
    private Integer nodeCount;
    private Integer totalPatents;
    private Integer totalEnterprises;
    private Integer strongCount;
    private Integer supplementCount;
    private Integer weakCount;
    private Integer missingCount;
}
