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
public class DiagnosisSummaryResponse {
    private Integer strongCount;
    private Integer supplementCount;
    private Integer weakCount;
    private Integer missingCount;
    private Integer totalCount;
    private List<CategoryDetail> categories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDetail {
        private String status;
        private String label;
        private Integer count;
        private String description;
        private String color;
    }
}
