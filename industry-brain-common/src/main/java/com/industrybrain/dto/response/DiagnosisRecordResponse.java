package com.industrybrain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisRecordResponse {
    private Long nodeId;
    private String nodeName;
    private String nodeLevel;
    private String nodeLevelLabel;
    private String diagnosisTag;
    private String diagnosisLabel;
    private String diagnosisColor;
    private Integer patentCount;
    private Integer highValueCount;
    private Double highValueRatio;
    private Double patentDensity;
    private Double benchmarkDensity;
    private Double percentile;
    private String diagnosisBasis;
    private String reason;
    private LocalDateTime diagnosisTime;
    private Integer version;
}
