package com.industrybrain.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 企业类型枚举
 */
@Getter
@AllArgsConstructor
public enum EnterpriseTypeEnum {
    LEADER("LEADER", "龙头企业", "行业领军企业，具有强大市场影响力和技术实力"),
    SPECIALIZED("SPECIALIZED", "专精特新", "专业化、精细化、特色化、新颖化的中小企业"),
    HIGH_TECH("HIGH_TECH", "高新技术企业", "获得高新技术企业认定的科技型企业"),
    TECH_SMALL("TECH_SMALL", "科技型中小企业", "登记入库的科技型中小企业"),
    OTHER("OTHER", "其他", "未归类的企业");

    private final String code;
    private final String label;
    private final String description;

    /**
     * 根据code获取枚举实例
     */
    public static EnterpriseTypeEnum fromCode(String code) {
        if (code == null) return null;
        for (EnterpriseTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 根据label获取枚举实例（宽松匹配）
     */
    public static EnterpriseTypeEnum fromLabel(String label) {
        if (label == null) return null;
        for (EnterpriseTypeEnum e : values()) {
            if (e.label.equals(label)) {
                return e;
            }
        }
        // 尝试从description匹配
        for (EnterpriseTypeEnum e : values()) {
            if (e.description != null && e.description.contains(label)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 验证是否为合法的企业类型
     */
    public static boolean isValid(String typeCode) {
        if (typeCode == null) return false;
        return Arrays.stream(values()).anyMatch(e -> e.code.equals(typeCode));
    }

    /**
     * 获取所有可用的类型列表（仅code和label）
     */
    public static List<Map<String, Object>> getOptions() {
        return Arrays.stream(values()).map(e -> {
            Map<String, Object> map = new HashMap<>();
            map.put("code", e.code);
            map.put("label", e.label);
            map.put("description", e.description);
            return map;
        }).collect(Collectors.toList());
    }
}
