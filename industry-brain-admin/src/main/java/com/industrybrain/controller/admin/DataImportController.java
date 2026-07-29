package com.industrybrain.controller.admin;

import com.industrybrain.common.core.domain.AjaxResult;
import com.industrybrain.dto.request.DataImportRequest;
import com.industrybrain.service.IDataImportService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 数据导入管理控制器
 * 对应产品模块：全局 - 数据导入管理
 */
@RestController
@RequestMapping("/admin/import")
public class DataImportController {

    @Resource
    private IDataImportService dataImportService;

    /**
     * 执行数据导入
     */
    @PostMapping
    public AjaxResult importData(@RequestBody DataImportRequest request) {
        dataImportService.importData(request);
        return AjaxResult.success("导入任务已提交");
    }

    /**
     * 查询导入任务状态
     */
    @GetMapping("/status/{taskId}")
    public AjaxResult getImportStatus(@PathVariable("taskId") String taskId) {
        String status = dataImportService.getImportStatus(taskId);
        return AjaxResult.success(status);
    }
}
