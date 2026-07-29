package com.industrybrain.dto.request;

import lombok.Data;

/**
 * 批量导出请求DTO
 * 对应 API: R08 POST /api/reports/export-all
 */
@Data
public class ExportAllRequest {

    /** 输出格式: Excel/PDF */
    private String format;
}
