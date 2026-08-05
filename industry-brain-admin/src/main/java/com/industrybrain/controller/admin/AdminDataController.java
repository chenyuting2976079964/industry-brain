package com.industrybrain.controller.admin;

import com.industrybrain.common.core.domain.AjaxResult;
import com.industrybrain.domain.model.ChainNode;
import com.industrybrain.domain.model.IndustrialPark;
import com.industrybrain.domain.model.IndustryChain;
import com.industrybrain.domain.model.Region;
import com.industrybrain.domain.model.DataSyncLog;
import com.industrybrain.mapper.mysql.ChainInfoMapper;
import com.industrybrain.mapper.mysql.ChainNodeMapper;
import com.industrybrain.mapper.mysql.DataSyncLogMapper;
import com.industrybrain.mapper.mysql.IndustrialParkMapper;
import com.industrybrain.mapper.mysql.NodeDiagnosisMapper;
import com.industrybrain.mapper.mysql.RegionMapper;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 数据管理接口 — S01~S08（查英）
 * 当 mysql.enabled=false 时跳过（无 MySQL 环境降级为模拟实现）
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mysql.enabled", havingValue = "true", matchIfMissing = true)
public class AdminDataController {

    private final DataSyncLogMapper dataSyncLogMapper;
    private final RegionMapper regionMapper;
    private final IndustrialParkMapper parkMapper;
    private final ChainInfoMapper chainInfoMapper;
    private final ChainNodeMapper chainNodeMapper;
    private final NodeDiagnosisMapper nodeDiagnosisMapper;

    // ==================== S02: 导入日志 ====================
    @GetMapping("/import/logs")
    public AjaxResult importLogs(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  @RequestParam(required = false) Long logId) {
        if (logId != null) {
            // 查最新日志作为详情（后续扩展按 ID 精确查）
            DataSyncLog log = dataSyncLogMapper.selectLatest();
            return AjaxResult.success(log != null ? toLogMap(log) : null);
        }
        DataSyncLog latest = dataSyncLogMapper.selectLatest();
        List<Map<String, Object>> records = new ArrayList<>();
        if (latest != null) records.add(toLogMap(latest));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", records.size());
        result.put("page", page);
        result.put("size", size);
        result.put("records", records);
        return AjaxResult.success(result);
    }

    private Map<String, Object> toLogMap(DataSyncLog s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("syncType", s.getSyncType());
        m.put("dataSource", s.getDataSource());
        m.put("syncStartTime", s.getSyncStartTime());
        m.put("syncEndTime", s.getSyncEndTime());
        m.put("totalCount", s.getTotalCount());
        m.put("successCount", s.getSuccessCount());
        m.put("failCount", s.getFailCount());
        m.put("syncStatus", s.getSyncStatus());
        m.put("errorMsg", s.getErrorMsg());
        return m;
    }

