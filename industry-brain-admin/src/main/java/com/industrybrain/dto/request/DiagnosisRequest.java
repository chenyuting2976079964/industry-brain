package com.industrybrain.dto.request;

import lombok.Data;

/**
 * 诊断请求DTO
 */
@Data
public class DiagnosisRequest {

    /** 产业链ID */
    private Long chainId;

    /** 诊断年份 */
    private String diagnosisYear;
}
