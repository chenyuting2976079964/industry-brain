package com.industrybrain.service.impl;

import com.industrybrain.domain.model.IndustrialPark;
import com.industrybrain.dto.response.*;
import com.industrybrain.mapper.mysql.PatentMapper;
import com.industrybrain.mapper.mysql.*;
import com.industrybrain.service.IDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 全景大屏业务实现 (v2.0)
 * 聚合 MySQL/ES/Neo4j → Redis缓存 → 返回
 * 所有数据均从数据库查询，不使用硬编码数据。
 *
 * @author 何锦鹏
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    private final PatentMapper patentMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final NodeDiagnosisMapper nodeDiagnosisMapper;
    private final IndustrialParkMapper parkMapper;
    private final ChainNodeMapper chainNodeMapper;

    /** F01: KPI */
    @Override
    @Cacheable(value = "dashboard:kpi", unless = "#result == null")
    public DashboardKpiResponse getKpi(String regionCode) {
        DashboardKpiResponse vo = new DashboardKpiResponse();
        vo.setTotalPatents(patentMapper.selectTotalCount());
        vo.setHighValuePatents(patentMapper.selectHighValueCount());
        vo.setCoveredEnterprises(patentMapper.selectDistinctEnterpriseCount());
        vo.setChainNodeCount(nodeDiagnosisMapper.selectNodeCount());
        vo.setDataUpdateTime(java.time.LocalDateTime.now().toString());
        return vo;
    }

    /** F02: 企业分布5格 */
    @Override
    public List<EnterpriseDistResponse> getEnterpriseDistribution(String regionCode) {
        long total = enterpriseMapper.selectCount(null);
        long aboveScale = enterpriseMapper.selectCountByRegion(2L);
        long localCount = enterpriseMapper.selectCountByLocal(1);
        long nonLocalCount = enterpriseMapper.selectCountByLocal(0);

        // 从 enterprise_evaluation 表或通过企业类型字段统计专精特新
        long specialized = 0;
        try {
            // 尝试从企业表按 enterprise_type 字段统计专精特新
            List<Map<String, Object>> localEnts = enterpriseMapper.selectLocalEnterprises(null);
            if (localEnts != null) {
                specialized = localEnts.stream()
                    .filter(m -> "专精特新".equals(m.get("enterprise_type")))
                    .count();
            }
        } catch (Exception e) {
            log.debug("查询专精特新企业数量失败: {}", e.getMessage());
        }

        return Arrays.asList(
            new EnterpriseDistResponse("total", "企业总数", total, "el-icon-office-building"),
            new EnterpriseDistResponse("aboveScale", "规上企业", aboveScale, "el-icon-data-line"),
            new EnterpriseDistResponse("highTech", "本地企业", localCount, "el-icon-star"),
            new EnterpriseDistResponse("techSME", "外地企业", nonLocalCount, "el-icon-aim"),
            new EnterpriseDistResponse("specialized", "专精特新", specialized, "el-icon-trophy")
        );
    }

    /** F03: 技术领域分布 */
    @Override
    public List<TechDistResponse> getTechDistribution(String regionCode) {
        List<Map<String, Object>> rows = patentMapper.selectTechDistribution();
        if (rows == null) return Collections.emptyList();
        // IPC 翻译为中文名，并按中文名合并计数
        Map<String, Long> merged = new java.util.LinkedHashMap<>();
        for (Map<String, Object> m : rows) {
            String ipcCode = (String) m.get("techField");
            String name = IpcNameMapper.toName(ipcCode);
            long cnt = ((Number) m.get("enterpriseCount")).longValue();
            merged.merge(name, cnt, Long::sum);
        }
        long total = merged.values().stream().mapToLong(Long::longValue).sum();
        return merged.entrySet().stream().map(e -> {
            TechDistResponse vo = new TechDistResponse();
            vo.setFieldName(e.getKey());
            vo.setEnterpriseCount(e.getValue());
            vo.setPercentage(e.getValue() * 100.0 / Math.max(total, 1));
            return vo;
        }).sorted((a, b) -> Long.compare(b.getEnterpriseCount(), a.getEnterpriseCount()))
          .collect(Collectors.toList());
    }

    /** F04: 高价值专利TOP5 */
    @Override
    public List<PatentTop5Response> getPatentTop5(String regionCode) {
        List<Map<String, Object>> top5 = patentMapper.selectTop5HighValue();
        if (top5 == null) return Collections.emptyList();
        int rank = 1;
        List<PatentTop5Response> result = new ArrayList<>();
        for (Map<String, Object> m : top5) {
            PatentTop5Response vo = new PatentTop5Response();
            vo.setRank(rank++);
            String pName = (String) m.get("patent_name");
            vo.setPatentName(pName != null ? pName : ((String) m.get("applicant")));
            vo.setPatentId(((Number) m.get("id")).longValue());
            vo.setApplicant((String) m.get("applicant"));
            Object years = m.get("maintenance_years");
            vo.setScore(years != null ? ((Number) years).intValue() * 10 : 0);
            result.add(vo);
        }
        return result;
    }

    /** F05: 产业链强弱分布 */
    @Override
    public List<ChainStrengthResponse> getChainStrengthDistribution(String regionCode) {
        List<Map<String, Object>> rows = nodeDiagnosisMapper.selectStatusSummary();
        if (rows == null) return Collections.emptyList();
        long total = rows.stream().mapToLong(m -> {
            Object v = m.get("node_count");
            if (v == null) v = m.get("cnt");
            return v == null ? 0 : ((Number) v).longValue();
        }).sum();

        return rows.stream().map(m -> {
            ChainStrengthResponse vo = new ChainStrengthResponse();
            Object cntVal = m.get("node_count");
            if (cntVal == null) cntVal = m.get("cnt");
            String status = (String) (m.get("diagnosis_tag") != null ? m.get("diagnosis_tag") : m.get("diagnosisTag"));
            vo.setStatus(status);
            vo.setCount(cntVal == null ? 0 : ((Number) cntVal).longValue());
            vo.setPercentage(vo.getCount() * 100.0 / Math.max(total, 1));
            vo.setLabel(status != null ? status : "未知");
            // 颜色从诊断标签推导（通用规则）
            vo.setColor(diagnosisTagColor(status));
            return vo;
        }).collect(Collectors.toList());
    }

    /** F06: 地图标注点 — 从园区表动态生成 */
    @Override
    public List<MapMarkerResponse> getMapMarkers(String regionCode) {
        List<IndustrialPark> parks = parkMapper.selectList(null);
        if (parks == null || parks.isEmpty()) return Collections.emptyList();

        List<MapMarkerResponse> markers = new ArrayList<>();

        // 动态布局参数（按数量均匀分布）
        int count = parks.size();
        for (int i = 0; i < count; i++) {
            IndustrialPark park = parks.get(i);

            // 根据园区类型选择标记样式
            String parkType = park.getParkType();
            String markerType;
            String markerSymbol;
            if ("核心园区".equals(parkType)) {
                markerType = "star";
                markerSymbol = "★";
            } else if ("产业聚集区".equals(parkType)) {
                markerType = "circle";
                markerSymbol = "●";
            } else if ("孵化园区".equals(parkType)) {
                markerType = "diamondOpen";
                markerSymbol = "◇";
            } else {
                markerType = "diamond";
                markerSymbol = "◆";
            }

            // 动态布局：在可视区域内均匀分布
            double leftPct = 15.0 + (i * 75.0 / Math.max(count, 1));
            double topPct = 30.0 + (i % 3) * 18.0;

            String description = park.getDominantIndustries() != null
                ? park.getParkType() + " · " + park.getDominantIndustries()
                : (park.getParkType() != null ? park.getParkType() : "园区");

            markers.add(MapMarkerResponse.builder()
                .type(markerType)
                .typeSymbol(markerSymbol)
                .name(park.getName())
                .leftPercent(leftPct)
                .topPercent(topPct)
                .color(parkTypeColorHex(parkType))
                .description(description)
                .build());
        }

        return markers;
    }

    /** F07: 本地企业总体分布（按类型统计） */
    @Override
    public Map<String, Object> getLocalEnterpriseStats(String regionCode) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        long totalLocal = enterpriseMapper.selectCountByLocal(1);
        result.put("totalLocal", totalLocal);
        // 按 employee_scale 近似分类
        List<Map<String, Object>> scaleStats = enterpriseMapper.selectLocalByScale();
        long large = 0, mediumLarge = 0, mediumSmall = 0, small = 0;
        if (scaleStats != null) {
            for (Map<String, Object> row : scaleStats) {
                String scale = (String) row.get("employee_scale");
                long cnt = ((Number) row.get("cnt")).longValue();
                if (scale == null) { small += cnt; continue; }
                if (scale.contains("大型") && !scale.contains("中") && !scale.contains("小")) large += cnt;
                else if (scale.contains("中大型") || scale.contains("中大")) mediumLarge += cnt;
                else if (scale.contains("中小")) mediumSmall += cnt;
                else if (scale.contains("中型")) mediumLarge += cnt;
                else if (scale.contains("小型")) small += cnt;
                else small += cnt;
            }
        }
        result.put("leadingEnterprise", large);
        result.put("highTechEnterprise", mediumLarge);
        result.put("techSME", mediumSmall);
        result.put("regularEnterprise", small);
        return result;
    }

    // ─── Helpers ───
    private String diagnosisTagColor(String tag) {
        if (tag == null) return "#bfbfbf";
        switch (tag) {
            case "强链": return "#52c41a";
            case "补链": return "#faad14";
            case "弱链": return "#ff4d4f";
            case "缺链": return "#bfbfbf";
            default: return "#999";
        }
    }

    private String parkTypeColorHex(String type) {
        if (type == null) return "#1890ff";
        switch (type) {
            case "核心园区": return "#1890ff";
            case "产业聚集区": return "#722ed1";
            case "特色园区": return "#52c41a";
            case "孵化园区": return "#faad14";
            default: return "#1890ff";
        }
    }
}
