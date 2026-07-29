package com.industrybrain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardKpiResponse {
    private Long totalPatents;
    private Long highValuePatents;
    private Long coveredEnterprises;
    private Long chainNodeCount;
    private String dataUpdateTime;
}
