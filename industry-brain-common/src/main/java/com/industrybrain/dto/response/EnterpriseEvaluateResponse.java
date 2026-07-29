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
public class EnterpriseEvaluateResponse {
    private String rating;           // A/B/C
    private String ratingLabel;      // 强烈推荐/推荐/关注
    private Integer compositeScore;  // 0-100
    private List<DimensionScore> scores;
    private String summaryText;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionScore {
        private String name;
        private Integer score;
        private String description;
    }
}
