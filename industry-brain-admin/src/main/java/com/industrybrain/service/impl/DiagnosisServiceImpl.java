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
    private final EnterpriseNodeMapper enterpriseNodeMapper;
    private final EnterpriseTagMapper enterpriseTagMapper;
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
            new DiagnosisSummaryResponse.CategoryDetail("强链", "强链", strong, "本地企业全国占比≥15%", "#52c41a"),
            new DiagnosisSummaryResponse.CategoryDetail("补链", "补链", supplement, "本地企业全国占比5%~15%", "#faad14"),
            new DiagnosisSummaryResponse.CategoryDetail("弱链", "弱链", weak, "本地企业全国占比1%~5%", "#ff4d4f"),
            new DiagnosisSummaryResponse.CategoryDetail("缺链", "缺链", missing, "本地企业全国占比<1%", "#bfbfbf")
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
                    // 本地企业占全国比例：按节点覆盖标签（firms）取全国/本地企业并集
                    List<String> covered = parseFirms(node.getFirms(), node.getNodeName());
                    Set<Long> nationalIds = new LinkedHashSet<>();
                    Set<Long> localIds = new LinkedHashSet<>();
                    for (String tag : covered) {
                        List<Long> nat = enterpriseTagMapper.selectEnterpriseIdsByTagName(tag);
                        if (nat != null) nationalIds.addAll(nat);
                        List<Long> loc = enterpriseTagMapper.selectLocalEnterpriseIdsByTagName(tag);
                        if (loc != null) localIds.addAll(loc);
                    }
                    int nationalCount = nationalIds.size();
                    int localCount = localIds.size();
                    double share = nationalCount > 0 ? (double) localCount / nationalCount : 0;
                    diagnoseAndSave(node.getId(), regionId, patentCount, highValueCount,
                            localCount, nationalCount, share);
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

    /**
     * 强弱链诊断判定（本地企业全国占比，只考虑企业）：
     *   缺链：本地企业全国占比 < 1%
     *   弱链：1% ≤ 占比 < 5%
     *   补链：5% ≤ 占比 < 15%
     *   强链：占比 ≥ 15%
     * 判断优先级：缺 → 弱 → 补 → 强，兜底缺链。
     *
     * @param localCount    本地企业数
     * @param nationalCount 全国企业数
     * @param share         本地企业全国占比 (0~1)
     */
    private void diagnoseAndSave(Long nodeId, Long regionId, int patentCount, int highValueCount,
                                 int localCount, int nationalCount, double share) {
        String tag;
        if (share < 0.01) tag = "缺链";
        else if (share < 0.05) tag = "弱链";
        else if (share < 0.15) tag = "补链";
        else tag = "强链";

        String pct = String.format("%.1f%%", share * 100);
        String basis = String.format("本地企业%d家/全国%d家(占比%s)", localCount, nationalCount, pct);
        switch (tag) {
            case "强链": basis += "，本地企业全国占比≥15%，该环节在全国具有比较优势"; break;
            case "补链": basis += "，本地企业全国占比5%~15%，具备一定基础"; break;
            case "弱链": basis += "，本地企业全国占比1%~5%，环节薄弱需重点招商"; break;
            default: basis += "，本地企业全国占比<1%，环节空白最优先级招商"; break;
        }

        NodeDiagnosis diagnosis = NodeDiagnosis.builder()
                .regionId(regionId).nodeId(nodeId).diagnosisTag(tag)
                .highValuePatentCount(highValueCount).totalPatentCount(patentCount)
                .enterpriseCount(localCount)
                .nationalEnterpriseCount(nationalCount)
                .localEnterpriseCount(localCount)
                .localEnterpriseShare(nationalCount > 0
                        ? java.math.BigDecimal.valueOf(share).setScale(4, java.math.RoundingMode.HALF_UP)
                        : null)
                .diagnosisBasis(basis)
                .diagnosisDate(LocalDate.now()).build();
        // 物理删除该节点当日的旧诊断（uk_region_node_date 唯一键约束，避免重复插入冲突；
        // 不能用 MyBatis-Plus delete()——它走逻辑删除，物理行仍在，唯一键仍冲突）
        nodeDiagnosisMapper.physicallyDelete(regionId, nodeId, diagnosis.getDiagnosisDate());
        nodeDiagnosisMapper.insert(diagnosis);
    }

    /** 从 firms 字段（逗号分隔）解析覆盖标签；firms 为空时回退 nodeName */
    private List<String> parseFirms(String firms, String nodeName) {
        if (firms != null && !firms.isBlank()) {
            return Arrays.stream(firms.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return nodeName != null ? List.of(nodeName) : Collections.emptyList();
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
