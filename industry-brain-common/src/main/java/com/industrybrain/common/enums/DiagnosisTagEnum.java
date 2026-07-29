package com.industrybrain.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 产业链强弱诊断标签枚举
 * A2算法输出: 四档分类 (绿/黄/红/灰)
 */
@Getter
@AllArgsConstructor
public enum DiagnosisTagEnum {
    STRONG("strong", "强链", "#52c41a", "高价值占比和专利密度均高于全国平均水平"),
    SUPPLEMENT("supplement", "补链", "#faad14", "高价值占比达到全国均值60%以上，但仍有提升空间"),
    WEAK("weak", "弱链", "#ff4d4f", "高价值占比低于全国均值60%，需要重点补强"),
    MISSING("missing", "缺链", "#bfbfbf", "该节点无覆盖企业，处于完全缺失状态");

    private final String code;
    private final String label;
    private final String color;
    private final String description;

    public static DiagnosisTagEnum fromCode(String code) {
        for (DiagnosisTagEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) return e;
        }
        return null;
    }
}
