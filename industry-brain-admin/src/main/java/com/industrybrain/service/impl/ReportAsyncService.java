package com.industrybrain.service.impl;

import com.industrybrain.domain.model.InvestmentRecommendation;
import com.industrybrain.domain.model.ReportRecord;
import com.industrybrain.domain.model.NodeDiagnosis;
import com.industrybrain.domain.model.ChainNode;
import com.industrybrain.domain.model.IndustryChain;
import com.industrybrain.domain.model.Region;
import com.industrybrain.dto.request.ReportGenerateRequest;
import com.industrybrain.infra.MinioFileService;
import com.industrybrain.mapper.mysql.InvestmentRecommendationMapper;
import com.industrybrain.mapper.mysql.ReportMapper;
import com.industrybrain.mapper.mysql.NodeDiagnosisMapper;
import com.industrybrain.mapper.mysql.ChainNodeMapper;
import com.industrybrain.mapper.mysql.ChainInfoMapper;
import com.industrybrain.mapper.mysql.RegionMapper;
import com.industrybrain.service.llm.LLMClient;
import com.industrybrain.utils.ExcelStyleUtils;
import com.industrybrain.utils.PdfReportUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * 报告异步生成服务 — 独立 Bean 以规避 Spring AOP 自调用失效问题
 *
 * 说明：若在 RecommendationServiceImpl 内部直接调用自身的 @Async 方法，
 * 走的是 this.xxx() 而不是 Spring 代理，@Async 不生效，异步变同步，
 * HTTP 请求会一直阻塞到报告生成完成。拆分为独立 Bean 后，
 * 通过注入的代理调用即可真正异步执行。
 */
@Service
@ConditionalOnProperty(name = "mysql.enabled", havingValue = "true", matchIfMissing = true)
public class ReportAsyncService {

    private static final Logger log = LoggerFactory.getLogger(ReportAsyncService.class);

    private static final String EXCEL_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PDF_MIME = "application/pdf";
    private static final String DATE_FMT = "yyyyMMdd_HHmmss";

    @Resource
    private ReportMapper reportMapper;

    @Resource
    private InvestmentRecommendationMapper recommendationMapper;

    @Resource
    private MinioFileService minioFileService;

    @Resource
    private LLMClient llmClient;

    @Resource
    private NodeDiagnosisMapper nodeDiagnosisMapper;

    @Resource
    private ChainNodeMapper chainNodeMapper;

    @Resource
    private ChainInfoMapper chainInfoMapper;

    @Resource
    private RegionMapper regionMapper;

