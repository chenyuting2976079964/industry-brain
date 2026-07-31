package com.industrybrain.controller.industry;

import com.industrybrain.common.core.domain.AjaxResult;
import com.industrybrain.dto.request.*;
import com.industrybrain.dto.response.*;
import com.industrybrain.service.IDashboardService;
import com.industrybrain.service.IDiagnosisService;
import com.industrybrain.service.IHomeDashboardService;
import com.industrybrain.service.IIndustryChainService;
import com.industrybrain.service.impl.GraphBuildService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 产业链 Controller — 统一入口
 * 涵盖：产业链全景(C01~C09) + 强弱诊断(D01~D04) + 全景大屏(F01~F06) + 首页看板(H01~H11)
 *
 * @author 何锦鹏
 */
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mysql.enabled", havingValue = "true", matchIfMissing = true)
public class IndustryController {

    private final IIndustryChainService industryChainService;
    private final IDiagnosisService diagnosisService;
    private final IDashboardService dashboardService;
    private final IHomeDashboardService homeDashboardService;
    private final GraphBuildService graphBuildService;

    // ==================== C01~C09: 产业链全景 ====================

    /** C01: 获取产业链目录列表 */
    @GetMapping("/api/v1/industry/chain/list")
    public AjaxResult chainList() {
        return AjaxResult.success(industryChainService.getChainList());
    }

    /** C02: 获取产业链全部节点 (含四档诊断标签) */
    @GetMapping("/api/v1/industry/chain/{id}/nodes")
    public AjaxResult chainNodes(@PathVariable Long id) {
        return AjaxResult.success(industryChainService.getChainNodes(id));
    }

    /** C03: 获取节点关联关系 */
    @GetMapping("/api/v1/industry/chain/{id}/links")
    public AjaxResult chainLinks(@PathVariable Long id) {
        return AjaxResult.success(industryChainService.getChainLinks(id));
    }

    /** C04: 获取节点详情 (含关联企业) */
    @GetMapping("/api/v1/industry/chain/node/{nodeId}")
    public AjaxResult chainNodeDetail(@PathVariable Long nodeId) {
        NodeDetailResponse detail = industryChainService.getChainNodeDetail(nodeId);
        if (detail == null) return AjaxResult.error(404, "Node not found");
        return AjaxResult.success(detail);
    }

    /** C05: 创建产业链节点 */
    @PostMapping("/api/v1/industry/chain/node")
    public AjaxResult addChainNode(@RequestBody ChainNodeCreateRequest req) {
        industryChainService.createNode(req.getChainId(), req.getNodeName(), req.getNodeCode(),
                req.getLayer(), req.getSortOrder());
        return AjaxResult.success();
    }

    /** C06: 更新节点信息 */
    @PutMapping("/api/v1/industry/chain/node/{id}")
    public AjaxResult updateChainNode(@PathVariable Long id, @RequestBody ChainNodeUpdateRequest req) {
        industryChainService.updateNode(id, req.getNodeName(), req.getNodeCode(),
                req.getLayer(), req.getSortOrder());
        return AjaxResult.success();
    }

    /** C07: 删除节点 (级联删关联) */
    @DeleteMapping("/api/v1/industry/chain/node/{id}")
    public AjaxResult deleteChainNode(@PathVariable Long id) {
        industryChainService.deleteNode(id);
        return AjaxResult.success();
    }

    /** C08: 创建节点关联 */
    @PostMapping("/api/v1/industry/chain/link")
    public AjaxResult addChainLink(@RequestBody ChainLinkCreateRequest req) {
        industryChainService.createLink(req.getSourceNodeId(), req.getTargetNodeId(), req.getRelationType());
        return AjaxResult.success();
    }

    /** C09: 删除节点关联 */
    @DeleteMapping("/api/v1/industry/chain/link/{id}")
    public AjaxResult deleteChainLink(@PathVariable Long id) {
        industryChainService.deleteLink(id);
        return AjaxResult.success();
    }

    /** C10: 重建产业链图谱（IPC 专利自动聚合生成链/节点/连线/专利映射/诊断） */
    @PostMapping("/api/v1/industry/chain/rebuild-graph")
    @org.springframework.cache.annotation.CacheEvict(value = "chain:list", allEntries = true)
    public AjaxResult rebuildGraph() {
        try {
            graphBuildService.rebuildGraph();
            return AjaxResult.success("产业链图谱重建完成");
        } catch (Exception e) {
            return AjaxResult.error("产业链图谱重建失败: " + e.getMessage());
        }
    }

    // ==================== D01~D04: 强弱诊断 ====================

    /** D01: 获取诊断列表 */
    @GetMapping("/api/v1/diagnosis/list")
    public AjaxResult diagnosisList(@RequestParam(required = false) String status,
                                    @RequestParam(required = false, defaultValue = "340500") String regionCode) {
        return AjaxResult.success(diagnosisService.getDiagnosisList(regionCode, status));
    }

    /** D02: 获取节点诊断详情 */
    @GetMapping("/api/v1/diagnosis/node/{id}")
    public AjaxResult diagnosisNodeDetail(@PathVariable Long id) {
        DiagnosisNodeDetailResponse detail = diagnosisService.getNodeDetail(id);
        if (detail == null) return AjaxResult.error(404, "Diagnosis record not found");
        return AjaxResult.success(detail);
    }

    /** D03: 触发重新诊断 */
    @PostMapping("/api/v1/diagnosis/recalc")
    public AjaxResult diagnosisRecalc(@RequestParam(required = false, defaultValue = "340500") String regionCode) {
        String taskId = diagnosisService.triggerRecalc(regionCode);
        return AjaxResult.success(diagnosisService.getRecalcStatus(taskId));
    }

