package com.industrybrain.service;

import com.industrybrain.dto.request.DataImportRequest;

/**
 * 数据导入业务接口
 * 负责企业、专利等数据的批量导入管理
 */
public interface IDataImportService {

    /**
     * 执行数据导入任务
     */
    void importData(DataImportRequest request);

    /**
     * 查询导入任务状态
     */
    String getImportStatus(String taskId);
}
