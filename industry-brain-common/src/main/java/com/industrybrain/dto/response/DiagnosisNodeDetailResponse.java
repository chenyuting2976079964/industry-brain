package com.industrybrain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisNodeDetailResponse {
    private Long nodeId;
    private String nodeName;
    private String diagnosisTag;
    private String diagnosisLabel;
    private String nodeLevel;
    private String description;
    private Integer patentCount;
    private Integer highValueCount;
    private Double highValueRatio;
    private Double patentDensity;
    private Double percentile;
    private List<PatentHolder> patentHolders;
    private List<String> suggestions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatentHolder {
        private Long enterpriseId;
        private String enterpriseName;
        private String shortName;
        private Integer patentCount;
    }
}
