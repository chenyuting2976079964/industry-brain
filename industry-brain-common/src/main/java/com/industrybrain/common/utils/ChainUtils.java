package com.industrybrain.common.utils;

/**
 * 产业链通用工具方法
 * 消除 IndustryChainAppService / DiagnosisAppService / EnterpriseAppService 中的重复代码
 *
 * @author 何锦鹏
 */
public final class ChainUtils {

    private ChainUtils() { /* utility class */ }

    // ---- Color Constants ----
    public static final String COLOR_STRONG    = "#52c41a";
    public static final String COLOR_SUPPLEMENT = "#faad14";
    public static final String COLOR_WEAK      = "#ff4d4f";
    public static final String COLOR_MISSING   = "#bfbfbf";
    public static final String COLOR_UNKNOWN   = "#999999";

    // ---- Diagnosis Tag Labels ----
    public static final String TAG_STRONG    = "强链";
    public static final String TAG_SUPPLEMENT = "补链";
    public static final String TAG_WEAK      = "弱链";
    public static final String TAG_MISSING   = "缺链";
    public static final String TAG_UNKNOWN   = "未知";

    // ---- Layer Labels ----
    public static final String LAYER_UPSTREAM   = "上游";
    public static final String LAYER_MIDSTREAM  = "中游";
    public static final String LAYER_DOWNSTREAM = "下游";
    public static final String LAYER_UNKNOWN    = "未知";

    // ---- Threshold Constants (与 DiagnosisEngine 保持一致) ----
    public static final int HIGH_VALUE_THRESHOLD_STRONG = 200;
    public static final int HIGH_VALUE_THRESHOLD_SUPPLEMENT = 50;
    public static final int ENTERPRISE_THRESHOLD_STRONG = 20;
    public static final int ENTERPRISE_THRESHOLD_SUPPLEMENT = 5;

    /**
     * 将层级码转为中文标签 (v2.1: 'up'/'mid'/'down')
     */
    public static String layerToLabel(String layer) {
        if (layer == null) return LAYER_UNKNOWN;
        switch (layer) {
            case "up":   return LAYER_UPSTREAM;
            case "mid":  return LAYER_MIDSTREAM;
            case "down": return LAYER_DOWNSTREAM;
            default:     return LAYER_UNKNOWN;
        }
    }

    /**
     * 将诊断标签映射为前端颜色
     */
    public static String tagToColor(String tag) {
        if (TAG_STRONG.equals(tag)) return COLOR_STRONG;
        if (TAG_SUPPLEMENT.equals(tag)) return COLOR_SUPPLEMENT;
        if (TAG_WEAK.equals(tag)) return COLOR_WEAK;
        if (TAG_MISSING.equals(tag)) return COLOR_MISSING;
        return COLOR_UNKNOWN;
    }

    /**
     * 安全转换 Object → Long
     */
    public static Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long) return (Long) val;
        if (val instanceof Integer) return ((Integer) val).longValue();
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.valueOf(val.toString());
    }

    /**
     * 安全转换 Object → Integer (0 作为 fallback)
     */
    public static int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Long) return ((Long) val).intValue();
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }
}
