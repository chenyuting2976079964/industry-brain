package com.industrybrain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatentTop5Response {
    private Integer rank;
    private String patentName;
    private Long patentId;
    private String applicant;
    private Integer score;
}
