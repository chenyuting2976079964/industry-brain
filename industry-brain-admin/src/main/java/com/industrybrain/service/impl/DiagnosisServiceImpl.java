package com.industrybrain.service.impl;

import com.industrybrain.domain.model.*;
import com.industrybrain.dto.request.DiagnosisRequest;
import com.industrybrain.dto.response.*;
import com.industrybrain.mapper.mysql.*;
import com.industrybrain.service.IDiagnosisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 强弱链诊断业务实现 (v2.0 合并版 — D01~D04)
 * @author 何锦鹏
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisServiceImpl implements IDiagnosisService {

    private final NodeDiagnosisMapper nodeDiagnosisMapper;
    private final ChainNodeMapper chainNodeMapper;
    private final PatentMapper patentMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    @Resource
    private java.util.concurrent.Executor taskExecutor;

    private static final String TASK_KEY_PREFIX = "diagnosis:task:";

    // ─── D01: 诊断列表 ───
    @Override
    public List<DiagnosisRecordResponse> getDiagnosisList(String regionCode, String status) {
        List<NodeDiagnosis> results;
        if (status != null && !status.isEmpty() && !"all".equals(status)) {
            results = nodeDiagnosisMapper.selectByTag(status);
        } else {
            results = nodeDiagnosisMapper.selectAllLatest();
        }
        if (results == null) return Collections.emptyList();

        List<ChainNode> nodes = chainNodeMapper.selectList(null);
        Map<Long, String> nodeNameMap = nodes.stream()
            .collect(Collectors.toMap(ChainNode::getId, ChainNode::getNodeName, (a, b) -> a));
        Map<Long, String> layerMap = nodes.stream()
            .collect(Collectors.toMap(ChainNode::getId, ChainNode::getLayer, (a, b) -> a));

        return results.stream().map(r -> {
            DiagnosisRecordResponse vo = new DiagnosisRecordResponse();
            vo.setNodeId(r.getNodeId());
            vo.setNodeName(nodeNameMap.getOrDefault(r.getNodeId(), "未知"));
            String layer = layerMap.get(r.getNodeId());
            vo.setNodeLevel(layer != null ? layer : "");
            vo.setNodeLevelLabel(mapLayerLabel(layer));
            vo.setDiagnosisTag(r.getDiagnosisTag());
            vo.setDiagnosisLabel(r.getDiagnosisTag());
            vo.setDiagnosisColor(mapColor(r.getDiagnosisTag()));
            vo.setPatentCount(r.getTotalPatentCount());
            vo.setHighValueCount(r.getHighValuePatentCount());
            vo.setDiagnosisTime(r.getDiagnosisDate() != null
                ? r.getDiagnosisDate().atStartOfDay() : null);
            vo.setDiagnosisBasis(r.getDiagnosisBasis());
            return vo;
        }).collect(Collectors.toList());
    }

    // ─── D02: 节点诊断详情 ───
    @Override
    public DiagnosisNodeDetailResponse getNodeDetail(Long nodeId) {
        NodeDiagnosis dr = nodeDiagnosisMapper.selectLatestByNodeId(nodeId);
        ChainNode node = chainNodeMapper.selectById(nodeId);
        if (dr == null || node == null) return null;

        DiagnosisNodeDetailResponse vo = new DiagnosisNodeDetailResponse();
        vo.setNodeId(nodeId);
        vo.setNodeName(node.getNodeName());
        vo.setDiagnosisTag(dr.getDiagnosisTag());
        vo.setDiagnosisLabel(dr.getDiagnosisTag());
        vo.setNodeLevel(layerToString(node.getLayer()));
        vo.setDescription(dr.getDiagnosisBasis());
        vo.setPatentCount(dr.getTotalPatentCount());
        vo.setHighValueCount(dr.getHighValuePatentCount());
        vo.setSuggestions(generateSuggestions(dr.getDiagnosisTag()));

        List<Map<String, Object>> holders = chainNodeMapper.selectTopEnterprisesByNodeId(nodeId, 5);
        if (holders != null && !holders.isEmpty()) {
            vo.setPatentHolders(holders.stream().map(m -> {
                DiagnosisNodeDetailResponse.PatentHolder ph = new DiagnosisNodeDetailResponse.PatentHolder();
                ph.setEnterpriseId(toLong(m.get("enterpriseId")));
                ph.setEnterpriseName((String) m.get("name"));
                ph.setPatentCount(toInt(m.get("patentCount")));
                return ph;
            }).collect(Collectors.toList()));
        }
        return vo;
    }

    // ─── D03: 触发重新诊断 ───
    @Override
    public String triggerRecalc(String regionCode) {
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        RecalcStatusResponse status = RecalcStatusResponse.builder()
            .taskId(taskId).status("pending").progress(0).message("诊断任务已创建").build();
        redisTemplate.opsForValue().set(TASK_KEY_PREFIX + taskId, status, Duration.ofHours(1));
        taskExecutor.execute(() -> executeRecalcAsync(taskId, regionCode));
        return taskId;
    }

    @Override
    public RecalcStatusResponse getRecalcStatus(String taskId) {
        Object cached = redisTemplate.opsForValue().get(TASK_KEY_PREFIX + taskId);
        if (cached == null) {
            return RecalcStatusResponse.builder().taskId(taskId).status("unknown").message("任务不存在或已过期").build();
        }
        return (RecalcStatusResponse) cached;
    }

    // ─── D04: 诊断汇总 ───
    @Override
    public DiagnosisSummaryResponse getSummary(String regionCode) {
        List<Map<String, Object>> rows = nodeDiagnosisMapper.selectStatusSummary();
        if (rows == null) rows = Collections.emptyList();
        int strong = 0, supplement = 0, weak = 0, missing = 0;
        for (Map<String, Object> row : rows) {
            String s = (String) (row.get("diagnosisTag") != null ? row.get("diagnosisTag") : row.get("diagnosis_tag"));
            Object cntVal = row.get("nodeCount");
            if (cntVal == null) cntVal = row.get("node_count");
            int count = toInt(cntVal);
            if ("强链".equals(s)) strong = count;
            else if ("补链".equals(s)) supplement = count;
            else if ("弱链".equals(s)) weak = count;
            else if ("缺链".equals(s)) missing = count;
        }

        DiagnosisSummaryResponse vo = new DiagnosisSummaryResponse();
        vo.setStrongCount(strong);
        vo.setSupplementCount(supplement);
        vo.setWeakCount(weak);
        vo.setMissingCount(missing);
        vo.setTotalCount(strong + supplement + weak + missing);
        vo.setCategories(Arrays.asList(
            new DiagnosisSummaryResponse.CategoryDetail("强链", "强链", strong, "高价值≥200且企业≥20家", "#52c41a"),
            new DiagnosisSummaryResponse.CategoryDetail("补链", "补链", supplement, "高价值≥50或企业≥5家", "#faad14"),
            new DiagnosisSummaryResponse.CategoryDetail("弱链", "弱链", weak, "高价值<50且企业<5家", "#ff4d4f"),
            new DiagnosisSummaryResponse.CategoryDetail("缺链", "缺链", missing, "无覆盖企业", "#bfbfbf")
        ));
        return vo;
    }

    // ─── IDiagnosisService (keep for backward compatibility) ───
    @Override
    public Object diagnose(DiagnosisRequest request) {
        log.info("deprecated diagnose() called, use D01-D04 APIs instead");
        return new java.util.HashMap<String, Object>();
    }

    @Override
    public DiagnosisResponse getDiagnosisResult(Long chainId, String year) {
        log.info("deprecated getDiagnosisResult() called, use D01-D04 APIs instead");
        return new DiagnosisResponse();
    }

    // ─── 异步执行重新诊断 ───
    @Override
    public void executeRecalcAsync(String taskId, String regionCode) {
        Long regionId = regionCode != null ? parseLong(regionCode) : null;
        try {
            updateTaskStatus(taskId, "running", 0, "正在执行诊断...");
            List<ChainNode> nodes = chainNodeMapper.selectList(null);
            if (nodes == null || nodes.isEmpty()) {
                updateTaskStatus(taskId, "failed", 0, "诊断失败: 节点列表为空");
                return;
            }
            int total = nodes.size();
            int failed = 0;
            for (int i = 0; i < total; i++) {
                ChainNode node = nodes.get(i);
                try {
                    int patentCount = patentMapper.selectCountByNodeId(node.getId());
                    int highValueCount = patentMapper.selectHighValueCountByNodeId(node.getId());
                    int enterpriseCount = patentMapper.selectEnterpriseCountByNodeId(node.getId());
                    diagnoseAndSave(node.getId(), regionId, patentCount, highValueCount, enterpriseCount);
                } catch (Exception e) {
                    failed++;
                    log.error("Recalc: node#{} ({}) failed", node.getId(), node.getNodeName(), e);
                }
                int progress = (int) ((i + 1) * 100.0 / total);
                updateTaskStatus(taskId, "running", progress,
                        "诊断中: " + (i + 1) + "/" + total + " (" + node.getNodeName() + ")");
            }
            String resultMsg = "诊断完成";
            if (failed > 0) resultMsg += " (" + failed + " 个节点失败)";
            updateTaskStatus(taskId, "completed", 100, resultMsg);
        } catch (Exception e) {
            log.error("Recalc task {} failed", taskId, e);
            updateTaskStatus(taskId, "failed", 0, "诊断失败: " + e.getMessage());
        }
    }

    private void diagnoseAndSave(Long nodeId, Long regionId, int patentCount, int highValueCount, int enterpriseCount) {
        String tag, basis;
        if (enterpriseCount == 0) {
            tag = "缺链"; basis = "该节点无覆盖企业(企业数=0)，处于完全缺失状态";
        } else if (enterpriseCount > 50 && highValueCount > 500) {
            tag = "强链"; basis = String.format("企业规模(%d家)和高价值专利(%d件)均远超阈值，强制判定为强链", enterpriseCount, highValueCount);
        } else if (highValueCount >= 200 && enterpriseCount >= 20) {
            tag = "强链"; basis = String.format("高价值专利%d件(≥200)且覆盖企业%d家(≥20)", highValueCount, enterpriseCount);
        } else if (highValueCount >= 50 || enterpriseCount >= 5 || patentCount >= 100) {
            tag = "补链";
            String reason = highValueCount >= 50 ? String.format("高价值专利%d件(≥50)", highValueCount)
                    : enterpriseCount >= 5 ? String.format("企业数%d家(≥5)", enterpriseCount)
                    : String.format("专利总数%d件(≥100)", patentCount);
            basis = reason + "，具备一定基础但仍有提升空间";
        } else {
            tag = "弱链"; basis = String.format("高价值专利%d件(<50)且企业数%d家(<5)，需要重点补强", highValueCount, enterpriseCount);
        }
        NodeDiagnosis diagnosis = NodeDiagnosis.builder()
                .regionId(regionId).nodeId(nodeId).diagnosisTag(tag)
                .highValuePatentCount(highValueCount).totalPatentCount(patentCount)
                .enterpriseCount(enterpriseCount).diagnosisBasis(basis)
                .diagnosisDate(LocalDate.now()).build();
        nodeDiagnosisMapper.insert(diagnosis);
    }

    private void updateTaskStatus(String taskId, String status, int progress, String message) {
        RecalcStatusResponse s = RecalcStatusResponse.builder()
                .taskId(taskId).status(status).progress(progress).message(message).build();
        redisTemplate.opsForValue().set(TASK_KEY_PREFIX + taskId, s, Duration.ofHours(1));
    }

    private Long parseLong(String s) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
    }

    // ─── helpers ───

    private String mapLayerLabel(String layer) {
        if (layer == null) return "未知";
        switch (layer) { case "up": return "上游"; case "mid": return "中游"; case "down": return "下游"; default: return "未知"; }
    }

    private String layerToString(String layer) {
        if (layer == null) return "";
        switch (layer) { case "up": return "upstream"; case "mid": return "midstream"; case "down": return "downstream"; default: return ""; }
    }

    private String mapColor(String tag) {
        if ("强链".equals(tag)) return "#52c41a";
        if ("补链".equals(tag)) return "#faad14";
        if ("弱链".equals(tag)) return "#ff4d4f";
        if ("缺链".equals(tag)) return "#bfbfbf";
        return "#999";
    }

    private List<String> generateSuggestions(String tag) {
        List<String> s = new ArrayList<>();
        if ("缺链".equals(tag)) { s.add("该节点无覆盖企业，建议优先招商引资"); s.add("可考虑引入龙头企业带动产业集聚"); }
        else if ("弱链".equals(tag)) { s.add("建议加大对该环节的招商引资力度"); s.add("关注相关领域的专利技术引进和人才引进"); }
        else if ("补链".equals(tag)) { s.add("建议引导现有企业加大研发投入"); s.add("鼓励产学研合作提升技术创新能力"); }
        else { s.add("保持现有优势，持续关注前沿技术发展"); s.add("鼓励企业参与国际标准制定"); }
        return s;
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long) return (Long) val;
        if (val instanceof Integer) return ((Integer) val).longValue();
        return Long.valueOf(val.toString());
    }

    private Integer toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Long) return ((Long) val).intValue();
        return Integer.valueOf(val.toString());
    }
}