    /** D03: 查询诊断任务状态 */
    @GetMapping("/api/v1/diagnosis/recalc/status/{taskId}")
    public AjaxResult diagnosisRecalcStatus(@PathVariable String taskId) {
        return AjaxResult.success(diagnosisService.getRecalcStatus(taskId));
    }

    /** D04: 获取诊断汇总统计 */
    @GetMapping("/api/v1/diagnosis/summary")
    public AjaxResult diagnosisSummary(@RequestParam(required = false, defaultValue = "340500") String regionCode) {
        return AjaxResult.success(diagnosisService.getSummary(regionCode));
    }

    // ==================== F01~F06: 全景大屏 ====================

    /** F01: 底部4个KPI */
    @GetMapping("/api/v1/dashboard/kpi")
    public AjaxResult dashboardKpi(@RequestParam(required = false, defaultValue = "340500") String regionCode) {
        return AjaxResult.success(dashboardService.getKpi(regionCode));
    }

    /** F02: 企业分布5格统计 */
    @GetMapping("/api/v1/dashboard/enterprise-dist")
    public AjaxResult dashboardEnterpriseDist(@RequestParam(required = false, defaultValue = "340500") String regionCode) {
        return AjaxResult.success(dashboardService.getEnterpriseDistribution(regionCode));
    }

    /** F03: 技术领域企业分布 (柱状图) */
    @GetMapping("/api/v1/dashboard/tech-dist")
    public AjaxResult dashboardTechDist(@RequestParam(required = false, defaultValue = "340500") String regionCode) {
        return AjaxResult.success(dashboardService.getTechDistribution(regionCode));
    }

    /** F04: 高价值专利TOP5 */
    @GetMapping("/api/v1/dashboard/patent-top5")
    public AjaxResult dashboardPatentTop5(@RequestParam(required = false, defaultValue = "340500") String regionCode) {
        return AjaxResult.success(dashboardService.getPatentTop5(regionCode));
    }

    /** F05: 产业链强弱分布 (环形图) */
    @GetMapping("/api/v1/dashboard/chain-strength")
    public AjaxResult dashboardChainStrength(@RequestParam(required = false, defaultValue = "340500") String regionCode) {
        return AjaxResult.success(dashboardService.getChainStrengthDistribution(regionCode));
    }

    /** F06: 地图标注点 */
    @GetMapping("/api/v1/dashboard/map-markers")
    public AjaxResult dashboardMapMarkers(@RequestParam(required = false, defaultValue = "340500") String regionCode) {
        return AjaxResult.success(dashboardService.getMapMarkers(regionCode));
    }

    // ==================== H01~H11: 首页看板 ====================

    /** H01: KPI 三卡片数据 */
    @GetMapping("/api/v1/homedashboard/kpi")
    public AjaxResult homeKpi() {
        return AjaxResult.success(homeDashboardService.getKpi());
    }

    /** H02: 产业链上中下游分布 */
    @GetMapping("/api/v1/homedashboard/chain-dist")
    public AjaxResult homeChainDist() {
        return AjaxResult.success(homeDashboardService.getChainDist());
    }

    /** H03: 预警列表 */
    @GetMapping("/api/v1/homedashboard/alerts")
    public AjaxResult homeAlerts() {
        return AjaxResult.success(homeDashboardService.getAlerts());
    }

    /** H04: 忽略预警 */
    @PutMapping("/api/v1/homedashboard/alerts/{id}/dismiss")
    public AjaxResult homeDismissAlert(@PathVariable Long id) {
        homeDashboardService.dismissAlert(id);
        return AjaxResult.success(Map.of("success", true));
    }

    /** H05: 园区统计 */
    @GetMapping("/api/v1/homedashboard/park-stats")
    public AjaxResult homeParkStats() {
        return AjaxResult.success(homeDashboardService.getParkStats());
    }

    /** H06: 专利详情 */
    @GetMapping("/api/v1/homedashboard/patent-detail")
    public AjaxResult homePatentDetail() {
        return AjaxResult.success(homeDashboardService.getPatentDetail());
    }

    /** H07: 企业详情（5阶梯） */
    @GetMapping("/api/v1/homedashboard/enterprise-detail")
    public AjaxResult homeEnterpriseDetail() {
        return AjaxResult.success(homeDashboardService.getEnterpriseDetail());
    }

    /** H08: 诊断汇总 */
    @GetMapping("/api/v1/homedashboard/diagnosis-summary")
    public AjaxResult homeDiagnosisSummary() {
        return AjaxResult.success(homeDashboardService.getDiagnosisSummary());
    }

    /** H09: 预警详情 */
    @GetMapping("/api/v1/homedashboard/alert-detail/{id}")
    public AjaxResult homeAlertDetail(@PathVariable Long id) {
        return AjaxResult.success(homeDashboardService.getAlertDetail(id));
    }

    /** H10: 园区专利技术领域分布 */
    @GetMapping("/api/v1/homedashboard/park-patent-detail")
    public AjaxResult homeParkPatentDetail(@RequestParam String parkName) {
        return AjaxResult.success(homeDashboardService.getParkPatentDetail(parkName));
    }

    /** H11: 园区高价值专利 TOP5 */
    @GetMapping("/api/v1/homedashboard/park-highvalue-patents")
    public AjaxResult homeParkHighValuePatents(@RequestParam String parkName) {
        return AjaxResult.success(homeDashboardService.getParkHighValuePatents(parkName));
    }
}
