package com.industrybrain.service.cluster;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 标签规则词典（纯静态规则表）
 * 供产业链自动聚类引擎使用：
 *  - GENERIC_TAGS    通用服务标签（不归链，预过滤）
 *  - TAG_CHAIN_NAME  标签 → 规范链名（命名候选）
 *  - TAG_LAYER       标签 → up/mid/down（分层兜底）
 *  - SAME_CHAIN      语义近亲标签对（向量失效时的规则兜底，如 钢铁冶炼↔金属材料）
 *
 * @author 产业大脑平台
 */
public final class TagRuleUtil {

    private TagRuleUtil() { /* utility class */ }

    /** 通用服务标签：不归入任何产业链 */
    public static final Set<String> GENERIC_TAGS = Set.of(
        "人力资源", "劳务/人力资源", "咨询服务", "贸易", "企业服务", "租赁服务", "投资管理", "普惠金融",
        "银行", "证券/基金", "保险", "金融服务", "法律服务", "财会/评估", "教育培训", "广告/会展",
        "广告/传媒", "酒店/公寓", "物业管理", "物业/保洁", "安保服务", "殡葬服务", "旅游/景区", "拍卖",
        "餐饮/酒店", "危险品运输");

    /** 标签 → 规范链名（命名第一优先级） */
    public static final Map<String, String> TAG_CHAIN_NAME = new LinkedHashMap<>();
    static {
        TAG_CHAIN_NAME.put("人工智能", "软件与信息技术");
        TAG_CHAIN_NAME.put("大数据", "软件与信息技术");
        TAG_CHAIN_NAME.put("云计算", "软件与信息技术");
        TAG_CHAIN_NAME.put("软件开发", "软件与信息技术");
        TAG_CHAIN_NAME.put("系统集成", "软件与信息技术");
        TAG_CHAIN_NAME.put("信息安全", "软件与信息技术");
        TAG_CHAIN_NAME.put("区块链", "软件与信息技术");
        TAG_CHAIN_NAME.put("物联网", "软件与信息技术");
        TAG_CHAIN_NAME.put("数字孪生/VR", "软件与信息技术");
        TAG_CHAIN_NAME.put("地理信息/遥感", "软件与信息技术");
        TAG_CHAIN_NAME.put("通信技术", "软件与信息技术");

        TAG_CHAIN_NAME.put("半导体/芯片", "电子电气制造");
        TAG_CHAIN_NAME.put("电子元器件", "电子电气制造");
        TAG_CHAIN_NAME.put("电线电缆", "电子电气制造");
        TAG_CHAIN_NAME.put("电器/数码", "电子电气制造");

        TAG_CHAIN_NAME.put("光伏/太阳能", "汽车及新能源");
        TAG_CHAIN_NAME.put("光热发电", "汽车及新能源");
        TAG_CHAIN_NAME.put("风力发电", "汽车及新能源");
        TAG_CHAIN_NAME.put("储能/氢能", "汽车及新能源");
        TAG_CHAIN_NAME.put("电池材料", "汽车及新能源");
        TAG_CHAIN_NAME.put("新能源汽车", "汽车及新能源");
        TAG_CHAIN_NAME.put("充电/换电", "汽车及新能源");
        TAG_CHAIN_NAME.put("汽车销售/服务", "汽车及新能源");
        TAG_CHAIN_NAME.put("新能源", "能源动力");

        TAG_CHAIN_NAME.put("机器人/自动化", "装备制造");
        TAG_CHAIN_NAME.put("智能制造", "装备制造");
        TAG_CHAIN_NAME.put("机械设备", "装备制造");
        TAG_CHAIN_NAME.put("精密加工", "装备制造");
        TAG_CHAIN_NAME.put("模具制造", "装备制造");
        TAG_CHAIN_NAME.put("3D打印", "装备制造");
        TAG_CHAIN_NAME.put("先进复合材料", "装备制造");
        TAG_CHAIN_NAME.put("无人机", "装备制造");
        TAG_CHAIN_NAME.put("制造业", "装备制造");

        TAG_CHAIN_NAME.put("钢铁冶炼", "金属材料");
        TAG_CHAIN_NAME.put("金属材料", "金属材料");
        TAG_CHAIN_NAME.put("有色金属", "金属材料");
        TAG_CHAIN_NAME.put("稀土/磁性材料", "金属材料");
        TAG_CHAIN_NAME.put("高性能合金", "金属材料");
        TAG_CHAIN_NAME.put("矿产品", "金属材料");
        TAG_CHAIN_NAME.put("矿山工程", "金属材料");

        TAG_CHAIN_NAME.put("化工", "化工新材料");
        TAG_CHAIN_NAME.put("精细化工", "化工新材料");
        TAG_CHAIN_NAME.put("石油/石化", "化工新材料");
        TAG_CHAIN_NAME.put("煤炭", "化工新材料");
        TAG_CHAIN_NAME.put("煤炭/煤化工", "化工新材料");
        TAG_CHAIN_NAME.put("天然气", "化工新材料");
        TAG_CHAIN_NAME.put("塑料制品", "化工新材料");
        TAG_CHAIN_NAME.put("橡胶制品", "化工新材料");
        TAG_CHAIN_NAME.put("涂料", "化工新材料");
        TAG_CHAIN_NAME.put("玻璃", "化工新材料");
        TAG_CHAIN_NAME.put("陶瓷", "化工新材料");
        TAG_CHAIN_NAME.put("再生资源", "化工新材料");

        TAG_CHAIN_NAME.put("农产品", "食品加工");
        TAG_CHAIN_NAME.put("农产品加工", "食品加工");
        TAG_CHAIN_NAME.put("种业/育种", "食品加工");
        TAG_CHAIN_NAME.put("畜牧/水产养殖", "食品加工");
        TAG_CHAIN_NAME.put("食品加工", "食品加工");
        TAG_CHAIN_NAME.put("食品/零售", "食品加工");
        TAG_CHAIN_NAME.put("日用消费品", "食品加工");

        TAG_CHAIN_NAME.put("生物技术", "生物医药");
        TAG_CHAIN_NAME.put("制药", "生物医药");
        TAG_CHAIN_NAME.put("医药制造", "生物医药");
        TAG_CHAIN_NAME.put("医疗器械", "生物医药");
        TAG_CHAIN_NAME.put("医疗服务", "生物医药");
        TAG_CHAIN_NAME.put("健康/养老", "生物医药");

        TAG_CHAIN_NAME.put("环保", "环保水务");
        TAG_CHAIN_NAME.put("固废处理", "环保水务");
        TAG_CHAIN_NAME.put("废气治理", "环保水务");
        TAG_CHAIN_NAME.put("污水处理", "环保水务");
        TAG_CHAIN_NAME.put("水务/水环境", "环保水务");
        TAG_CHAIN_NAME.put("土壤修复", "环保水务");

        TAG_CHAIN_NAME.put("仓储", "物流仓储");
        TAG_CHAIN_NAME.put("冷链物流", "物流仓储");
        TAG_CHAIN_NAME.put("物流/仓储", "物流仓储");
        TAG_CHAIN_NAME.put("多式联运/货代", "物流仓储");
        TAG_CHAIN_NAME.put("港口物流", "物流仓储");
        TAG_CHAIN_NAME.put("港口航道", "物流仓储");
        TAG_CHAIN_NAME.put("快递配送", "物流仓储");

        TAG_CHAIN_NAME.put("电力/供电", "能源动力");
        TAG_CHAIN_NAME.put("电力工程", "能源动力");
        TAG_CHAIN_NAME.put("电力设备", "能源动力");
        TAG_CHAIN_NAME.put("供热", "能源动力");
        TAG_CHAIN_NAME.put("燃气供应", "能源动力");
        TAG_CHAIN_NAME.put("碳中和", "能源动力");

        TAG_CHAIN_NAME.put("建材", "建筑工程");
        TAG_CHAIN_NAME.put("建筑施工/钢结构", "建筑工程");
        TAG_CHAIN_NAME.put("工程设计", "建筑工程");
        TAG_CHAIN_NAME.put("工程咨询", "建筑工程");
        TAG_CHAIN_NAME.put("地基基础", "建筑工程");
        TAG_CHAIN_NAME.put("市政/轨道交通", "建筑工程");
        TAG_CHAIN_NAME.put("公路工程", "建筑工程");
        TAG_CHAIN_NAME.put("水利水电", "建筑工程");
        TAG_CHAIN_NAME.put("消防工程", "建筑工程");
        TAG_CHAIN_NAME.put("机电安装", "建筑工程");
        TAG_CHAIN_NAME.put("测绘", "建筑工程");
        TAG_CHAIN_NAME.put("建筑装饰", "建筑工程");
        TAG_CHAIN_NAME.put("园林古建", "建筑工程");
        TAG_CHAIN_NAME.put("园林绿化", "建筑工程");
        TAG_CHAIN_NAME.put("房地产开发", "建筑工程");
        TAG_CHAIN_NAME.put("家具制造", "建筑工程");
        TAG_CHAIN_NAME.put("印刷包装", "建筑工程");

        TAG_CHAIN_NAME.put("航空航天", "交通装备");
        TAG_CHAIN_NAME.put("船舶/海工", "交通装备");

        TAG_CHAIN_NAME.put("检验检测/认证", "检验检测");
        TAG_CHAIN_NAME.put("技术服务", "检验检测");
    }

