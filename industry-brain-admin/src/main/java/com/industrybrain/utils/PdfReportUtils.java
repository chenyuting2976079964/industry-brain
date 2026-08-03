package com.industrybrain.utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDF 报告生成器 — 基于 iText 5 + 内嵌中文字体
 *
 * 支持两种报告：
 *   1. 招商推荐清单表格 PDF（investment_list）：标题 + 企业表格（名称/行业/区域/节点/诊断标签/三维度评分/评级）
 *   2. 摘要类 PDF（diagnosis/enterprise_profile/chain_supplement）：标题 + 键值摘要 + 说明
 *
 * 字体方案：
 *   - 优先从 classpath 加载内嵌字体 fonts/Deng.ttf（等线），保证跨环境可用
 *   - 加载失败时回退到操作系统常见中文字体
 *   - 字体对象缓存复用，避免每次生成都重新解析 16MB 字体
 */
public class PdfReportUtils {

    private static final Logger log = LoggerFactory.getLogger(PdfReportUtils.class);

    private static final String FONT_PATH = "/fonts/Deng.ttf";

    /** 缓存字体，避免重复加载大字体文件 */
    private static volatile BaseFont cachedBaseFont;

    // 色彩常量（与前端主题一致）
    private static final BaseColor COLOR_TITLE = new BaseColor(30, 64, 175);       // #1e40af 深蓝
    private static final BaseColor COLOR_HEADER_BG = new BaseColor(30, 64, 175);   // #1e40af
    private static final BaseColor COLOR_HEADER_TEXT = BaseColor.WHITE;
    private static final BaseColor COLOR_BORDER = new BaseColor(226, 232, 240);    // #e2e8f0
    private static final BaseColor COLOR_TEXT = new BaseColor(30, 41, 59);         // #1e293b
    private static final BaseColor COLOR_SUBTEXT = new BaseColor(100, 116, 139);   // #64748b
    private static final BaseColor COLOR_ALT_BG = new BaseColor(248, 250, 252);    // #f8fafc
    private static final BaseColor COLOR_AI_BG = new BaseColor(239, 246, 255);     // blue-50
    private static final BaseColor COLOR_AI_BORDER = new BaseColor(147, 197, 253); // blue-300
    private static final BaseColor COLOR_AI_TITLE = new BaseColor(30, 64, 175);    // blue-800

    /** A4 横向 */
    private static final Rectangle PAGE_LANDSCAPE = new Rectangle(PageSize.A4.rotate());
    /** A4 纵向 */
    private static final Rectangle PAGE_PORTRAIT = new Rectangle(PageSize.A4);

    private PdfReportUtils() {}

    // ==================== 公开 API ====================

    /**
     * 生成招商推荐清单表格 PDF
     *
     * @param title     报告标题
     * @param companies 招商推荐企业列表（按 Excel 生成器同构）
     */
    public static byte[] buildInvestmentListPdf(String title, List<PdfCompanyRow> companies) throws DocumentException, java.io.IOException {
        return buildInvestmentListPdf(title, companies, null);
    }

    /**
     * 生成招商推荐清单表格 PDF（增强版，含 AI 分析）
     *
     * @param title      报告标题
     * @param companies  招商推荐企业列表
     * @param aiAnalysis AI 分析文本（可为 null，为 null 时退化为普通版本）
     */
    public static byte[] buildInvestmentListPdf(String title, List<PdfCompanyRow> companies, String aiAnalysis) throws DocumentException, java.io.IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Document doc = new Document(PAGE_LANDSCAPE, 30, 30, 30, 30);
            PdfWriter.getInstance(doc, bos);
            doc.open();

            addTitle(doc, title, true);

            // 统计行
            addSummaryLine(doc, "报告类型：招商推荐清单    覆盖企业数：" + companies.size()
                    + "    生成时间：" + now());

            // AI 分析章节（如果有）
            if (aiAnalysis != null && !aiAnalysis.isBlank()) {
                addAIAnalysisSection(doc, aiAnalysis);
            }

            // 表头
            PdfPTable table = new PdfPTable(11);
            table.setWidthPercentage(100);
            table.setSpacingBefore(12);
            table.setSpacingAfter(8);
            float[] colWidths = {3f, 14f, 8f, 7f, 9f, 6f, 7f, 7f, 7f, 7f, 5f};
            table.setWidths(colWidths);

