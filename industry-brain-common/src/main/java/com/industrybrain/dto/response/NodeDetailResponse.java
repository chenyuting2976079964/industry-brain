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
public class NodeDetailResponse {
    private Long id;
    private String nodeName;
    private String nodeLevel;
    private String nodeLevelCode;
    private String nodeLevelLabel;
    private String diagnosisTag;
    private String diagnosisLabel;
    private String diagnosisColor;
    private String description;
    private Integer enterpriseCount;
    private Integer patentCount;
    private Integer highValueCount;
    private Double highValueRatio;
    private List<AssociatedEnterprise> enterprises;  // top 5-10

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssociatedEnterprise {
        private Long enterpriseId;
        private String enterpriseName;
        private String shortName;
        private Integer patentCount;
        private String scale;
        private List<String> tags;
    }
}
