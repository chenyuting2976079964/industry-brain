package com.industrybrain.service.impl;

import java.util.Map;
import java.util.HashMap;

/**
 * IPC 分类号 → 技术领域中文名称映射
 * 覆盖常见钢铁冶金、新材料、装备制造等领域的 IPC 分类
 */
final class IpcNameMapper {

    private static final Map<String, String> MAP = new HashMap<>();

    static {
        // ── 钢铁冶金 ──
        MAP.put("C22C38/02", "铁基合金（特种钢）");
        MAP.put("C22C38/04", "锰系合金钢");
        MAP.put("C22C38/06", "含铅易切削钢");
        MAP.put("C22C38/08", "含镍合金钢");
        MAP.put("C22C38/12", "含钨合金钢");
        MAP.put("C22C38/14", "含钛合金钢");
        MAP.put("C22C38/16", "含铜合金钢");
        MAP.put("C22C38/18", "含铬合金钢");
        MAP.put("C22C38/20", "含铜铬合金钢");
        MAP.put("C22C38/22", "铬钼合金钢");
        MAP.put("C22C38/24", "含钒合金钢");
        MAP.put("C22C38/26", "含铌合金钢");
        MAP.put("C22C38/28", "含钛钒合金钢");
        MAP.put("C22C38/30", "含钴合金钢");
        MAP.put("C22C38/32", "含硼合金钢");
        MAP.put("C22C38/34", "含硅合金钢");
        MAP.put("C22C38/36", "含硫易切削钢");
        MAP.put("C22C38/38", "含磷合金钢");
        MAP.put("C22C38/40", "含镍铬合金钢");
        MAP.put("C22C38/42", "含锌合金钢");
        MAP.put("C22C38/44", "含钼钨合金钢");
        MAP.put("C22C38/46", "含钒钛合金钢");
        MAP.put("C22C38/48", "含铌钒合金钢");
        MAP.put("C22C38/50", "含钛硼合金钢");
        MAP.put("C22C38/52", "含钴镍合金钢");
        MAP.put("C22C38/54", "含铌硼合金钢");
        MAP.put("C22C38/56", "含铌钼合金钢");
        MAP.put("C22C38/58", "含钒氮合金钢");
        MAP.put("C22C38/60", "含铅铋易切削钢");
        MAP.put("C21C5/28", "转炉炼钢工艺");
        MAP.put("C21C5/30", "转炉吹炼控制");
        MAP.put("C21C7/10", "钢水真空精炼");
        MAP.put("C21C7/00", "钢水精炼处理");
        MAP.put("C22C33/04", "电炉炼钢工艺");
        MAP.put("C22C33/00", "铁基合金制造");
        MAP.put("C21D8/02", "轧制热处理工艺");
        MAP.put("C21D8/00", "金属轧制热处理");
        MAP.put("C21D9/00", "金属制品热处理");
        MAP.put("B22D11/00", "金属连铸工艺");
        MAP.put("B22D11/10", "连铸钢坯生产");
        MAP.put("B22D41/00", "钢水浇注设备");
        MAP.put("B21B1/46", "薄板坯连铸连轧");
        MAP.put("B21B1/00", "金属轧制工艺");
        MAP.put("B21B37/00", "轧机控制方法");
        MAP.put("B21B45/02", "轧制表面处理");

        // ── 表面处理与涂镀 ──
        MAP.put("C23C2/06", "热浸镀锌铝工艺");
        MAP.put("C23C2/02", "热浸镀预处理");
        MAP.put("C23C2/04", "热浸镀铝工艺");
        MAP.put("C23C2/08", "热浸镀铅工艺");
        MAP.put("C23C2/12", "热浸镀铝锌工艺");
        MAP.put("C23C2/14", "热浸镀合金工艺");
        MAP.put("C23C2/26", "热浸镀后处理");
        MAP.put("C23C2/28", "热浸镀层退火");
        MAP.put("C23C2/30", "热浸镀层合金化");
        MAP.put("C23C2/34", "连续热浸镀工艺");
        MAP.put("C23C2/36", "热浸镀层厚度控制");
        MAP.put("C23C2/38", "热浸镀丝材工艺");
        MAP.put("C23C2/40", "热浸镀板材工艺");
        MAP.put("C25D3/22", "电镀锌工艺");
        MAP.put("C25D3/00", "金属电镀工艺");
        MAP.put("C25D5/00", "电镀表面处理");
        MAP.put("C23C22/00", "化学转化膜处理");
        MAP.put("C23C18/00", "化学镀工艺");

        // ── 机械加工与制造 ──
        MAP.put("B23K9/00", "电弧焊接工艺");
        MAP.put("B23K9/16", "气体保护焊");
        MAP.put("B23K9/18", "埋弧焊接工艺");
        MAP.put("B23K31/00", "金属焊接方法");
        MAP.put("B23K35/00", "焊接材料制备");
        MAP.put("B23K35/30", "焊接用合金材料");
        MAP.put("B23K37/00", "焊接辅助装置");
        MAP.put("B23P15/00", "金属零件制造");
        MAP.put("B23P6/00", "金属修复工艺");
        MAP.put("B23Q3/00", "工件夹持装置");
        MAP.put("B23Q11/00", "机床安全防护");
        MAP.put("B21D22/00", "金属板冲压成型");
        MAP.put("B21D28/00", "金属板冲裁工艺");
        MAP.put("B21C37/00", "管材成型工艺");

        // ── 新材料与检测 ──
        MAP.put("G01N3/00", "材料力学性能检测");
        MAP.put("G01N3/08", "材料拉伸强度测试");
        MAP.put("G01N3/40", "材料硬度测试");
        MAP.put("G01N3/56", "材料耐磨性测试");
        MAP.put("G01N21/00", "光学材料分析检测");
        MAP.put("G01N23/00", "X射线材料分析");
        MAP.put("G01N33/20", "金属材料成分分析");
        MAP.put("G01N33/00", "材料理化性能检测");
        MAP.put("G01N1/28", "材料试样制备");
        MAP.put("G01N1/00", "样品取样方法");
        MAP.put("G01B11/00", "光学精密测量");
        MAP.put("G01B21/00", "尺寸精度测量");
        MAP.put("C22B9/00", "金属精炼提纯");
        MAP.put("C22B7/00", "金属废料回收");
        MAP.put("C22B1/00", "矿石预处理工艺");
        MAP.put("C04B35/00", "陶瓷材料制备");
        MAP.put("C04B28/00", "无机胶凝材料");
        MAP.put("C04B18/00", "废渣建材利用");

        // ── 环保与能源 ──
        MAP.put("B01D53/00", "工业废气处理");
        MAP.put("B01D53/14", "废气吸收净化");
        MAP.put("B01D53/18", "废气洗涤处理");
        MAP.put("B01D53/26", "废气干燥脱水");
        MAP.put("B01D53/34", "含硫废气处理");
        MAP.put("B01D53/50", "烟气脱硫工艺");
        MAP.put("B01D53/56", "氮氧化物脱除");
        MAP.put("B01D53/62", "二氧化碳捕集");
        MAP.put("B01D53/78", "湿法废气净化");
        MAP.put("B01D46/02", "布袋除尘工艺");
        MAP.put("B01D46/00", "工业除尘过滤");
        MAP.put("C02F1/00", "工业废水处理");
        MAP.put("C02F1/52", "废水絮凝沉淀");
        MAP.put("C02F9/00", "废水多级处理");
        MAP.put("B09B3/00", "固体废物处理");
        MAP.put("F27D17/00", "工业炉余热回收");
        MAP.put("F23G7/00", "工业废弃物焚烧");
        MAP.put("C21B13/00", "直接还原炼铁");
        MAP.put("C21B11/00", "非高炉炼铁");
        MAP.put("F27B14/00", "坩埚熔炼炉");
        MAP.put("F27D1/00", "工业炉衬耐火材料");
        MAP.put("F27D3/00", "工业炉加料装置");

        // ── 汽车与零部件 ──
        MAP.put("B60L50/00", "电动汽车动力系统");
        MAP.put("B60L53/00", "电动汽车充电技术");
        MAP.put("B60L58/00", "电池管理系统");
        MAP.put("B60K1/00", "电动车辆驱动装置");
        MAP.put("B60K6/00", "混合动力系统");
        MAP.put("B60W20/00", "混合动力控制策略");
        MAP.put("B62D21/00", "汽车底盘结构");
        MAP.put("B62D25/00", "车身结构件");
        MAP.put("B60R16/00", "汽车电气系统");
        MAP.put("F16H57/00", "变速箱传动装置");
        MAP.put("F02B37/00", "涡轮增压技术");

        // ── 电子信息 ──
        MAP.put("H01L21/00", "半导体制造工艺");
        MAP.put("H01L23/00", "半导体封装技术");
        MAP.put("H01L29/00", "半导体器件结构");
        MAP.put("H05K1/00", "印刷电路板设计");
        MAP.put("H05K3/00", "电路板制造工艺");
        MAP.put("G06F16/00", "数据检索与分析");
        MAP.put("G06F17/00", "数字计算与处理");
        MAP.put("G06Q10/00", "企业管理系统");
        MAP.put("G06Q50/04", "工业制造信息化");
        MAP.put("G06Q50/06", "能源管理信息化");

        // ── 通用 ──
        MAP.put("B65G15/00", "带式输送设备");
        MAP.put("B65G47/00", "物料输送分拣");
        MAP.put("B66C13/00", "起重机控制装置");
        MAP.put("B66C19/00", "港口起重机");
        MAP.put("B66C23/00", "塔式起重设备");
        MAP.put("F16K31/00", "阀门驱动装置");
        MAP.put("F16L23/00", "管道法兰连接");
        MAP.put("F16L55/00", "管道维护技术");
        MAP.put("F04D29/00", "泵与风机部件");
        MAP.put("F15B13/00", "液压控制阀组");
        MAP.put("E04B1/00", "建筑结构设计");
        MAP.put("E04G21/00", "建筑施工方法");
    }

    static String toName(String ipcCode) {
        if (ipcCode == null || ipcCode.trim().isEmpty()) return "未知领域";
        String name = MAP.get(ipcCode.trim());
        if (name != null) return name;
        // 模糊匹配：取前4位大类
        if (ipcCode.length() >= 4) {
            String section = ipcCode.substring(0, 4);
            for (Map.Entry<String, String> e : MAP.entrySet()) {
                if (e.getKey().startsWith(section)) return e.getValue();
            }
        }
        // 取部类中文名
        if (ipcCode.length() > 0) {
            char c = ipcCode.charAt(0);
            switch (c) {
                case 'B': return "机械加工设备";
                case 'C': return "化学冶金技术";
                case 'F': return "机械工程技术";
                case 'G': return "物理测量技术";
                case 'H': return "电子信息工程";
                case 'E': return "建筑工程技术";
                case 'A': return "生活用品技术";
                case 'D': return "纺织造纸技术";
            }
        }
        return "其他技术领域";
    }
}
