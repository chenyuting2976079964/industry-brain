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
public class EnterpriseProfileResponse {
    private PatentStats patentStats;
    private List<AdvantageItem> advantages;
    private List<String> techTags;
    private List<WeaknessItem> weaknesses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatentStats {
        private Integer totalPatents;
        private Integer highValuePatents;
        private Integer inventionPatents;
        private Double techMatchRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdvantageItem {
        private String name;
        private Integer patentCount;
        private String description;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeaknessItem {
        private String name;
        private String description;
        private String suggestion;
    }
}