            String[] headers = {"序号", "企业名称", "行业", "区域", "节点名称", "诊断标签",
                    "业务匹配", "规模实力", "地域协同", "综合分", "评级"};
            for (String h : headers) {
                addHeaderCell(table, h);
            }

            int seq = 1;
            for (PdfCompanyRow c : companies) {
                table.addCell(bodyCell(String.valueOf(seq++), Element.ALIGN_CENTER));
                table.addCell(bodyCell(safe(c.getName()), Element.ALIGN_LEFT));
                table.addCell(bodyCell(safe(c.getIndustry()), Element.ALIGN_LEFT));
                table.addCell(bodyCell(safe(c.getRegion()), Element.ALIGN_CENTER));
                table.addCell(bodyCell(safe(c.getNodeName()), Element.ALIGN_LEFT));
                table.addCell(bodyCell(safe(c.getDiagnosisTag()), Element.ALIGN_CENTER));
                table.addCell(bodyCell(fmtScore(c.getBusinessFitScore()), Element.ALIGN_CENTER));
                table.addCell(bodyCell(fmtScore(c.getScaleStrengthScore()), Element.ALIGN_CENTER));
                table.addCell(bodyCell(fmtScore(c.getRegionalSynergyScore()), Element.ALIGN_CENTER));
                table.addCell(bodyCell(fmtScore(c.getTotalScore()), Element.ALIGN_CENTER));
                table.addCell(ratingCell(safe(c.getRating())));
            }

