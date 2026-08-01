package com.industrybrain.dto.request;

import lombok.Data;

/**
 * 数据导入请求DTO
 */
@Data
public class DataImportRequest {

    /** 导入类型（enterprise/patent/diagnosis） */
    private String importType;

    /** 区域编码 */
    private String regionCode;

    /** 覆盖模式（0-跳过已存在 1-更新 2-全量覆盖） */
    private String overwriteMode = "0";

    /** 数据文件路径（可选，为空则使用默认目录） */
    private String filePath;
}