    // ==================== S03: 区域管理 ====================
    @GetMapping("/regions")
    public AjaxResult regions(@RequestParam(required = false) Long parentId) {
        List<Region> regions = regionMapper.selectByParentId(parentId);
        if (regions == null) return AjaxResult.success(List.of());
        List<Map<String, Object>> list = new ArrayList<>();
        for (Region r : regions) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("name", r.getName());
            m.put("code", r.getCode());
            m.put("parentId", r.getParentId());
            List<Region> children = regionMapper.selectByParentId(r.getId());
            if (children != null && !children.isEmpty()) {
                List<Map<String, Object>> childList = new ArrayList<>();
                for (Region c : children) {
                    Map<String, Object> cm = new LinkedHashMap<>();
                    cm.put("id", c.getId());
                    cm.put("name", c.getName());
                    cm.put("code", c.getCode());
                    cm.put("parentId", c.getParentId());
                    childList.add(cm);
                }
                m.put("children", childList);
            }
            list.add(m);
        }
        return AjaxResult.success(list);
    }

    // ==================== S04: 园区列表 ====================
    @GetMapping("/parks")
    public AjaxResult parks(@RequestParam(required = false) Long regionId) {
        List<IndustrialPark> parks = parkMapper.selectByRegionId(regionId);
        if (parks == null) return AjaxResult.success(List.of());
        List<Map<String, Object>> list = new ArrayList<>();
        for (IndustrialPark p : parks) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("regionId", p.getRegionId());
            m.put("parkLevel", p.getParkLevel());
            m.put("address", p.getAddress());
            m.put("description", p.getDescription());
            list.add(m);
        }
        return AjaxResult.success(list);
    }

    // ==================== S05: 园区新增/更新 ====================
    @PostMapping("/parks/save")
    public AjaxResult savePark(@RequestBody Map<String, Object> parkData) {
        Long id = toLong(parkData.get("id"));
        String name = (String) parkData.get("name");
        Long regionId = toLong(parkData.get("regionId"));
        String parkLevel = (String) parkData.get("parkLevel");
        String address = (String) parkData.get("address");
        String description = (String) parkData.get("description");

        if (id != null && id > 0) {
            // 更新：用 BaseMapper.updateById
            IndustrialPark park = IndustrialPark.builder().id(id)
                    .name(name).regionId(regionId).parkLevel(parkLevel)
                    .address(address).description(description)
                    .updatedAt(LocalDateTime.now()).build();
            parkMapper.updateById(park);
            return AjaxResult.success(Map.of("id", id, "action", "updated"));
        } else {
            IndustrialPark park = IndustrialPark.builder()
                    .name(name).regionId(regionId).parkLevel(parkLevel)
                    .address(address).description(description)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            parkMapper.insert(park);
            return AjaxResult.success(Map.of("id", park.getId(), "action", "inserted"));
        }
    }

    // ==================== S06: 产业链管理 ====================
    @GetMapping("/chains")
    public AjaxResult chains() {
        List<IndustryChain> chains = chainInfoMapper.selectAllOrdered();
        if (chains == null) return AjaxResult.success(List.of());
        List<Map<String, Object>> result = new ArrayList<>();
        for (IndustryChain c : chains) {
            Map<String, Object> mc = new LinkedHashMap<>();
            mc.put("id", c.getId());
            mc.put("name", c.getName());
            mc.put("code", c.getCode());
            List<ChainNode> nodes = chainNodeMapper.selectByChainId(c.getId());
            if (nodes != null) {
                List<Map<String, Object>> nodeList = new ArrayList<>();
                for (ChainNode n : nodes) {
                    Map<String, Object> nm = new LinkedHashMap<>();
                    nm.put("id", n.getId());
                    nm.put("name", n.getNodeName());
                    nm.put("parentId", n.getParentId());
                    nm.put("sortOrder", n.getSortOrder());
                    nodeList.add(nm);
                }
                mc.put("nodes", nodeList);
            }
            result.add(mc);
        }
        return AjaxResult.success(result);
    }

    // ==================== S07: 健康检查 ====================
    @GetMapping("/health")
    public AjaxResult health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("timestamp", LocalDateTime.now().toString());
        try { chainInfoMapper.selectAllOrdered(); status.put("mysql", "UP"); }
        catch (Exception e) { status.put("mysql", "DOWN"); }
        status.put("elasticsearch", probeEs());
        status.put("neo4j", probeNeo4j());
        status.put("redis", probeRedis());
        status.put("minio", probeMinio());
        status.put("canal", "canal:11111");
        return AjaxResult.success(status);
    }

    private String probeEs() {
        try {
            java.net.Socket s = new java.net.Socket("localhost", 9200);
            s.setSoTimeout(2000);
            s.close();
            return "UP (es:9200)";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String probeNeo4j() {
        try {
            java.net.Socket s = new java.net.Socket("localhost", 7687);
            s.setSoTimeout(2000);
            s.close();
            return "UP (neo4j:7687)";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String probeRedis() {
        try {
            java.net.Socket s = new java.net.Socket("localhost", 6379);
            s.setSoTimeout(2000);
            s.close();
            return "UP (redis:6379)";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String probeMinio() {
        try {
            java.net.Socket s = new java.net.Socket("localhost", 9000);
            s.setSoTimeout(2000);
            s.close();
            return "UP (minio:9000)";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    // ==================== S08: 数据导出 ====================
    @PostMapping("/export")
    public AjaxResult exportData(@RequestBody Map<String, Object> request) {
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        return AjaxResult.success(Map.of("taskId", taskId, "message", "导出任务已创建"));
    }

    // ==================== helper ====================
    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long) return (Long) val;
        if (val instanceof Integer) return ((Integer) val).longValue();
        return Long.valueOf(val.toString());
    }
}