    /** 标签 → 上中下游分层（第一优先级，比 IPC 规则优先） */
    public static final Map<String, String> TAG_LAYER = new LinkedHashMap<>();
    static {
        // 软件与信息技术：基础平台 up / 开发集成 mid / 智能应用 down
        TAG_LAYER.put("云计算", "up");
        TAG_LAYER.put("大数据", "up");
        TAG_LAYER.put("地理信息/遥感", "up");
        TAG_LAYER.put("软件开发", "mid");
        TAG_LAYER.put("系统集成", "mid");
        TAG_LAYER.put("信息安全", "mid");
        TAG_LAYER.put("区块链", "mid");
        TAG_LAYER.put("人工智能", "down");
        TAG_LAYER.put("物联网", "down");
        TAG_LAYER.put("数字孪生/VR", "down");
        TAG_LAYER.put("通信技术", "down");

        // 电子电气：元件 up / 制造 mid
        TAG_LAYER.put("电子元器件", "up");
        TAG_LAYER.put("半导体/芯片", "up");
        TAG_LAYER.put("电线电缆", "mid");
        TAG_LAYER.put("电器/数码", "mid");

        // 汽车及新能源：能源材料 up / 整车制造 mid / 销售 down
        TAG_LAYER.put("光伏/太阳能", "up");
        TAG_LAYER.put("光热发电", "up");
        TAG_LAYER.put("风力发电", "up");
        TAG_LAYER.put("电池材料", "up");
        TAG_LAYER.put("储能/氢能", "up");
        TAG_LAYER.put("新能源汽车", "mid");
        TAG_LAYER.put("充电/换电", "mid");
        TAG_LAYER.put("汽车销售/服务", "down");

        // 装备制造：材料成型 up / 机械制造 mid / 整机 down
        TAG_LAYER.put("先进复合材料", "up");
        TAG_LAYER.put("精密加工", "up");
        TAG_LAYER.put("机械设备", "mid");
        TAG_LAYER.put("机器人/自动化", "mid");
        TAG_LAYER.put("智能制造", "mid");
        TAG_LAYER.put("模具制造", "mid");
        TAG_LAYER.put("3D打印", "down");
        TAG_LAYER.put("无人机", "down");
        TAG_LAYER.put("制造业", "down");

        // 金属材料：冶炼 up / 加工 mid / 再生 down
        TAG_LAYER.put("钢铁冶炼", "up");
        TAG_LAYER.put("金属材料", "up");
        TAG_LAYER.put("有色金属", "up");
        TAG_LAYER.put("稀土/磁性材料", "up");
        TAG_LAYER.put("高性能合金", "up");
        TAG_LAYER.put("矿产品", "up");
        TAG_LAYER.put("矿山工程", "mid");

        // 化工新材料：基础化工 up / 化工产品 mid / 应用 down
        TAG_LAYER.put("化工", "up");
        TAG_LAYER.put("精细化工", "up");
        TAG_LAYER.put("石油/石化", "up");
        TAG_LAYER.put("煤炭", "up");
        TAG_LAYER.put("煤炭/煤化工", "up");
        TAG_LAYER.put("天然气", "up");
        TAG_LAYER.put("塑料制品", "mid");
        TAG_LAYER.put("橡胶制品", "mid");
        TAG_LAYER.put("涂料", "mid");
        TAG_LAYER.put("玻璃", "mid");
        TAG_LAYER.put("陶瓷", "mid");
        TAG_LAYER.put("再生资源", "down");

        // 食品加工：农产品 up / 加工 mid / 零售 down
        TAG_LAYER.put("农产品", "up");
        TAG_LAYER.put("种业/育种", "up");
        TAG_LAYER.put("畜牧/水产养殖", "up");
        TAG_LAYER.put("农产品加工", "mid");
        TAG_LAYER.put("食品加工", "mid");
        TAG_LAYER.put("食品/零售", "down");
        TAG_LAYER.put("日用消费品", "down");

        // 生物医药：研发 up / 制药 mid / 医疗 down
        TAG_LAYER.put("生物技术", "up");
        TAG_LAYER.put("制药", "mid");
        TAG_LAYER.put("医药制造", "mid");
        TAG_LAYER.put("医疗器械", "down");
        TAG_LAYER.put("医疗服务", "down");
        TAG_LAYER.put("健康/养老", "down");

        // 环保水务：环保材料 up / 处理工艺 mid / 服务 down
        TAG_LAYER.put("环保", "up");
        TAG_LAYER.put("土壤修复", "up");
        TAG_LAYER.put("污水处理", "mid");
        TAG_LAYER.put("水务/水环境", "mid");
        TAG_LAYER.put("废气治理", "mid");
        TAG_LAYER.put("固废处理", "mid");

        // 物流仓储：仓储 up / 运输 mid / 配送 down
        TAG_LAYER.put("仓储", "up");
        TAG_LAYER.put("港口航道", "up");
        TAG_LAYER.put("物流/仓储", "mid");
        TAG_LAYER.put("多式联运/货代", "mid");
        TAG_LAYER.put("港口物流", "mid");
        TAG_LAYER.put("冷链物流", "mid");
        TAG_LAYER.put("快递配送", "down");

        // 能源动力：能源开采 up / 电力生产 mid / 供应 down
        TAG_LAYER.put("天然气", "up");
        TAG_LAYER.put("电力/供电", "mid");
        TAG_LAYER.put("电力工程", "mid");
        TAG_LAYER.put("电力设备", "mid");
        TAG_LAYER.put("供热", "mid");
        TAG_LAYER.put("燃气供应", "down");
        TAG_LAYER.put("碳中和", "down");

        // 建筑工程：材料 up / 施工设计 mid / 装饰服务 down
        TAG_LAYER.put("建材", "up");
        TAG_LAYER.put("建筑施工/钢结构", "mid");
        TAG_LAYER.put("工程设计", "mid");
        TAG_LAYER.put("工程咨询", "mid");
        TAG_LAYER.put("地基基础", "mid");
        TAG_LAYER.put("市政/轨道交通", "mid");
        TAG_LAYER.put("公路工程", "mid");
        TAG_LAYER.put("水利水电", "mid");
        TAG_LAYER.put("消防工程", "mid");
        TAG_LAYER.put("机电安装", "mid");
        TAG_LAYER.put("测绘", "mid");
        TAG_LAYER.put("印刷包装", "mid");
        TAG_LAYER.put("建筑装饰", "down");
        TAG_LAYER.put("园林古建", "down");
        TAG_LAYER.put("园林绿化", "down");
        TAG_LAYER.put("房地产开发", "down");
        TAG_LAYER.put("家具制造", "down");

        // 交通装备
        TAG_LAYER.put("航空航天", "up");
        TAG_LAYER.put("船舶/海工", "up");

        // 检验检测
        TAG_LAYER.put("检验检测/认证", "down");
        TAG_LAYER.put("技术服务", "down");
    }

