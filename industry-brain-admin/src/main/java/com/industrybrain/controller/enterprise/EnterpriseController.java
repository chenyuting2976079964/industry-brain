package com.industrybrain.controller.enterprise;

import com.industrybrain.common.core.domain.AjaxResult;
import com.industrybrain.domain.model.Enterprise;
import com.industrybrain.dto.PageResponse;
import com.industrybrain.dto.response.*;
import com.industrybrain.service.IEnterpriseService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 企业详情 Controller — E01~E05 + enterprise_type 扩展
 * 对标技术架构文档 §2.3 + §2.7 (EnterpriseController)
 *
 * @author 何锦鹏
 */
@RestController
@ConditionalOnProperty(name = "mysql.enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("/api/v1/enterprise")
@RequiredArgsConstructor
public class EnterpriseController {

    private final IEnterpriseService appService;

    /** E01: 获取企业基本信息 */
    @GetMapping("/{id}")
    public AjaxResult getEnterprise(@PathVariable Long id) {
        EnterpriseResponse resp = appService.getEnterprise(id);
        if (resp == null) return AjaxResult.error(404, "Enterprise not found");
        return AjaxResult.success(resp);
    }

    /** E02: 获取企业技术画像 */
    @GetMapping("/{id}/profile")
    public AjaxResult getProfile(@PathVariable Long id) {
        return AjaxResult.success(appService.getProfile(id));
    }

    /** E03: 获取企业专利列表 (分页+搜索+过滤) */
    @GetMapping("/{id}/patents")
    public AjaxResult getPatents(@PathVariable Long id,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String type) {
        List<EnterprisePatentResponse> rows = appService.getPatents(id, page, size, keyword, type);
        long total = appService.countPatents(id, keyword, type);
        return AjaxResult.success(PageResponse.of(rows, total, page, size));
    }

    /** E04: 获取企业综合评估 */
    @GetMapping("/{id}/evaluate")
    public AjaxResult getEvaluate(@PathVariable Long id) {
        return AjaxResult.success(appService.getEvaluate(id));
    }

    /** E05: 企业模糊搜索 */
    @GetMapping("/search")
    public AjaxResult search(@RequestParam String keyword) {
        return AjaxResult.success(appService.searchEnterprise(keyword));
    }

    /** 本地企业查询 (原型 local-enterprises) */
    @GetMapping("/local/list")
    public AjaxResult localEnterprises(@RequestParam(required = false) String keyword) {
        return AjaxResult.success(appService.getLocalEnterprises(keyword));
    }

    /** ── enterprise_type 新增接口 ── */

    /** 按企业类型查询企业列表 */
    @GetMapping("/listByType")
    public AjaxResult listByType(@RequestParam("type") String type) {
        List<Enterprise> enterprises = appService.getEnterprisesByType(type);
        return AjaxResult.success(enterprises);
    }

    /** 按企业类型统计企业数 */
    @GetMapping("/countByType")
    public AjaxResult countByType(@RequestParam("type") String type) {
        Long count = appService.getEnterprisesCountByType(type);
        return AjaxResult.success(Map.of("count", count, "type", type));
    }

    /** 获取企业类型分布统计 */
    @GetMapping("/typeStats")
    public AjaxResult typeStats() {
        List<Map<String, Object>> stats = appService.getEnterpriseTypeStats();
        return AjaxResult.success(stats);
    }

    /** 触发企业类型自动分类（填充 enterprise_type 为空的企业） */
    @PostMapping("/classifyTypes")
    public AjaxResult classifyTypes() {
        int count = appService.classifyEnterpriseTypes();
        return AjaxResult.success(Map.of("updated", count));
    }
}
