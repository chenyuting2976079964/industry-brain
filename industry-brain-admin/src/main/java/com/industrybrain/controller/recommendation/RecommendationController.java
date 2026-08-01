package com.industrybrain.controller.recommendation;

import com.industrybrain.common.core.domain.AjaxResult;
import com.industrybrain.dto.request.*;
import com.industrybrain.service.IRecommendationService;
import com.industrybrain.service.ITechnicalProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 招商推荐控制器 — 统一招商推荐、精准招商、招商报告
 * 对应产品模块：选目标（弱链定位、目标筛选）+ 引进来（技术画像、招商评估）+ 招商清单 + 招商报告
 */
@RestController
@ConditionalOnProperty(name = "mysql.enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("/api/v1/recommendation")
public class RecommendationController {

    private static final Logger log = LoggerFactory.getLogger(RecommendationController.class);

    @Resource
    private IRecommendationService recommendationService;

    @Resource
    private ITechnicalProfileService technicalProfileService;

    // ==================== 招商推荐 ====================

    /** 获取招商推荐列表 */
    @PostMapping("/list")
    public AjaxResult getRecommendations(@RequestBody RecommendationRequest request) {
        return AjaxResult.success(recommendationService.getRecommendations(request));
    }

    /** 计算匹配度得分 */
    @GetMapping("/match/{enterpriseId}/{chainId}")
    public AjaxResult calculateMatch(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable("chainId") Long chainId,
            @RequestParam(value = "demandProvince", required = false) String demandProvince) {
        return AjaxResult.success(recommendationService.calculateMatchScore(enterpriseId, chainId, demandProvince));
    }

    /** 生成企业技术画像 */
    @GetMapping("/profile/{enterpriseId}")
    public AjaxResult getTechnicalProfile(@PathVariable("enterpriseId") Long enterpriseId) {
        return AjaxResult.success(technicalProfileService.generateProfile(enterpriseId));
    }

    /** 导出招商推荐报告 */
    @PostMapping("/export")
    public AjaxResult exportReport(@RequestBody RecommendationRequest request) {
        String filePath = recommendationService.exportRecommendations(request);
        return AjaxResult.success(filePath);
    }

    // ==================== 精准招商 ====================

    /** I01 — 获取招商匹配推荐列表（分页+多条件筛选） */
    @GetMapping("/match-list")
    public AjaxResult getMatchList(@ModelAttribute InvestmentMatchRequest request) {
        log.info("I01 招商匹配列表查询: weakness={}, matchMin={}, keyword={}, page={}",
                request.getWeakness(), request.getMatchMin(), request.getKeyword(), request.getPage());
        return AjaxResult.success(recommendationService.getMatchList(request));
    }

    /** I02 — 获取企业详情面板 */
    @GetMapping("/enterprise/{id}")
    public AjaxResult getEnterpriseDetail(@PathVariable("id") Long enterpriseId,
                                          @RequestParam(value = "nodeId", required = false) Long nodeId) {
        log.info("I02 企业招商详情: enterpriseId={}, nodeId={}", enterpriseId, nodeId);
        return AjaxResult.success(recommendationService.getEnterpriseDetail(enterpriseId, nodeId));
    }

    /** I03 — 获取招商清单列表 */
    @GetMapping("/wishlist")
    public AjaxResult getWishlist() {
        log.info("I03 查询招商清单");
        return AjaxResult.success(recommendationService.getWishlist());
    }

    /** I04 — 加入招商清单 */
    @PostMapping("/wishlist")
    public AjaxResult addToWishlist(@RequestBody WishlistAddRequest request) {
        log.info("I04 加入清单: enterpriseId={}, nodeId={}", request.getEnterpriseId(), request.getNodeId());
        recommendationService.addToWishlist(request);
        return AjaxResult.success();
    }

    /** I05 — 移出招商清单 */
    @DeleteMapping("/wishlist/{id}")
    public AjaxResult removeFromWishlist(@PathVariable("id") Long id) {
        log.info("I05 移出清单: id={}", id);
        recommendationService.removeFromWishlist(id);
        return AjaxResult.success();
    }

    /** I06 — 触发重新匹配 */
    @PostMapping("/rematch")
    public AjaxResult reMatch(@RequestParam(value = "nodeId", required = false) Long nodeId) {
        log.info("I06 重新匹配: nodeId={}", nodeId);
        String taskId = recommendationService.reMatch(nodeId);
        return AjaxResult.success(taskId);
    }

    // ==================== 招商报告 ====================

    /** R01 — 获取已生成报告列表 */
    @GetMapping("/reports")
    public AjaxResult getReports(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                  @RequestParam(value = "size", defaultValue = "10") Integer size) {
        log.info("R01 报告列表查询: page={}, size={}", page, size);
        return AjaxResult.success(recommendationService.getReports(page, size));
    }

    /** R02 — 获取报告详情/预览 */
    @GetMapping("/reports/{id}")
    public AjaxResult getReportDetail(@PathVariable("id") Long id) {
        log.info("R02 报告详情: id={}", id);
        return AjaxResult.success(recommendationService.getReportDetail(id));
    }

    /** R03 — 生成新报告 */
    @PostMapping("/reports/generate")
    public AjaxResult generateReport(@RequestBody ReportGenerateRequest request) {
        log.info("R03 生成报告: type={}, format={}, scope={}", request.getType(), request.getFormat(), request.getScope());
        String taskId = recommendationService.generateReport(request);
        return AjaxResult.success(taskId);
    }

    /** R04 — 下载报告 */
    @GetMapping("/reports/{id}/download")
    public AjaxResult downloadReport(@PathVariable("id") Long id) {
        log.info("R04 下载报告: id={}", id);
        String fileUrl = recommendationService.downloadReport(id);
        if (fileUrl == null) {
            return AjaxResult.error("报告文件尚未生成或不存在");
        }
        return AjaxResult.success(fileUrl);
    }

    /** R05 — 删除报告 */
    @DeleteMapping("/reports/{id}")
    public AjaxResult deleteReport(@PathVariable("id") Long id) {
        log.info("R05 删除报告: id={}", id);
        recommendationService.deleteReport(id);
        return AjaxResult.success();
    }

    /** R06 — 获取报告模板列表 */
    @GetMapping("/reports/templates")
    public AjaxResult getTemplates() {
        log.info("R06 报告模板列表");
        return AjaxResult.success(recommendationService.getTemplates());
    }

    /** R07 — 获取报告统计 */
    @GetMapping("/reports/stats")
    public AjaxResult getStats() {
        log.info("R07 报告统计");
        return AjaxResult.success(recommendationService.getStats());
    }

    /** R08 — 批量导出全部 */
    @PostMapping("/reports/export-all")
    public AjaxResult exportAll(@RequestBody ExportAllRequest request) {
        log.info("R08 批量导出: format={}", request.getFormat());
        String taskId = recommendationService.exportAll(request);
        return AjaxResult.success(taskId);
    }
}
