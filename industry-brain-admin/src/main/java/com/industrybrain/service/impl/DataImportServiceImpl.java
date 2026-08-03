package com.industrybrain.service.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.industrybrain.domain.model.Enterprise;
import com.industrybrain.domain.model.Patent;
import com.industrybrain.domain.model.Region;
import com.industrybrain.dto.request.DataImportRequest;
import com.industrybrain.mapper.mysql.EnterpriseMapper;
import com.industrybrain.mapper.mysql.PatentMapper;
import com.industrybrain.mapper.mysql.RegionMapper;
import com.industrybrain.service.IDataImportService;
import com.industrybrain.utils.ExcelImportUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据导入业务实现类
 * 负责从Excel文件导入企业、专利等数据
 */
@Service
public class DataImportServiceImpl implements IDataImportService {

    private static final Logger log = LoggerFactory.getLogger(DataImportServiceImpl.class);

    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private EnterpriseMapper enterpriseMapper;

    @Autowired
    private PatentMapper patentMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String DATA_DIR = "产业大脑数据/产业大脑数据";
    private static final String TASK_KEY_PREFIX = "import:task:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importData(DataImportRequest request) {
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        updateTaskStatus(taskId, "running", 0, "导入任务已启动");

        try {
            log.info("开始数据导入任务, type={}, region={}, file={}",
                    request.getImportType(), request.getRegionCode(), request.getFilePath());

            String filePath = request.getFilePath();
            if (StringUtils.isEmpty(filePath)) {
                filePath = DATA_DIR;
            }

            File dataDir = new File(filePath);
            if (!dataDir.exists() || !dataDir.isDirectory()) {
                log.error("数据目录不存在: {}", filePath);
                updateTaskStatus(taskId, "failed", 0, "数据目录不存在: " + filePath);
                throw new IllegalArgumentException("数据目录不存在: " + filePath);
            }

            switch (request.getImportType()) {
                case "ALL":
                    importAllData(dataDir);
                    break;
                case "REGION":
                    importRegionData(dataDir);
                    break;
                case "ENTERPRISE":
                    importEnterpriseData(dataDir);
                    break;
                case "PATENT":
                    importPatentData(dataDir);
                    break;
                default:
                    throw new IllegalArgumentException("未知的导入类型: " + request.getImportType());
            }

            updateTaskStatus(taskId, "completed", 100, "导入完成");
            log.info("数据导入任务完成, taskId={}", taskId);
        } catch (Exception e) {
            log.error("数据导入失败, taskId={}, error={}", taskId, e.getMessage(), e);
            updateTaskStatus(taskId, "failed", 0, "导入失败: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public String getImportStatus(String taskId) {
        Object cached = redisTemplate.opsForValue().get(TASK_KEY_PREFIX + taskId);
        if (cached == null) {
            return "UNKNOWN";
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> status = (Map<String, Object>) cached;
        return (String) status.getOrDefault("status", "UNKNOWN");
    }

    // ─── 导入流程控制 ───

    private void importAllData(File dataDir) {
        log.info("开始导入所有数据...");
        importRegionData(dataDir);
        importEnterpriseData(dataDir);
        importPatentData(dataDir);
        log.info("所有数据导入完成");
    }

    // ─── 区域导入 ───

    private void importRegionData(File dataDir) {
        log.info("开始导入区域数据...");

        File regionFile = new File(dataDir, "01_区域对照表.xlsx");
        if (!regionFile.exists()) {
            log.warn("区域文件不存在，跳过区域导入: {}", regionFile.getAbsolutePath());
            return;
        }

        List<Map<String, Object>> rows = ExcelImportUtils.readExcelData(regionFile);
        if (rows == null || rows.isEmpty()) {
            log.warn("区域数据为空");
            return;
        }

        AtomicInteger count = new AtomicInteger(0);
        for (int i = 1; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            try {
                String name = (String) row.get("区域名称");
                if (StringUtils.isEmpty(name)) continue;

                Region existing = regionMapper.selectByName(name);
                if (existing != null) {
                    log.debug("区域已存在，跳过: {}", name);
                    continue;
                }

                Region region = new Region();
                region.setName(name);
                region.setCode(generateRegionCode(name));
                region.setCreatedAt(LocalDateTime.now());
                region.setUpdatedAt(LocalDateTime.now());
                region.setIsDeleted(0);
                regionMapper.insert(region);
                count.incrementAndGet();
                log.debug("导入区域: {}", name);
            } catch (Exception e) {
                log.error("导入区域失败, row={}, error={}", i, e.getMessage());
            }
        }
        log.info("区域数据导入完成: {} 条", count.get());
    }

    // ─── 企业导入 ───

    private void importEnterpriseData(File dataDir) {
        log.info("开始导入企业数据...");

        File[] excelFiles = dataDir.listFiles((dir, name) ->
                name.startsWith("02_企业清洗结果") && name.endsWith(".xlsx"));
        if (excelFiles == null || excelFiles.length == 0) {
            log.warn("企业数据文件不存在，跳过企业导入");
            return;
        }

        AtomicInteger totalCount = new AtomicInteger();
        AtomicInteger successCount = new AtomicInteger();

        for (File file : excelFiles) {
            log.info("处理企业文件: {}", file.getName());
            List<Map<String, Object>> rows = ExcelImportUtils.readExcelData(file);
            if (rows == null || rows.isEmpty()) continue;

            for (int i = 1; i < rows.size(); i++) {
                Map<String, Object> row = rows.get(i);
                try {
                    Enterprise enterprise = mapEnterpriseRow(row);
                    if (enterprise != null && StringUtils.isNotBlank(enterprise.getUnifiedCode())) {
                        Enterprise existing = enterpriseMapper.selectByUnifiedCode(enterprise.getUnifiedCode());
                        if (existing == null) {
                            enterprise.setCreatedAt(LocalDateTime.now());
                            enterprise.setUpdatedAt(LocalDateTime.now());
                            enterprise.setIsDeleted(0);
                            enterpriseMapper.insert(enterprise);
                            successCount.incrementAndGet();
                            log.debug("导入企业: {}", enterprise.getName());
                        } else {
                            log.debug("企业已存在，跳过: {}", enterprise.getName());
                        }
                    }
                } catch (Exception e) {
                    log.error("解析企业数据失败, row={}, error={}", i, e.getMessage());
                }
                totalCount.incrementAndGet();
            }
        }
        log.info("企业数据导入完成: 总计{}条, 成功{}条", totalCount.get(), successCount.get());
    }

    // ─── 专利导入 ───

    private void importPatentData(File dataDir) {
        log.info("开始导入专利数据...");

        File patentFile = new File(dataDir, "专利数据_合并.xlsx");
        if (!patentFile.exists()) {
            log.warn("专利文件不存在，跳过专利导入: {}", patentFile.getAbsolutePath());
            return;
        }

        List<Map<String, Object>> rows = ExcelImportUtils.readExcelData(patentFile);
        if (rows == null || rows.isEmpty()) {
            log.warn("专利数据为空");
            return;
        }

        AtomicInteger count = new AtomicInteger(0);
        for (int i = 1; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            try {
                Patent patent = mapPatentRow(row);
                if (patent != null && StringUtils.isNotBlank(patent.getPatentNumber())) {
                    Patent existing = patentMapper.selectByPatentNumber(patent.getPatentNumber());
                    if (existing == null) {
                        patent.setCreatedAt(LocalDateTime.now());
                        patent.setUpdatedAt(LocalDateTime.now());
                        patent.setIsDeleted(0);
                        patentMapper.insert(patent);
                        count.incrementAndGet();
                        log.debug("导入专利: {}", patent.getPatentName());
                    }
                }
            } catch (Exception e) {
                log.error("导入专利失败, row={}, error={}", i, e.getMessage());
            }
        }
        log.info("专利数据导入完成: {} 条", count.get());
    }

    // ─── 行映射 ───

    private Enterprise mapEnterpriseRow(Map<String, Object> row) {
        Enterprise enterprise = new Enterprise();
        enterprise.setName((String) row.get("企业名称"));
        enterprise.setUnifiedCode((String) row.get("统一社会信用代码"));
        enterprise.setLegalRepresentative((String) row.get("法定代表人"));
        enterprise.setRegisteredAddress((String) row.get("注册地址"));
        enterprise.setContactPhone((String) row.get("联系电话"));

        String capital = (String) row.get("注册资本");
        if (StringUtils.isNotBlank(capital)) {
            enterprise.setRegisteredCapital(capital);
        }

        String establishDate = (String) row.get("成立日期");
        if (StringUtils.isNotBlank(establishDate)) {
            try {
                enterprise.setEstablishmentDate(LocalDate.parse(establishDate));
            } catch (Exception e) {
                log.warn("日期解析失败: {}", establishDate);
            }
        }

        enterprise.setBusinessStatus((String) row.get("经营状态"));
        enterprise.setIsLocal(0);
        return enterprise;
    }

    private Patent mapPatentRow(Map<String, Object> row) {
        Patent patent = new Patent();

        String patentNumber = (String) row.get("专利号");
        if (StringUtils.isEmpty(patentNumber)) {
            patentNumber = (String) row.get("申请号");
        }
        patent.setPatentNumber(patentNumber);

        String patentName = (String) row.get("专利名称");
        if (StringUtils.isEmpty(patentName)) {
            patentName = (String) row.get("标题");
        }
        patent.setPatentName(patentName);

        patent.setApplicant((String) row.get("申请人"));

        String ipcMain = (String) row.get("IPC主分类");
        if (StringUtils.isEmpty(ipcMain)) {
            ipcMain = (String) row.get("主分类号");
        }
        patent.setIpcCode(ipcMain);

        patent.setPatentType((String) row.get("专利类型"));

        String appDate = (String) row.get("申请日");
        if (StringUtils.isNotBlank(appDate)) {
            try {
                patent.setApplicationDate(LocalDate.parse(appDate));
            } catch (Exception e) {
                log.warn("专利日期解析失败: {}", appDate);
            }
        }

        patent.setLegalStatus((String) row.get("法律状态"));
        return patent;
    }

    // ─── helpers ───

    private String generateRegionCode(String name) {
        Map<String, String> regionCodes = new LinkedHashMap<>();
        regionCodes.put("安徽省", "340000");
        regionCodes.put("合肥市", "340100");
        regionCodes.put("芜湖市", "340200");
        regionCodes.put("马鞍山市", "340500");
        regionCodes.put("六安市", "341500");
        return regionCodes.getOrDefault(name, "000000");
    }

    private void updateTaskStatus(String taskId, String status, int progress, String message) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("taskId", taskId);
        s.put("status", status);
        s.put("progress", progress);
        s.put("message", message);
        redisTemplate.opsForValue().set(TASK_KEY_PREFIX + taskId, s, Duration.ofHours(1));
    }
}