    /**
     * 异步生成报告 — 由外部 Bean 调用以确保 @Async 生效
     */
    @Async("reportTaskExecutor")
    public void generateReportAsync(Long reportId, ReportGenerateRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("异步报告生成开始: reportId={}, type={}, thread={}",
                reportId, request.getType(), Thread.currentThread().getName());

        ReportRecord record = reportMapper.selectById(reportId);
        if (record == null) {
            log.error("报告记录不存在: reportId={}", reportId);
            return;
        }

        try {
            record.setStatus("processing");
            reportMapper.update(record);

            // 规范化输出格式：Excel/XLS/XLSX → XLSX；PDF → PDF
            String format = normalizeFormat(request.getFormat());
            boolean isPdf = "PDF".equalsIgnoreCase(format);

            byte[] fileData;
            String mime;
            int enterpriseCount;

            // 异步收集 LLM 分析内容（不阻塞报告生成，失败时降级为规则模板）
            String aiAnalysis = null;
            if (llmClient.isEnabled()) {
                try {
                    aiAnalysis = collectAIAnalysis(request, record);
                } catch (Exception e) {
                    log.warn("AI 分析生成失败，使用规则模板降级: {}", e.getMessage());
                }
            }

            if ("investment_list".equals(request.getType())) {
                List<InvestmentRecommendation> enterprises = recommendationMapper.selectMatchList(
                        null,
                        request.getWeakness(),
                        request.getMatchMin(),
                        request.getScale(),
                        request.getSortBy(),
                        request.getKeyword(),
                        request.getNodeId(),
                        null, 0, 200);
                enterpriseCount = enterprises.size();
                if (isPdf) {
                    List<PdfReportUtils.PdfCompanyRow> rows = enterprises.stream()
                            .map(PdfReportUtils.PdfCompanyRow::of)
                            .collect(Collectors.toList());
                    // 传入 AI 分析内容，生成增强版 PDF
                    fileData = PdfReportUtils.buildInvestmentListPdf(record.getTitle(), rows, aiAnalysis);
                    mime = PDF_MIME;
                } else {
                    fileData = buildInvestmentListExcel(enterprises, record.getTitle());
                    mime = EXCEL_MIME;
                }
            } else {
                enterpriseCount = 0;
                if (isPdf) {
                    // 使用 AI 分析替换规则模板描述
                    String description = (aiAnalysis != null && !aiAnalysis.isBlank())
                            ? aiAnalysis
                            : buildReportDescription(request.getType());
                    fileData = PdfReportUtils.buildSummaryPdf(record.getTitle(),
                            getReportTypeName(record.getReportType()), record.getScope(),
                            format, description);
                    mime = PDF_MIME;
                } else {
                    fileData = buildGenericReportExcel(record);
                    mime = EXCEL_MIME;
                }
            }

            String fileName = "report_" + reportId + "_" + timestamp() + "." + format.toLowerCase();
            if (minioFileService == null) {
                log.warn("MinIO 未启用，跳过报告上传");
                record.setStatus("failed");
                reportMapper.update(record);
                return;
            }
            String url = minioFileService.uploadToCategory("reports", fileName, fileData, mime);

            if (url == null) {
                log.error("报告文件上传 MinIO 失败: reportId={}", reportId);
                record.setStatus("failed");
                reportMapper.update(record);
                return;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            record.setFileUrl(url);
            record.setFileSize((long) fileData.length);
            record.setEnterpriseCount(enterpriseCount);
            record.setOutputFormat(format);
            record.setStatus("done");
            record.setGenerationTime(elapsed);
            reportMapper.update(record);

            log.info("异步报告生成完成: reportId={}, 企业数={}, 耗时={}ms",
                    reportId, enterpriseCount, elapsed);

        } catch (Exception e) {
            log.error("异步报告生成失败: reportId={}, error={}", reportId, e.getMessage(), e);
            try {
                record.setStatus("failed");
                reportMapper.update(record);
            } catch (Exception ex) {
                log.error("更新报告失败状态异常: {}", ex.getMessage());
            }
        }
    }

    /**
     * 异步批量导出全部已完成报告
     */
    @Async("reportTaskExecutor")
    public void exportAllAsync(String taskId, String format, String timestamp) {
        log.info("异步批量导出开始: taskId={}, format={}, thread={}",
                taskId, format, Thread.currentThread().getName());

        try {
            List<ReportRecord> allReports = reportMapper.selectList(0, 1000);
            // 数据库 status CHECK 约束: pending/processing/done/failed，已完成状态为 done
            List<ReportRecord> completedReports = allReports.stream()
                    .filter(r -> "done".equals(r.getStatus()))
                    .collect(Collectors.toList());

            byte[] excelData = buildBatchExportExcel(completedReports);

            String fileName = "batch_export_" + taskId + "_" + timestamp + ".xlsx";
            if (minioFileService == null) {
                log.warn("MinIO 未启用，跳过批量导出上传");
                return;
            }
            String url = minioFileService.uploadToCategory("exports", fileName, excelData, EXCEL_MIME);

            if (url != null) {
                log.info("批量导出完成: taskId={}, 报告数={}, url={}",
                        taskId, completedReports.size(), url);
            } else {
                log.warn("批量导出上传 MinIO 失败: taskId={}", taskId);
            }

        } catch (Exception e) {
            log.error("批量导出失败: taskId={}, error={}", taskId, e.getMessage(), e);
        }
    }

    // ==================== Excel 生成方法 ====================

    private byte[] buildInvestmentListExcel(List<InvestmentRecommendation> enterprises, String title) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle titleStyle = ExcelStyleUtils.createTitleStyle(wb);
            CellStyle headerStyle = ExcelStyleUtils.createHeaderStyle(wb);
            CellStyle dataStyle = ExcelStyleUtils.createDataStyle(wb);
            CellStyle aStyle = ExcelStyleUtils.createHighlightStyle(wb, IndexedColors.GREEN);
            CellStyle bStyle = ExcelStyleUtils.createHighlightStyle(wb, IndexedColors.LIGHT_BLUE);
            CellStyle cStyle = ExcelStyleUtils.createHighlightStyle(wb, IndexedColors.GREY_40_PERCENT);

            Sheet sheet = wb.createSheet("招商推荐清单");

            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));
            rowIdx++;

            Row hRow = sheet.createRow(rowIdx++);
            String[] headers = {"序号", "企业名称", "行业", "区域", "节点名称", "诊断标签",
                    "业务匹配度", "规模实力", "地域协同", "综合评分", "评级"};
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int seq = 1;
            for (InvestmentRecommendation ent : enterprises) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(seq++);
                r.createCell(1).setCellValue(ent.getEnterpriseName() != null ? ent.getEnterpriseName() : "");
                r.createCell(2).setCellValue(ent.getIndustry() != null ? ent.getIndustry() : "");
                r.createCell(3).setCellValue(ent.getRegionCode() != null ? ent.getRegionCode() : "");
                r.createCell(4).setCellValue(ent.getNodeName() != null ? ent.getNodeName() : "");
                r.createCell(5).setCellValue(ent.getDiagnosisTag() != null ? ent.getDiagnosisTag() : "");
                r.createCell(6).setCellValue(ent.getBusinessFitScore() != null ? ent.getBusinessFitScore().doubleValue() : 0);
                r.createCell(7).setCellValue(ent.getScaleStrengthScore() != null ? ent.getScaleStrengthScore().doubleValue() : 0);
                r.createCell(8).setCellValue(ent.getRegionalSynergyScore() != null ? ent.getRegionalSynergyScore().doubleValue() : 0);
                r.createCell(9).setCellValue(ent.getTotalScore() != null ? ent.getTotalScore().doubleValue() : 0);

                String rating = ent.getRating();
                r.createCell(10).setCellValue(rating != null ? rating : "");
                CellStyle ratingStyle = dataStyle;
                if ("A".equals(rating)) ratingStyle = aStyle;
                else if ("B".equals(rating)) ratingStyle = bStyle;
                else if ("C".equals(rating)) ratingStyle = cStyle;

                for (int i = 0; i < headers.length; i++) {
                    if (i == 10) r.getCell(i).setCellStyle(ratingStyle);
                    else r.getCell(i).setCellStyle(dataStyle);
                }
            }

            int[] widths = {6, 28, 16, 10, 14, 10, 10, 10, 10, 10, 8};
            for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    private byte[] buildGenericReportExcel(ReportRecord record) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle titleStyle = ExcelStyleUtils.createTitleStyle(wb);
            CellStyle headerStyle = ExcelStyleUtils.createHeaderStyle(wb);
            CellStyle dataStyle = ExcelStyleUtils.createDataStyle(wb);
            CellStyle labelStyle = ExcelStyleUtils.createLabelStyle(wb);

            Sheet sheet = wb.createSheet("报告摘要");
            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(record.getTitle());
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            rowIdx++;

            ExcelStyleUtils.addKvRow(sheet, rowIdx++, "报告类型", getReportTypeName(record.getReportType()), labelStyle, dataStyle);
            ExcelStyleUtils.addKvRow(sheet, rowIdx++, "产业链", record.getChainName() != null ? record.getChainName() : "-", labelStyle, dataStyle);
            ExcelStyleUtils.addKvRow(sheet, rowIdx++, "范围", record.getScope() != null ? record.getScope() : "全部", labelStyle, dataStyle);
            ExcelStyleUtils.addKvRow(sheet, rowIdx++, "输出格式", record.getOutputFormat() != null ? record.getOutputFormat() : "Excel", labelStyle, dataStyle);
            ExcelStyleUtils.addKvRow(sheet, rowIdx++, "生成时间", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), labelStyle, dataStyle);
            rowIdx++;

            Row noteRow = sheet.createRow(rowIdx);
            Cell noteCell = noteRow.createCell(0);
            noteCell.setCellValue("此报告为自动生成，具体企业详情请通过招商推荐模块查看。");
            noteCell.setCellStyle(dataStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 3));

            sheet.setColumnWidth(0, 16 * 256);
            sheet.setColumnWidth(1, 40 * 256);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    private byte[] buildBatchExportExcel(List<ReportRecord> reports) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle titleStyle = ExcelStyleUtils.createTitleStyle(wb);
            CellStyle headerStyle = ExcelStyleUtils.createHeaderStyle(wb);
            CellStyle dataStyle = ExcelStyleUtils.createDataStyle(wb);

            Sheet sheet = wb.createSheet("批量导出汇总");
            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("招商报告批量导出汇总");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
            rowIdx++;

            Row hRow = sheet.createRow(rowIdx++);
            String[] headers = {"序号", "报告标题", "类型", "企业数", "格式", "状态", "生成时间"};
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int seq = 1;
            for (ReportRecord r : reports) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(seq++);
                row.createCell(1).setCellValue(r.getTitle() != null ? r.getTitle() : "");
                row.createCell(2).setCellValue(getReportTypeName(r.getReportType()));
                row.createCell(3).setCellValue(r.getEnterpriseCount() != null ? r.getEnterpriseCount() : 0);
                row.createCell(4).setCellValue(r.getOutputFormat() != null ? r.getOutputFormat() : "");
                row.createCell(5).setCellValue("done".equals(r.getStatus()) ? "已完成" : r.getStatus());
                row.createCell(6).setCellValue(r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
                for (int i = 0; i < headers.length; i++) row.getCell(i).setCellStyle(dataStyle);
            }

            int[] widths = {6, 36, 18, 10, 10, 10, 20};
            for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
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

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FMT));
    }

    /**
     * 规范化输出格式：Excel/XLS/XLSX → XLSX；PDF → PDF；其他 → XLSX
     */
    private String normalizeFormat(String format) {
        if (format == null) return "XLSX";
        if ("PDF".equalsIgnoreCase(format)) return "PDF";
        return "XLSX"; // Excel/XLS/XLSX/其他 → XLSX
    }

    /**
     * 摘要类 PDF 报告的说明文字
     */
    private String buildReportDescription(String type) {
        if ("diagnosis".equals(type)) {
            return "本报告为产业链诊断报告，汇总目标产业链各环节的强弱链诊断结果，"
                    + "识别薄弱环节与缺口环节，为精准招商提供决策依据。\n\n"
                    + "详细诊断数据请前往「产业链诊断」模块查看。";
        }
        if ("enterprise_profile".equals(type)) {
            return "本报告为企业画像报告，汇总目标企业的技术实力、专利布局、经营规模与"
                    + "产业链匹配度评估。\n\n详细企业画像请前往「精准招商」模块查看。";
        }
        if ("chain_supplement".equals(type)) {
            return "本报告为补链建议方案，基于产业链诊断结果提出补链、强链、延链的招商"
                    + "建议与目标企业名单。\n\n详细补链建议请前往「产业链全景」模块查看。";
        }
        return "本报告为自动生成的招商报告，具体企业详情请通过招商推荐模块查看。";
    }

    /**
     * 收集 LLM 分析内容
     * 根据报告类型采集相关数据并调用 LLM 生成分析文本。
     * 失败时返回 null，调用方降级为规则模板。
     */
    private String collectAIAnalysis(ReportGenerateRequest request, ReportRecord record) {
        String type = request.getType();
        if ("diagnosis".equals(type) || "chain_supplement".equals(type)) {
            return generateDiagnosisAnalysis(request, record);
        }
        if ("investment_list".equals(type)) {
            return generateInvestmentAnalysis(request);
        }
        return null;
    }

    /**
     * 生成产业链诊断 / 补链建议的 AI 分析
     */
    private String generateDiagnosisAnalysis(ReportGenerateRequest request, ReportRecord record) {
        try {
            // 收集诊断数据
            List<NodeDiagnosis> diagnoses;
            if (request.getNodeId() != null) {
                NodeDiagnosis nd = nodeDiagnosisMapper.selectLatestByNodeId(request.getNodeId());
                diagnoses = nd != null ? List.of(nd) : Collections.emptyList();
            } else {
                diagnoses = nodeDiagnosisMapper.selectAllLatest();
            }
            if (diagnoses.isEmpty()) {
                log.info("无诊断数据，跳过 AI 分析");
                return null;
            }

            // 收集节点信息
            List<ChainNode> nodes = chainNodeMapper.selectList(null);
            Map<Long, ChainNode> nodeMap = nodes.stream()
                    .collect(Collectors.toMap(ChainNode::getId, n -> n, (a, b) -> a));

            // 收集产业链信息
            String chainName = null;
            if (request.getChainId() != null) {
                List<IndustryChain> chains = chainInfoMapper.selectAllOrdered();
                if (chains != null) {
                    for (IndustryChain c : chains) {
                        if (request.getChainId().equals(c.getId())) {
                            chainName = c.getName();
                            break;
                        }
                    }
                }
            }

            // 收集区域信息
            String regionName = null;
            if (record.getRegionId() != null) {
                Region region = regionMapper.selectById(record.getRegionId());
                if (region != null) {
                    regionName = region.getName();
                }
            }

            // 构建诊断数据摘要
            StringBuilder sb = new StringBuilder();
            sb.append("【产业链】").append(chainName != null ? chainName : "全部产业链");
            sb.append("【区域】").append(regionName != null ? regionName : "全部区域");
            sb.append("\n\n");

            // 统计诊断标签分布
            long strongCount = diagnoses.stream().filter(d -> "强链".equals(d.getDiagnosisTag())).count();
            long supplementCount = diagnoses.stream().filter(d -> "补链".equals(d.getDiagnosisTag())).count();
            long weakCount = diagnoses.stream().filter(d -> "弱链".equals(d.getDiagnosisTag())).count();
            long missingCount = diagnoses.stream().filter(d -> "缺链".equals(d.getDiagnosisTag())).count();

            sb.append("【诊断概况】\n");
            sb.append(String.format("强链环节：%d个 | 补链环节：%d个 | 弱链环节：%d个 | 缺链环节：%d个\n\n",
                    strongCount, supplementCount, weakCount, missingCount));

            // 弱链和缺链节点详情（优先）
            List<NodeDiagnosis> targetNodes = diagnoses.stream()
                    .filter(d -> "弱链".equals(d.getDiagnosisTag()) || "缺链".equals(d.getDiagnosisTag()))
                    .sorted(Comparator.comparingInt(d -> d.getLocalEnterpriseShare() != null
                            ? d.getLocalEnterpriseShare().intValue() : 999))
                    .limit(10)
                    .collect(Collectors.toList());

            if (!targetNodes.isEmpty()) {
                sb.append("【薄弱环节详情】\n");
                for (NodeDiagnosis nd : targetNodes) {
                    ChainNode node = nodeMap.get(nd.getNodeId());
                    String nodeName = node != null ? node.getNodeName() : "未知节点";
                    String layer = node != null ? node.getLayer() : "";
                    String layerLabel = "up".equals(layer) ? "上游" : "mid".equals(layer) ? "中游" : "down".equals(layer) ? "下游" : "";
                    String share = nd.getLocalEnterpriseShare() != null
                            ? String.format("%.2f%%", nd.getLocalEnterpriseShare().multiply(new java.math.BigDecimal("100")).doubleValue())
                            : "未知";
                    sb.append(String.format("- %s [%s] 本地企业占比%s (本地%d家/全国%d家)\n",
                            nodeName, layerLabel, share,
                            nd.getLocalEnterpriseCount(), nd.getNationalEnterpriseCount()));
                }
            }

            // 调用 LLM 生成分析
            String prompt = buildDiagnosisPrompt(sb.toString());
            return llmClient.generate(prompt);
        } catch (Exception e) {
            log.warn("生成诊断分析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 生成招商推荐清单的 AI 分析
     */
    private String generateInvestmentAnalysis(ReportGenerateRequest request) {
        try {
            // 收集推荐企业数据
            List<InvestmentRecommendation> enterprises = recommendationMapper.selectMatchList(
                    null, request.getWeakness(), request.getMatchMin(),
                    request.getScale(), request.getSortBy(), request.getKeyword(),
                    request.getNodeId(), null, 0, 50);

            if (enterprises.isEmpty()) {
                log.info("无推荐企业数据，跳过 AI 分析");
                return null;
            }

            // 收集节点和产业链信息
            String nodeName = null;
            String chainName = null;
            if (request.getNodeId() != null) {
                ChainNode node = chainNodeMapper.selectById(request.getNodeId());
                if (node != null) {
                    nodeName = node.getNodeName();
                    if (node.getChainId() != null) {
                        List<IndustryChain> chains = chainInfoMapper.selectAllOrdered();
                        for (IndustryChain c : chains) {
                            if (node.getChainId().equals(c.getId())) {
                                chainName = c.getName();
                                break;
                            }
                        }
                    }
                }
            }

            // 构建企业摘要
            StringBuilder sb = new StringBuilder();
            sb.append("【产业链】").append(chainName != null ? chainName : "全部产业链");
            if (nodeName != null) {
                sb.append("【节点】").append(nodeName);
            }
            sb.append("\n\n");
            sb.append(String.format("推荐企业数：%d家\n", enterprises.size()));

            // 按评级汇总
            long countA = enterprises.stream().filter(e -> "A".equals(e.getRating())).count();
            long countB = enterprises.stream().filter(e -> "B".equals(e.getRating())).count();
            long countC = enterprises.stream().filter(e -> "C".equals(e.getRating())).count();
            sb.append(String.format("A级（强烈推荐）：%d家 | B级（推荐关注）：%d家 | C级（观察）：%d家\n\n", countA, countB, countC));

            // 前5家企业详情
            enterprises.stream().limit(5).forEach(e -> {
                sb.append(String.format("- %s [%s] 综合分:%.0f (业务契合:%.0f, 规模实力:%.0f, 地域协同:%.0f)\n",
                        e.getEnterpriseName(), e.getIndustry() != null ? e.getIndustry() : "未知",
                        e.getTotalScore() != null ? e.getTotalScore().doubleValue() : 0,
                        e.getBusinessFitScore() != null ? e.getBusinessFitScore().doubleValue() : 0,
                        e.getScaleStrengthScore() != null ? e.getScaleStrengthScore().doubleValue() : 0,
                        e.getRegionalSynergyScore() != null ? e.getRegionalSynergyScore().doubleValue() : 0));
            });

            String prompt = buildInvestmentPrompt(sb.toString());
            return llmClient.generate(prompt);
        } catch (Exception e) {
            log.warn("生成招商分析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建诊断分析 Prompt
     */
    private String buildDiagnosisPrompt(String dataSummary) {
        return "你是一位资深的产业经济分析师，擅长产业链诊断与招商引资策略分析。\n\n"
                + "请根据以下产业链诊断数据，生成一份专业、简洁的分析报告。\n\n"
                + dataSummary
                + "\n【分析要求】\n"
                + "1. 简要总结该产业链的整体发展态势（2-3句话）\n"
                + "2. 重点分析薄弱环节（弱链/缺链）的成因和影响（3-4句话）\n"
                + "3. 提出3-5条具体、可操作的招商引资建议\n"
                + "4. 语言专业、适合政府招商部门阅读，避免空话套话\n\n"
                + "【输出格式】\n直接输出分析内容，分段落，每段2-4句话，不要标题。";
    }

    /**
     * 构建招商推荐分析 Prompt
     */
    private String buildInvestmentPrompt(String dataSummary) {
        return "你是一位资深的产业招商顾问，擅长企业匹配分析和招商策略制定。\n\n"
                + "请根据以下招商推荐数据，生成一份专业的招商分析报告。\n\n"
                + dataSummary
                + "\n【分析要求】\n"
                + "1. 简要评价推荐企业的整体质量（2-3句话）\n"
                + "2. 分析 Top 推荐企业的核心优势（3-4句话）\n"
                + "3. 提出2-3条后续对接建议（如重点跟进方向、对接策略等）\n"
                + "4. 语言专业简洁，适合政府招商部门决策参考\n\n"
                + "【输出格式】\n直接输出分析内容，分段落，每段2-4句话，不要标题。";
    }
}
