package com.industrybrain.service.impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.industrybrain.domain.model.ReportRecord;
import com.industrybrain.dto.response.RecommendationResponse;
import com.industrybrain.dto.response.TechnicalProfileResponse;
import com.industrybrain.dto.response.TechnicalProfileResponse.AdvantageField;
import com.industrybrain.dto.response.TechnicalProfileResponse.TechnologyField;
import com.industrybrain.infra.MinioFileService;
import com.industrybrain.mapper.mysql.ReportMapper;
import com.industrybrain.service.IExportService;
import com.industrybrain.service.ITechnicalProfileService;
import com.industrybrain.utils.ExcelStyleUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 报告导出业务实现类
 * 负责技术画像报告、招商推荐报告等文件的 Excel 生成与 MinIO 存储
 */
@Service
@ConditionalOnProperty(name = "mysql.enabled", havingValue = "true", matchIfMissing = true)
public class ExportServiceImpl implements IExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportServiceImpl.class);

    @Resource
    private ITechnicalProfileService technicalProfileService;

    @Autowired(required = false)
    private MinioFileService minioFileService;

    @Resource
    private ReportMapper reportMapper;

    // ==================== Excel 样式常量 ====================
    private static final String EXCEL_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String DATE_FMT = "yyyyMMdd_HHmmss";

    @Override
    public String exportTechnicalProfile(Long enterpriseId) {
        log.info("导出技术画像报告, enterpriseId={}", enterpriseId);

        try {
            // 1. 获取技术画像数据
            TechnicalProfileResponse profile = technicalProfileService.generateProfile(enterpriseId);
            if (profile == null || profile.getEnterpriseName() == null) {
                log.warn("技术画像数据为空, enterpriseId={}", enterpriseId);
                return null;
            }

            // 2. 生成 Excel 文件
            byte[] excelData = buildProfileExcel(profile);

            // 3. 上传到 MinIO
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FMT));
            String fileName = "technical_profile_" + enterpriseId + "_" + timestamp + ".xlsx";
            if (minioFileService == null) {
                log.warn("MinIO 未启用, 返回本地临时路径");
                return "/tmp/" + fileName;
            }
            String url = minioFileService.uploadToCategory("profile", fileName, excelData, EXCEL_MIME);

            if (url == null) {
                log.warn("技术画像上传 MinIO 失败, 返回本地临时路径");
                return "/tmp/" + fileName;
            }

            log.info("技术画像报告导出成功: enterpriseId={}, url={}", enterpriseId, url);
            return url;

        } catch (Exception e) {
            log.error("导出技术画像报告失败: enterpriseId={}, error={}", enterpriseId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String exportRecommendationReport(String taskId) {
        log.info("导出招商推荐报告, taskId={}", taskId);

        try {
            // 1. 从 taskId 反查报告记录（taskId 格式: GEN_<reportId>）
            Long reportId = parseReportIdFromTaskId(taskId);
            if (reportId == null) {
                log.warn("无法从 taskId 解析报告ID: taskId={}", taskId);
                return null;
            }
            ReportRecord report = reportMapper.selectById(reportId);
            if (report == null) {
                log.warn("未找到报告记录, reportId={}, taskId={}", reportId, taskId);
                return null;
            }

            // 2. 生成 Excel 文件（基于报告记录中的企业数据重新查询组装）
            //    这里生成一份包含报告元信息 + 企业清单的汇总表
            byte[] excelData = buildReportSummaryExcel(report);

            // 3. 上传到 MinIO
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FMT));
            String fileName = "report_" + report.getId() + "_" + timestamp + ".xlsx";
            if (minioFileService == null) {
                log.warn("MinIO 未启用, 返回本地临时路径");
                return "/tmp/" + fileName;
            }
            String url = minioFileService.uploadToCategory("reports", fileName, excelData, EXCEL_MIME);

            if (url == null) {
                log.warn("推荐报告上传 MinIO 失败, 返回本地临时路径");
                return "/tmp/" + fileName;
            }

            // 4. 更新报告记录的文件URL和状态
            report.setFileUrl(url);
            report.setStatus("completed");
            report.setOutputFormat("Excel");
            reportMapper.update(report);

            log.info("招商推荐报告导出成功: reportId={}, url={}", report.getId(), url);
            return url;

        } catch (Exception e) {
            log.error("导出招商推荐报告失败: taskId={}, error={}", taskId, e.getMessage(), e);
            return null;
        }
    }

    // ==================== Excel 生成方法 ====================

    /**
     * 构建技术画像 Excel 文件
     */
    private byte[] buildProfileExcel(TechnicalProfileResponse profile) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            // ---- 样式定义 ----
            CellStyle titleStyle = ExcelStyleUtils.createTitleStyle(wb);
            CellStyle headerStyle = ExcelStyleUtils.createHeaderStyle(wb);
            CellStyle dataStyle = ExcelStyleUtils.createDataStyle(wb);
            CellStyle labelStyle = ExcelStyleUtils.createLabelStyle(wb);

            // ---- Sheet1: 企业概览 ----
            Sheet overviewSheet = wb.createSheet("企业技术概览");
            int rowIdx = 0;

            // 标题行
            Row titleRow = overviewSheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(profile.getEnterpriseName() + " — 技术画像报告");
            titleCell.setCellStyle(titleStyle);
            overviewSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            rowIdx++; // 空行

            // 基本信息
            ExcelStyleUtils.addKvRow(overviewSheet, rowIdx++, "企业名称", profile.getEnterpriseName(), labelStyle, dataStyle);
            ExcelStyleUtils.addKvRow(overviewSheet, rowIdx++, "专利总数", String.valueOf(profile.getTotalPatents()), labelStyle, dataStyle);
            ExcelStyleUtils.addKvRow(overviewSheet, rowIdx++, "有效专利数", String.valueOf(profile.getValidPatents()), labelStyle, dataStyle);
            ExcelStyleUtils.addKvRow(overviewSheet, rowIdx++, "研发能力评级", profile.getRdCapabilityLevel(), labelStyle, dataStyle);
            ExcelStyleUtils.addKvRow(overviewSheet, rowIdx++, "技术新颖度评分", profile.getNoveltyScore() + " / 100", labelStyle, dataStyle);
            ExcelStyleUtils.addKvRow(overviewSheet, rowIdx++, "技术影响力评分", profile.getInfluenceScore() + " / 100", labelStyle, dataStyle);
            ExcelStyleUtils.addKvRow(overviewSheet, rowIdx++, "生成时间", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), labelStyle, dataStyle);

            // 自动列宽
            overviewSheet.setColumnWidth(0, 18 * 256);
            overviewSheet.setColumnWidth(1, 40 * 256);

            // ---- Sheet2: 技术领域分布 ----
            Sheet fieldSheet = wb.createSheet("技术领域分布");
            Row fieldHeader = fieldSheet.createRow(0);
            String[] fieldHeaders = {"IPC分类", "技术领域", "专利数量", "最新专利日期"};
            for (int i = 0; i < fieldHeaders.length; i++) {
                Cell cell = fieldHeader.createCell(i);
                cell.setCellValue(fieldHeaders[i]);
                cell.setCellStyle(headerStyle);
            }
            int fieldRow = 1;
            List<TechnologyField> fields = profile.getTechnologyFields();
            if (fields != null) {
                for (TechnologyField f : fields) {
                    Row r = fieldSheet.createRow(fieldRow++);
                    r.createCell(0).setCellValue(f.getIpcCode() != null ? f.getIpcCode() : "");
                    r.createCell(1).setCellValue(f.getFieldName() != null ? f.getFieldName() : "");
                    r.createCell(2).setCellValue(f.getPatentCount() != null ? f.getPatentCount() : 0);
                    r.createCell(3).setCellValue(f.getLatestPatentDate() != null ? f.getLatestPatentDate() : "");
                    for (int i = 0; i < 4; i++) r.getCell(i).setCellStyle(dataStyle);
                }
            }
            fieldSheet.setColumnWidth(0, 14 * 256);
            fieldSheet.setColumnWidth(1, 24 * 256);
            fieldSheet.setColumnWidth(2, 12 * 256);
            fieldSheet.setColumnWidth(3, 16 * 256);

            // ---- Sheet3: 技术优势领域 ----
            Sheet advSheet = wb.createSheet("技术优势领域");
            Row advHeader = advSheet.createRow(0);
            String[] advHeaders = {"领域名称", "IPC分类", "企业专利数", "行业均值", "优势倍数"};
            for (int i = 0; i < advHeaders.length; i++) {
                Cell cell = advHeader.createCell(i);
                cell.setCellValue(advHeaders[i]);
                cell.setCellStyle(headerStyle);
            }
            int advRow = 1;
            List<AdvantageField> advantages = profile.getAdvantages();
            if (advantages != null) {
                for (AdvantageField adv : advantages) {
                    Row r = advSheet.createRow(advRow++);
                    r.createCell(0).setCellValue(adv.getFieldName() != null ? adv.getFieldName() : "");
                    r.createCell(1).setCellValue(adv.getIpcCode() != null ? adv.getIpcCode() : "");
                    r.createCell(2).setCellValue(adv.getEnterprisePatentCount() != null ? adv.getEnterprisePatentCount() : 0);
                    r.createCell(3).setCellValue(adv.getIndustryAvg() != null ? adv.getIndustryAvg() : 0.0);
                    r.createCell(4).setCellValue(adv.getRatio() != null ? adv.getRatio() : 0.0);
                    for (int i = 0; i < 5; i++) r.getCell(i).setCellStyle(dataStyle);
                }
            }
            advSheet.setColumnWidth(0, 24 * 256);
            advSheet.setColumnWidth(1, 14 * 256);
            advSheet.setColumnWidth(2, 14 * 256);
            advSheet.setColumnWidth(3, 12 * 256);
            advSheet.setColumnWidth(4, 12 * 256);

            // ---- Sheet4: 技术短板与研发方向 ----
            Sheet swSheet = wb.createSheet("短板与研发方向");
            int swRow = 0;

            // 短板
            Row swTitleRow = swSheet.createRow(swRow++);
            Cell swTitle = swTitleRow.createCell(0);
            swTitle.setCellValue("技术短板");
            swTitle.setCellStyle(headerStyle);
            List<String> weaknesses = profile.getWeaknesses();
            if (weaknesses != null) {
                for (String w : weaknesses) {
                    Row r = swSheet.createRow(swRow++);
                    r.createCell(0).setCellValue(w);
                    r.getCell(0).setCellStyle(dataStyle);
                }
            } else {
                Row r = swSheet.createRow(swRow++);
                r.createCell(0).setCellValue("无明显短板");
                r.getCell(0).setCellStyle(dataStyle);
            }
            swRow++; // 空行

            // 研发方向
            Row rdTitleRow = swSheet.createRow(swRow++);
            Cell rdTitle = rdTitleRow.createCell(0);
            rdTitle.setCellValue("研发方向建议");
            rdTitle.setCellStyle(headerStyle);
            List<String> rdDirections = profile.getRdDirections();
            if (rdDirections != null) {
                for (String d : rdDirections) {
                    Row r = swSheet.createRow(swRow++);
                    r.createCell(0).setCellValue(d);
                    r.getCell(0).setCellStyle(dataStyle);
                }
            } else {
                Row r = swSheet.createRow(swRow++);
                r.createCell(0).setCellValue("暂无推断方向");
                r.getCell(0).setCellStyle(dataStyle);
            }
            swSheet.setColumnWidth(0, 60 * 256);

            // 写出字节数组
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    /**
     * 构建报告汇总 Excel（招商推荐清单汇总）
     */
    private byte[] buildReportSummaryExcel(ReportRecord report) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle titleStyle = ExcelStyleUtils.createTitleStyle(wb);
            CellStyle headerStyle = ExcelStyleUtils.createHeaderStyle(wb);
            CellStyle dataStyle = ExcelStyleUtils.createDataStyle(wb);
            CellStyle labelStyle = ExcelStyleUtils.createLabelStyle(wb);

            Sheet sheet = wb.createSheet("招商推荐报告");

            int rowIdx = 0;
            // 标题
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(report.getTitle() != null ? report.getTitle() : "招商推荐报告");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
            rowIdx++;

            // 报告元信息
            ExcelStyleUtils.addKvRow(sheet, rowIdx++, "报告类型",
                    getReportTypeName(report.getReportType()), labelStyle, dataStyle);
            ExcelStyleUtils.addKvRow(sheet, rowIdx++, "产业链",
                    report.getChainName() != null ? report.getChainName() : "-", labelStyle, dataStyle);
            ExcelStyleUtils.addKvRow(sheet, rowIdx++, "范围",
                    report.getScope() != null ? report.getScope() : "-", labelStyle, dataStyle);
            ExcelStyleUtils.addKvRow(sheet, rowIdx++, "覆盖企业数",
                    String.valueOf(report.getEnterpriseCount() != null ? report.getEnterpriseCount() : 0), labelStyle, dataStyle);
            ExcelStyleUtils.addKvRow(sheet, rowIdx++, "格式",
                    report.getOutputFormat() != null ? report.getOutputFormat() : "Excel", labelStyle, dataStyle);
            ExcelStyleUtils.addKvRow(sheet, rowIdx++, "生成时间",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), labelStyle, dataStyle);
            rowIdx++;

            // 表头
            Row hRow = sheet.createRow(rowIdx++);
            String[] headers = {"序号", "企业名称", "行业", "区域", "综合评分", "评级", "匹配度", "推荐理由"};
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // 数据行（报告中已有企业数据则填充）
            if (report.getEnterpriseCount() != null && report.getEnterpriseCount() > 0) {
                for (int i = 1; i <= report.getEnterpriseCount(); i++) {
                    Row r = sheet.createRow(rowIdx++);
                    r.createCell(0).setCellValue(i);
                    // 其他字段依赖数据库查询，这里预留
                    for (int j = 0; j < headers.length; j++) r.getCell(j).setCellStyle(dataStyle);
                }
            } else {
                Row r = sheet.createRow(rowIdx);
                r.createCell(0).setCellValue("暂无企业数据，请通过招商推荐模块生成推荐列表后导出");
                r.getCell(0).setCellStyle(dataStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 7));
            }

            // 列宽
            int[] widths = {6, 28, 16, 10, 12, 8, 10, 40};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    /**
     * 从 taskId 解析报告ID
     * taskId 格式: GEN_<reportId> 或 EXPORT_ALL_<uuid>
     */
    private Long parseReportIdFromTaskId(String taskId) {
        if (taskId == null) return null;
        try {
            if (taskId.startsWith("GEN_")) {
                String idStr = taskId.substring(4);
                return Long.parseLong(idStr);
            }
        } catch (NumberFormatException e) {
            log.warn("taskId 解析失败: {}", taskId);
        }
        return null;
    }

    private String getReportTypeName(String type) {
        if (type == null) return "未知类型";
        switch (type) {
            case "investment_list": return "招商推荐清单";
            case "diagnosis": return "产业链诊断报告";
            case "enterprise_profile": return "企业画像报告";
            case "chain_supplement": return "补链建议方案";
            default: return type;
        }
    }
}
