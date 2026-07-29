package com.industrybrain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecalcStatusResponse {
    private String taskId;
    private String status;    // pending/running/completed/failed
    private Integer progress; // 0-100
    private String message;
    private Long durationMs;
}
