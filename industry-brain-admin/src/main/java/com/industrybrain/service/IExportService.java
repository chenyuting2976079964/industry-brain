package com.industrybrain.service;

/**
 * 报告导出业务接口
 * 负责技术画像报告、招商推荐报告等文件的导出
 */
public interface IExportService {

    /**
     * 导出技术画像报告
     */
    String exportTechnicalProfile(Long enterpriseId);

    /**
     * 导出招商推荐报告
     */
    String exportRecommendationReport(String taskId);
}
