package com.industrybrain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainStrengthResponse {
    private String status;
    private String label;
    private Long count;
    private Double percentage;
    private String color;
}
