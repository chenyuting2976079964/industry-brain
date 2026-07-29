package com.industrybrain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainNodeResponse {
    private Long id;
    private Long chainId;
    private String nodeName;
    private String nodeLevel;       // upstream/midstream/downstream
    private String nodeLevelCode;   // 1/2/3
    private String nodeLevelLabel;  // 上游/中游/下游
    private String diagnosisTag;    // strong/supplement/weak/missing
    private String diagnosisLabel;  // 强链/补链/弱链/缺链
    private String diagnosisColor;  // hex color
    private Integer enterpriseCount;
    private Integer patentCount;
    private Integer highValueCount;
    private Double highValueRatio;
    private Integer sortOrder;
    private String techLabels;      // JSON array
}
