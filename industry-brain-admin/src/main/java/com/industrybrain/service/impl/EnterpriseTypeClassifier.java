package com.industrybrain.service.impl;

import com.industrybrain.domain.model.Enterprise;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 企业类型自动分类器
 * 根据《企业类型标准》按优先级判定：龙头企业 > 高新技术企业 > 科技型中小企业 > 普通企业
 *
 * @author chentao
 */
@Slf4j
@Component
public class EnterpriseTypeClassifier {

    // ─── 龙头企业名称关键词 ───
    private static final List<String> LEADING_NAME_KEYWORDS = Arrays.asList(
            "集团总公司", "集团股份", "集团有限", "控股集团",
            "实业集团", "产业集团", "建设集团", "发展集团", "集团"
    );

    // ─── 高新技术企业名称关键词 ───
    private static final List<String> HIGH_TECH_NAME_KEYWORDS = Arrays.asList(
            "人工智能科技", "半导体科技", "生物医药科技", "智能科技", "信息技术",
            "生物科技", "光电科技", "材料科技", "新能源科技", "电子科技",
            "网络科技", "信息科技", "数码科技", "自动化科技", "通信科技",
            "环保科技", "科创", "科技"
    );

    // ─── 高新技术企业经营范围关键词 ───
    private static final List<String> HIGH_TECH_DESC_KEYWORDS = Arrays.asList(
            "新兴能源技术研发", "发电技术服务", "风电场研发", "太阳能技术服务",
            "电子专用材料研发", "资源再生利用技术研发", "动力电池回收", "合同能源管理",
            "锂电池研发", "智能控制系统", "工业互联网", "5G通信",
            "机器视觉", "虚拟现实", "增强现实", "芯片设计", "半导体封装",
            "基因测序", "体外诊断", "工业机器人", "数控机床", "精密仪器",
            "废水处理", "废气治理", "固废回收", "碳减排", "超导材料",
            "特种陶瓷", "碳纤维", "卫星应用", "无人机", "雷达技术",
            "光通信", "传感器", "嵌入式开发", "大数据", "云计算",
            "物联网", "区块链", "新能源", "新材料", "人工智能",
            "软件开发", "集成电路", "生物医药", "节能环保", "高端装备",
            "量子技术", "航空航天", "光伏", "风电", "储能",
            "生物质能", "氢能", "光热发电"
    );

    // ─── 阈值 ───
    private static final long LEADING_CAPITAL_MIN = 100_000_000L;   // 1亿元
    private static final long HIGH_TECH_CAPITAL_MIN = 10_000_000L;  // 1000万元
    private static final long ESTABLISHMENT_DAYS_MIN = 1095;        // 3年

    private static final Pattern NUMBER_PATTERN = Pattern.compile("[\\d.]+");

    /**
     * 判定企业类型，返回 DB CHECK 约束允许的值
     */
    public String classify(Enterprise e) {
        if (e == null) return "普通企业";

        // 1. 龙头企业
        if (isLeading(e)) return "龙头企业";

        // 2. 高新技术企业
        if (isHighTech(e)) return "高新技术企业";

        // 3. 科技型中小企业
        if (isTechSME(e)) return "科技型中小企业";

        // 4. 兜底
        return "普通企业";
    }

    // ─────────────────── 判定方法 ───────────────────

    /** 龙头企业：A(名称命中) 或 B(注册资本≥1亿 且 成立≥3年) */
    private boolean isLeading(Enterprise e) {
        // 条件A：名称关键词
        if (containsAny(e.getName(), LEADING_NAME_KEYWORDS)) return true;

        // 条件B：双条件同时满足
        return parseCapital(e.getRegisteredCapital()) >= LEADING_CAPITAL_MIN
                && isEstablishedOver3Years(e.getEstablishmentDate());
    }

    /** 高新技术企业：(名称或描述命中) 且 注册资本≥1000万 */
    private boolean isHighTech(Enterprise e) {
        boolean scopeMatch = containsAny(e.getName(), HIGH_TECH_NAME_KEYWORDS)
                || containsAny(e.getDescription(), HIGH_TECH_DESC_KEYWORDS);
        return scopeMatch && parseCapital(e.getRegisteredCapital()) >= HIGH_TECH_CAPITAL_MIN;
    }

    /** 科技型中小企业：名称或描述命中高新关键词 */
    private boolean isTechSME(Enterprise e) {
        return containsAny(e.getName(), HIGH_TECH_NAME_KEYWORDS)
                || containsAny(e.getDescription(), HIGH_TECH_DESC_KEYWORDS);
    }

    // ─────────────────── 工具方法 ───────────────────

    /** 模糊包含匹配，不区分大小写 */
    private boolean containsAny(String text, List<String> keywords) {
        if (text == null || text.isEmpty()) return false;
        String lower = text.toLowerCase();
        return keywords.stream().anyMatch(kw -> lower.contains(kw.toLowerCase()));
    }

    /** 成立日期距今 ≥ 1095天 */
    private boolean isEstablishedOver3Years(LocalDate date) {
        if (date == null) return false;
        return ChronoUnit.DAYS.between(date, LocalDate.now()) >= ESTABLISHMENT_DAYS_MIN;
    }

    /**
     * 解析注册资本为数值（元）
     * 支持格式：1000万元、1.5亿元、5000000、1000万、1亿、1,000万 等
     */
    long parseCapital(String capitalStr) {
        if (capitalStr == null || capitalStr.isBlank()) return 0;

        String s = capitalStr.replace(",", "").replace("，", "").trim();
        Matcher m = NUMBER_PATTERN.matcher(s);
        if (!m.find()) return 0;

        double num = Double.parseDouble(m.group());
        if (s.contains("亿")) num *= 100_000_000;
        else if (s.contains("万")) num *= 10_000;

        return (long) num;
    }
}
