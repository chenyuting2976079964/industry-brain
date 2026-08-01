package com.industrybrain.common.utils;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 高价值专利判定工具（国知局标准）
 *
 * 认定标准（国家知识产权局高价值发明专利）：
 *  ① 战略性新兴产业的发明专利：patent_type='发明' 且 IPC 主分类属于九大战略性新兴产业
 *  ③ 维持年限超过5年的发明专利：patent_type='发明' 且 maintenance_years >= 5
 *
 * 九大战略性新兴产业：新一代信息技术、高端装备制造、新材料、生物、
 * 新能源汽车、新能源、节能环保、航空航天、海洋装备。
 *
 * 该映射为全项目唯一数据源，SQL 与 Java 两侧共用，避免口径漂移。
 *
 * @author 何锦鹏
 */
public final class HighValuePatentUtils {

    private HighValuePatentUtils() { /* utility class */ }

    /** 高价值专利类型：发明专利 */
    public static final String PATENT_TYPE_INVENTION = "发明";

    /** 维持年限阈值（年） */
    public static final int MAINTENANCE_YEARS_THRESHOLD = 5;

    /** 九大战略性新兴产业 IPC class 前缀（3位） */
    public static final Set<String> STRATEGIC_INDUSTRY_IPC = Set.of(
        // 新一代信息技术
        "G06", "H04", "H01", "G16", "G05", "G01",
        // 高端装备制造
        "B23", "B24", "B25", "B26", "B65", "B66",
        "F01", "F02", "F03", "F04", "F15", "F16",
        // 新材料
        "C01", "C03", "C04", "C08", "C09", "C22", "C23", "C30", "C07", "C10",
        // 生物
        "A61", "C12",
        // 新能源汽车
        "B60", "B62",
        // 新能源
        "H02",
        // 节能环保
        "B01", "B02", "B03", "B09", "C02",
        // 航空航天
        "B64",
        // 海洋装备
        "B63"
    );

    /** 供 SQL IN 子句拼接的 IPC 前缀列表（由 Set 单一来源生成，如 'B01','B02',...） */
    public static final String IPC_SQL_IN = STRATEGIC_INDUSTRY_IPC.stream()
            .sorted()
            .map(c -> "'" + c + "'")
            .collect(Collectors.joining(","));

    /**
     * Java 侧判断是否为高价值专利（国知局标准）：
     * 必须是发明专利，且满足 维持年限≥5 或 IPC 属于战略新兴产业。
     *
     * @param patentType       专利类型
     * @param maintenanceYears 维持年限
     * @param ipcMain          IPC 主分类号
     * @return 是否高价值专利
     */
    public static boolean isHighValue(String patentType, Integer maintenanceYears, String ipcMain) {
        if (!PATENT_TYPE_INVENTION.equals(patentType)) return false;
        if (maintenanceYears != null && maintenanceYears >= MAINTENANCE_YEARS_THRESHOLD) return true;
        return ipcMain != null && ipcMain.length() >= 3
                && STRATEGIC_INDUSTRY_IPC.contains(ipcMain.substring(0, 3));
    }
}