            doc.add(table);
            doc.close();
            return bos.toByteArray();
        }
    }

    /**
     * 生成摘要类 PDF 报告（诊断 / 企业画像 / 补链建议）
     *
     * @param title       报告标题
     * @param reportType  报告类型中文名
     * @param scope       报告范围
     * @param format      输出格式（显示用）
     * @param description 报告说明
     */
    public static byte[] buildSummaryPdf(String title, String reportType, String scope,
                                         String format, String description) throws DocumentException, java.io.IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Document doc = new Document(PAGE_PORTRAIT, 45, 45, 40, 40);
            PdfWriter.getInstance(doc, bos);
            doc.open();

            addTitle(doc, title, false);

            // 键值摘要表
            PdfPTable kv = new PdfPTable(2);
            kv.setWidthPercentage(100);
            kv.setSpacingBefore(14);
            float[] kvWidths = {3f, 9f};
            kv.setWidths(kvWidths);
            addKvRow(kv, "报告类型", reportType);
            addKvRow(kv, "报告范围", scope != null ? scope : "全部");
            addKvRow(kv, "输出格式", format != null ? format : "PDF");
            addKvRow(kv, "生成时间", now());
            doc.add(kv);

            // AI 分析内容（description 可能包含 AI 生成的分析）
            if (description != null && !description.isBlank()) {
                addAnalysisSection(doc, description);
            }

            doc.close();
            return bos.toByteArray();
        }
    }

    // ==================== AI 分析章节渲染 ====================

    /**
     * 添加 AI 分析章节到文档（横向报表用，如招商推荐清单）
     */
    private static void addAIAnalysisSection(Document doc, String aiAnalysis) throws DocumentException {
        // 章节标题
        Font titleFont = font(12, BaseFont.NOT_EMBEDDED, COLOR_AI_TITLE, Font.BOLD);
        Paragraph sectionTitle = new Paragraph("💡 AI 智能分析", titleFont);
        sectionTitle.setSpacingBefore(12);
        sectionTitle.setSpacingAfter(6);
        doc.add(sectionTitle);

        // 分隔线
        Paragraph line = new Paragraph();
        line.setSpacingAfter(4);
        doc.add(line);

        // AI 分析内容（按段落分割）
        String[] paragraphs = aiAnalysis.split("\\n\\n");
        for (String para : paragraphs) {
            if (para.isBlank()) continue;
            // 处理行内换行
            String[] lines = para.split("\\n");
            for (String lineText : lines) {
                if (lineText.isBlank()) continue;
                Font paraFont = font(9, BaseFont.NOT_EMBEDDED, COLOR_TEXT, Font.NORMAL);
                Paragraph p = new Paragraph(lineText.trim(), paraFont);
                p.setSpacingBefore(4);
                p.setLeading(16);
                p.setFirstLineIndent(16);
                doc.add(p);
            }
        }

        // 底部提示
        Font hintFont = font(8, BaseFont.NOT_EMBEDDED, COLOR_SUBTEXT, Font.ITALIC);
        Paragraph hint = new Paragraph("※ 以上分析由大模型生成，仅供参考，请以实际数据为准。", hintFont);
        hint.setSpacingBefore(8);
        hint.setAlignment(Element.ALIGN_RIGHT);
        doc.add(hint);
    }

    /**
     * 添加分析章节到文档（纵向报表用，如诊断报告）
     */
    private static void addAnalysisSection(Document doc, String analysis) throws DocumentException {
        // 章节标题
        Font titleFont = font(12, BaseFont.NOT_EMBEDDED, COLOR_AI_TITLE, Font.BOLD);
        Paragraph sectionTitle = new Paragraph("📊 智能分析报告", titleFont);
        sectionTitle.setSpacingBefore(16);
        sectionTitle.setSpacingAfter(8);
        doc.add(sectionTitle);

        // AI 分析内容（按段落分割）
        String[] paragraphs = analysis.split("\\n\\n");
        for (String para : paragraphs) {
            if (para.isBlank()) continue;
            String[] lines = para.split("\\n");
            for (String lineText : lines) {
                if (lineText.isBlank()) continue;
                Font paraFont = font(10, BaseFont.NOT_EMBEDDED, COLOR_TEXT, Font.NORMAL);
                Paragraph p = new Paragraph(lineText.trim(), paraFont);
                p.setSpacingBefore(4);
                p.setLeading(18);
                p.setFirstLineIndent(20);
                doc.add(p);
            }
        }
    }

    // ==================== 表格单元格 ====================

    private static void addHeaderCell(PdfPTable table, String text) {
        Font f = font(10, BaseFont.NOT_EMBEDDED, COLOR_HEADER_TEXT, Font.BOLD);
        Paragraph p = new Paragraph(text, f);
        p.setAlignment(Element.ALIGN_CENTER);
        PdfPCell cell = new PdfPCell(p);
        cell.setBackgroundColor(COLOR_HEADER_BG);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(COLOR_HEADER_BG);
        table.addCell(cell);
    }

    private static PdfPCell bodyCell(String text, int align) {
        Font f = font(9, BaseFont.NOT_EMBEDDED, COLOR_TEXT, Font.NORMAL);
        Paragraph p = new Paragraph(text, f);
        p.setAlignment(align);
        PdfPCell cell = new PdfPCell(p);
        cell.setPadding(5);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(COLOR_BORDER);
        return cell;
    }

    private static PdfPCell ratingCell(String rating) {
        Font f = font(9, BaseFont.NOT_EMBEDDED, COLOR_TEXT, Font.BOLD);
        Paragraph p = new Paragraph(rating, f);
        p.setAlignment(Element.ALIGN_CENTER);
        PdfPCell cell = new PdfPCell(p);
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(COLOR_BORDER);
        if ("A".equalsIgnoreCase(rating)) {
            cell.setBackgroundColor(new BaseColor(220, 252, 231));   // emerald-50
        } else if ("B".equalsIgnoreCase(rating)) {
            cell.setBackgroundColor(new BaseColor(219, 234, 254));   // blue-50
        } else if ("C".equalsIgnoreCase(rating)) {
            cell.setBackgroundColor(COLOR_ALT_BG);
        }
        return cell;
    }

    private static void addKvRow(PdfPTable table, String label, String value) {
        Font labelFont = font(10, BaseFont.NOT_EMBEDDED, COLOR_SUBTEXT, Font.BOLD);
        Font valueFont = font(10, BaseFont.NOT_EMBEDDED, COLOR_TEXT, Font.NORMAL);

        PdfPCell labelCell = new PdfPCell(new Paragraph(label, labelFont));
        labelCell.setBackgroundColor(COLOR_ALT_BG);
        labelCell.setPadding(8);
        labelCell.setBorderColor(COLOR_BORDER);

        PdfPCell valueCell = new PdfPCell(new Paragraph(safe(value), valueFont));
        valueCell.setPadding(8);
        valueCell.setBorderColor(COLOR_BORDER);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    // ==================== 页面元素 ====================

    private static void addTitle(Document doc, String title, boolean landscape) throws DocumentException {
        Font titleFont = font(16, BaseFont.NOT_EMBEDDED, COLOR_TITLE, Font.BOLD);
        Paragraph p = new Paragraph(title, titleFont);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(6);
        doc.add(p);

        // 分隔线
        Paragraph line = new Paragraph();
        line.setSpacingAfter(2);
        doc.add(line);
    }

    private static void addSummaryLine(Document doc, String text) throws DocumentException {
        Font f = font(9, BaseFont.NOT_EMBEDDED, COLOR_SUBTEXT, Font.NORMAL);
        Paragraph p = new Paragraph(text, f);
        p.setAlignment(Element.ALIGN_LEFT);
        p.setSpacingBefore(2);
        doc.add(p);
    }

    // ==================== 字体 ====================

    /**
     * 获取中文 BaseFont：classpath 内嵌字体优先，失败回退系统字体
     */
    private static BaseFont baseFont() {
        if (cachedBaseFont != null) {
            return cachedBaseFont;
        }
        // 1. classpath 内嵌字体（首选，跨环境可靠）—— iText 5 支持从 classpath 读取资源路径
        try {
            cachedBaseFont = BaseFont.createFont(FONT_PATH, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            log.info("PDF 字体加载成功: classpath {}", FONT_PATH);
            return cachedBaseFont;
        } catch (Exception e) {
            log.warn("classpath 字体加载失败，尝试系统字体: {}", e.getMessage());
        }
        // 2. 系统字体回退
        String[] systemFonts = {
                "C:/Windows/Fonts/Deng.ttf",
                "C:/Windows/Fonts/msyh.ttc",
                "C:/Windows/Fonts/simsun.ttc",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/System/Library/Fonts/PingFang.ttc"
        };
        for (String fontPath : systemFonts) {
            try {
                if (new java.io.File(fontPath).exists()) {
                    cachedBaseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
                    log.info("PDF 字体加载成功: system {}", fontPath);
                    return cachedBaseFont;
                }
            } catch (Exception e) {
                log.debug("系统字体加载失败: {} -> {}", fontPath, e.getMessage());
            }
        }
        // 3. 兜底：Helvetica（中文会显示乱码，但保证不崩溃）
        try {
            cachedBaseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            log.error("兜底字体加载失败: {}", e.getMessage());
        }
        return cachedBaseFont;
    }

    private static Font font(float size, boolean embedded, BaseColor color, int style) {
        return new Font(baseFont(), size, style, color);
    }

    // ==================== 辅助 ====================

    private static String safe(String s) {
        return s != null ? s : "";
    }

    private static String fmtScore(Object score) {
        if (score == null) return "0";
        try {
            return String.format("%.0f", ((Number) score).doubleValue());
        } catch (Exception e) {
            return String.valueOf(score);
        }
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 招商推荐清单 PDF 行数据（与 Excel 生成器字段一致）
     */
    public static class PdfCompanyRow {
        private String name;
        private String industry;
        private String region;
        private String nodeName;
        private String diagnosisTag;
        private Object businessFitScore;
        private Object scaleStrengthScore;
        private Object regionalSynergyScore;
        private Object totalScore;
        private String rating;

        public static PdfCompanyRow of(com.industrybrain.domain.model.InvestmentRecommendation r) {
            PdfCompanyRow row = new PdfCompanyRow();
            row.name = r.getEnterpriseName();
            row.industry = r.getIndustry();
            row.region = r.getRegionCode();
            row.nodeName = r.getNodeName();
            row.diagnosisTag = r.getDiagnosisTag();
            row.businessFitScore = r.getBusinessFitScore();
            row.scaleStrengthScore = r.getScaleStrengthScore();
            row.regionalSynergyScore = r.getRegionalSynergyScore();
            row.totalScore = r.getTotalScore();
            row.rating = r.getRating();
            return row;
        }

        public String getName() { return name; }
        public String getIndustry() { return industry; }
        public String getRegion() { return region; }
        public String getNodeName() { return nodeName; }
        public String getDiagnosisTag() { return diagnosisTag; }
        public Object getBusinessFitScore() { return businessFitScore; }
        public Object getScaleStrengthScore() { return scaleStrengthScore; }
        public Object getRegionalSynergyScore() { return regionalSynergyScore; }
        public Object getTotalScore() { return totalScore; }
        public String getRating() { return rating; }
    }
}