    /** 语义近亲标签对（必须同链；向量失效时的规则兜底） */
    public static final Set<String> SAME_CHAIN_PAIRS = buildSameChainPairs();

    private static Set<String> buildSameChainPairs() {
        Set<String> pairs = new HashSet<>();
        String[][] groups = {
            // 钢铁/金属/合金
            {"钢铁冶炼", "金属材料", "有色金属", "高性能合金", "稀土/磁性材料"},
            // 化工/精细化工/石油
            {"化工", "精细化工", "石油/石化", "煤炭", "煤炭/煤化工", "天然气"},
            // 材料制品
            {"塑料制品", "橡胶制品", "涂料", "玻璃", "陶瓷", "先进复合材料"},
            // 电子
            {"电子元器件", "半导体/芯片", "电线电缆", "电器/数码"},
            // AI/软件
            {"人工智能", "大数据", "云计算", "物联网", "区块链", "软件开发", "系统集成", "信息安全", "数字孪生/VR"},
            // 新能源
            {"光伏/太阳能", "光热发电", "风力发电", "储能/氢能", "电池材料", "新能源汽车", "充电/换电", "碳中和"},
            // 装备
            {"机器人/自动化", "智能制造", "机械设备", "精密加工", "模具制造", "3D打印", "无人机", "制造业"},
            // 环保
            {"环保", "固废处理", "废气治理", "污水处理", "水务/水环境", "土壤修复"},
            // 食品
            {"农产品", "农产品加工", "种业/育种", "畜牧/水产养殖", "食品加工", "食品/零售", "日用消费品"},
            // 生物医药
            {"生物技术", "制药", "医药制造", "医疗器械", "医疗服务", "健康/养老"},
            // 物流
            {"仓储", "物流/仓储", "多式联运/货代", "港口物流", "港口航道", "冷链物流", "快递配送"},
            // 能源
            {"电力/供电", "电力工程", "电力设备", "供热", "燃气供应"},
            // 建筑
            {"建材", "建筑施工/钢结构", "工程设计", "工程咨询", "地基基础", "市政/轨道交通", "公路工程",
             "水利水电", "消防工程", "机电安装", "测绘", "建筑装饰", "园林古建", "园林绿化", "房地产开发",
             "家具制造", "印刷包装"},
            // 交通
            {"航空航天", "船舶/海工", "无人机"},
        };
        for (String[] group : groups) {
            for (int i = 0; i < group.length; i++) {
                for (int j = i + 1; j < group.length; j++) {
                    pairs.add(key(group[i], group[j]));
                }
            }
        }
        return pairs;
    }

    /** 语义近亲标签对 key（双向同 key） */
    private static final String PAIR_SEP = "::";

    public static String key(String a, String b) {
        return a.compareTo(b) <= 0 ? a + PAIR_SEP + b : b + PAIR_SEP + a;
    }

    /** 两标签是否命中语义近亲词典 */
    public static boolean isSameChain(String a, String b) {
        return SAME_CHAIN_PAIRS.contains(key(a, b));
    }

    /** 查询标签的分层词典值（无则 null） */
    public static String layerOf(String tag) {
        return TAG_LAYER.get(tag);
    }

    /** 查询标签的规范链名（无则 null） */
    public static String chainNameOf(String tag) {
        return TAG_CHAIN_NAME.get(tag);
    }

    /** 是否为通用服务标签（不归链） */
    public static boolean isGeneric(String tag) {
        return GENERIC_TAGS.contains(tag);
    }
}
