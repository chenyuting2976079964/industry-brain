package com.industrybrain.service.impl;

import com.industrybrain.domain.model.*;
import com.industrybrain.mapper.mysql.*;
import com.industrybrain.service.IPatentMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 产业链全景图谱自动聚合构建引擎
 *
 * 核心：以专利 IPC 分类为聚合维度，通过「企业共现」社区发现自动形成产业链，
 * 不依赖预定义的产业链清单。流程：
 *   1. IPC subclass 级统计 + 共现矩阵 → 社区发现 → 自动生成 industry_chain
 *   2. 每个 IPC subclass → 一个 chain_node（归属自动生成的链）
 *   3. 有专利企业 → 通过专利归属到 IPC 节点；有标签企业 → 标签补充归属
 *   4. A1 算法专利映射、节点诊断聚合、IPC 规则填充、Neo4j 同步
 *
 * 启动时若 industry_chain 为空或 chain_node 为空则自动构建一次。
 *
 * @author 产业大脑平台
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphBuildService implements ApplicationRunner {

    private final ChainInfoMapper chainInfoMapper;
    private final ChainNodeMapper chainNodeMapper;
    private final ChainNodeLinkMapper chainNodeLinkMapper;
    private final EnterpriseTagMapper enterpriseTagMapper;
    private final EnterpriseNodeMapper enterpriseNodeMapper;
    private final PatentNodeMapper patentNodeMapper;
    private final NodeDiagnosisMapper nodeDiagnosisMapper;
    private final PatentMapper patentMapper;
    private final IpcChainMappingMapper ipcChainMappingMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final IPatentMappingService patentMappingService;
    private final org.springframework.beans.factory.ObjectProvider<Neo4jSyncService> neo4jSyncServiceProvider;

    private static final LocalDateTime NOW = LocalDateTime.now();

    /** IPC subclass 共现聚类的最小企业数（过滤碎片） */
    private static final int MIN_IPC_ENT = 2;
    /** 共现企业数阈值（subclass 视为同一链的最小共享企业数） */
    private static final int COOCCUR_MIN = 3;
    /** Jaccard 相似度阈值（subclass 视为同一链） */
    private static final double JACCARD_MIN = 0.15;

    /**
     * IPC class 前缀 → 规范产业链名（命名层映射，非聚类规则）
     * IPC 分类体系：A生活需求/B作业运输/C化学冶金/D纺织/E固定建筑/F机械物理/G物理仪器/H电学
     */
    private static final Map<String, String> IPC_CLASS_CHAIN = new LinkedHashMap<>();
    static {
        // 金属材料与冶金
        IPC_CLASS_CHAIN.put("C22", "金属材料与冶金");
        IPC_CLASS_CHAIN.put("C21", "钢铁冶金");
        IPC_CLASS_CHAIN.put("C23", "金属表面处理");
        IPC_CLASS_CHAIN.put("B21", "金属成型加工");
        IPC_CLASS_CHAIN.put("B22", "铸造技术");
        IPC_CLASS_CHAIN.put("B23", "机床与金属切削");
        IPC_CLASS_CHAIN.put("B24", "磨削加工");
        IPC_CLASS_CHAIN.put("B25", "手工工具");
        IPC_CLASS_CHAIN.put("B26", "切割加工");
        // 装备制造
        IPC_CLASS_CHAIN.put("B65", "物流输送与包装");
        IPC_CLASS_CHAIN.put("B66", "起重运输机械");
        IPC_CLASS_CHAIN.put("B62", "车辆工程");
        IPC_CLASS_CHAIN.put("B60", "车辆通用");
        IPC_CLASS_CHAIN.put("B61", "铁路车辆");
        IPC_CLASS_CHAIN.put("B63", "船舶海洋工程");
        IPC_CLASS_CHAIN.put("B64", "飞行器航空");
        IPC_CLASS_CHAIN.put("B27", "木材加工");
        IPC_CLASS_CHAIN.put("B28", "陶瓷建材成型");
        IPC_CLASS_CHAIN.put("B29", "塑料成型");
        IPC_CLASS_CHAIN.put("B30", "压力机");
        IPC_CLASS_CHAIN.put("B31", "纸品加工");
        // 环保与化工
        IPC_CLASS_CHAIN.put("B01", "化工分离混合");
        IPC_CLASS_CHAIN.put("B02", "破碎筛分");
        IPC_CLASS_CHAIN.put("B03", "固液分离");
        IPC_CLASS_CHAIN.put("B04", "离心分离");
        IPC_CLASS_CHAIN.put("B05", "喷涂雾化");
        IPC_CLASS_CHAIN.put("B07", "分级筛选");
        IPC_CLASS_CHAIN.put("B08", "清洗清洁");
        IPC_CLASS_CHAIN.put("B09", "固体废弃物");
        IPC_CLASS_CHAIN.put("C01", "无机化学");
        IPC_CLASS_CHAIN.put("C02", "水处理");
        IPC_CLASS_CHAIN.put("C03", "玻璃制造");
        IPC_CLASS_CHAIN.put("C04", "水泥陶瓷");
        IPC_CLASS_CHAIN.put("C05", "肥料");
        IPC_CLASS_CHAIN.put("C06", "炸药");
        IPC_CLASS_CHAIN.put("C07", "有机化学");
        IPC_CLASS_CHAIN.put("C08", "高分子材料");
        IPC_CLASS_CHAIN.put("C09", "涂料胶粘剂");
        IPC_CLASS_CHAIN.put("C10", "石油化工");
        IPC_CLASS_CHAIN.put("C11", "油脂工业");
        IPC_CLASS_CHAIN.put("C12", "生物化学");
        IPC_CLASS_CHAIN.put("C13", "糖工业");
        IPC_CLASS_CHAIN.put("C14", "皮革");
        IPC_CLASS_CHAIN.put("C23", "表面处理");
        IPC_CLASS_CHAIN.put("C25", "电解加工");
        IPC_CLASS_CHAIN.put("C30", "晶体生长");
        // 机械通用
        IPC_CLASS_CHAIN.put("F01", "动力机械");
        IPC_CLASS_CHAIN.put("F02", "内燃机");
        IPC_CLASS_CHAIN.put("F03", "流体机械");
        IPC_CLASS_CHAIN.put("F04", "泵");
        IPC_CLASS_CHAIN.put("F15", "液压");
        IPC_CLASS_CHAIN.put("F16", "机械零部件");
        IPC_CLASS_CHAIN.put("F17", "储气储液");
        IPC_CLASS_CHAIN.put("F21", "照明");
        IPC_CLASS_CHAIN.put("F22", "蒸汽发生");
        IPC_CLASS_CHAIN.put("F23", "燃烧设备");
        IPC_CLASS_CHAIN.put("F24", "供热制冷");
        IPC_CLASS_CHAIN.put("F25", "制冷");
        IPC_CLASS_CHAIN.put("F26", "干燥");
        IPC_CLASS_CHAIN.put("F27", "炉窑");
        IPC_CLASS_CHAIN.put("F28", "热交换");
        IPC_CLASS_CHAIN.put("F42", "武器弹道");
        // 测量检测
        IPC_CLASS_CHAIN.put("G01", "测量检测");
        IPC_CLASS_CHAIN.put("G02", "光学");
        IPC_CLASS_CHAIN.put("G03", "摄影");
        IPC_CLASS_CHAIN.put("G04", "钟表");
        IPC_CLASS_CHAIN.put("G05", "控制调节");
        IPC_CLASS_CHAIN.put("G06", "计算与人工智能");
        IPC_CLASS_CHAIN.put("G07", "检验装置");
        IPC_CLASS_CHAIN.put("G08", "信号报警");
        IPC_CLASS_CHAIN.put("G09", "教育显示");
        IPC_CLASS_CHAIN.put("G10", "乐器声学");
        IPC_CLASS_CHAIN.put("G12", "仪器细节");
        IPC_CLASS_CHAIN.put("G16", "信息与数据");
        // 电子电气
        IPC_CLASS_CHAIN.put("H01", "电气元件与电池");
        IPC_CLASS_CHAIN.put("H02", "电力发电输配电");
        IPC_CLASS_CHAIN.put("H03", "电子电路");
        IPC_CLASS_CHAIN.put("H04", "通信网络");
        IPC_CLASS_CHAIN.put("H05", "电气杂项");
        IPC_CLASS_CHAIN.put("H10", "半导体器件");
        // 生活与建筑
        IPC_CLASS_CHAIN.put("A01", "农业园艺");
        IPC_CLASS_CHAIN.put("A21", "烘烤食品");
        IPC_CLASS_CHAIN.put("A22", "屠宰肉类");
        IPC_CLASS_CHAIN.put("A23", "食品加工");
        IPC_CLASS_CHAIN.put("A24", "烟草");
        IPC_CLASS_CHAIN.put("A41", "服装");
        IPC_CLASS_CHAIN.put("A42", "帽类");
        IPC_CLASS_CHAIN.put("A43", "鞋类");
        IPC_CLASS_CHAIN.put("A44", "服饰用品");
        IPC_CLASS_CHAIN.put("A45", "手携物品");
        IPC_CLASS_CHAIN.put("A46", "刷类");
        IPC_CLASS_CHAIN.put("A47", "家具家居");
        IPC_CLASS_CHAIN.put("A61", "医疗器械");
        IPC_CLASS_CHAIN.put("A62", "救生消防");
        IPC_CLASS_CHAIN.put("A63", "运动娱乐");
        IPC_CLASS_CHAIN.put("E01", "道路建筑");
        IPC_CLASS_CHAIN.put("E02", "水利工程");
        IPC_CLASS_CHAIN.put("E03", "给排水");
        IPC_CLASS_CHAIN.put("E04", "房屋建筑");
        IPC_CLASS_CHAIN.put("E05", "门窗锁具");
        IPC_CLASS_CHAIN.put("E06", "门窗");
        IPC_CLASS_CHAIN.put("E21", "矿业开采");
        IPC_CLASS_CHAIN.put("D21", "造纸");
        IPC_CLASS_CHAIN.put("D06", "纺织印染");
        IPC_CLASS_CHAIN.put("D04", "纺织");
        IPC_CLASS_CHAIN.put("D01", "天然纤维");
    }

    /**
     * IPC class 前缀 → 产业链环节角色（up/mid/down）
     * 决定链内节点落在上游/中游/下游哪一列。
     * 原则：上游=原料/材料/化工/矿产，中游=加工/制造/组装/设备/物流，下游=成品/检测/电气/应用
     */
    private static final Map<String, String> IPC_CLASS_LAYER = new LinkedHashMap<>();
    static {
        // ── 上游：原料/材料/化工/矿产/冶金 ──
        IPC_CLASS_LAYER.put("C01", "up");   // 无机化学
        IPC_CLASS_LAYER.put("C21", "up");   // 钢铁冶金
        IPC_CLASS_LAYER.put("C22", "up");   // 金属材料
        IPC_CLASS_LAYER.put("C23", "up");   // 表面处理
        IPC_CLASS_LAYER.put("C25", "up");   // 电解
        IPC_CLASS_LAYER.put("C30", "up");   // 晶体
        IPC_CLASS_LAYER.put("C07", "up");   // 有机化学
        IPC_CLASS_LAYER.put("C08", "up");   // 高分子
        IPC_CLASS_LAYER.put("C09", "up");   // 涂料胶粘
        IPC_CLASS_LAYER.put("C10", "up");   // 石油
        IPC_CLASS_LAYER.put("C11", "up");   // 油脂
        IPC_CLASS_LAYER.put("C12", "up");   // 生物化学
        IPC_CLASS_LAYER.put("C13", "up");   // 糖
        IPC_CLASS_LAYER.put("C14", "up");   // 皮革
        IPC_CLASS_LAYER.put("B22", "up");   // 铸造
        IPC_CLASS_LAYER.put("B28", "up");   // 陶瓷建材
        IPC_CLASS_LAYER.put("B32", "up");   // 复合材料
        IPC_CLASS_LAYER.put("C03", "up");   // 玻璃
        IPC_CLASS_LAYER.put("C04", "up");   // 水泥
        IPC_CLASS_LAYER.put("D01", "up");   // 纤维
        IPC_CLASS_LAYER.put("D04", "up");   // 纺织
        IPC_CLASS_LAYER.put("A01", "up");   // 农业
        IPC_CLASS_LAYER.put("A21", "up");   // 烘烤
        IPC_CLASS_LAYER.put("A22", "up");   // 屠宰
        IPC_CLASS_LAYER.put("A23", "up");   // 食品
        IPC_CLASS_LAYER.put("A24", "up");   // 烟草
        IPC_CLASS_LAYER.put("E21", "up");   // 矿业

        // ── 中游：加工/制造/组装/设备/物流/工艺 ──
        IPC_CLASS_LAYER.put("B21", "mid");  // 成型
        IPC_CLASS_LAYER.put("B23", "mid");  // 机床
        IPC_CLASS_LAYER.put("B24", "mid");  // 磨削
        IPC_CLASS_LAYER.put("B25", "mid");  // 工具
        IPC_CLASS_LAYER.put("B26", "mid");  // 切割
        IPC_CLASS_LAYER.put("B27", "mid");  // 木材
        IPC_CLASS_LAYER.put("B29", "mid");  // 塑料
        IPC_CLASS_LAYER.put("B30", "mid");  // 压力机
        IPC_CLASS_LAYER.put("B31", "mid");  // 纸品
        IPC_CLASS_LAYER.put("B65", "mid");  // 输送包装
        IPC_CLASS_LAYER.put("B66", "mid");  // 起重
        IPC_CLASS_LAYER.put("B67", "mid");  // 灌装
        IPC_CLASS_LAYER.put("B01", "mid");  // 化工分离
        IPC_CLASS_LAYER.put("B02", "mid");  // 破碎
        IPC_CLASS_LAYER.put("B03", "mid");  // 固液
        IPC_CLASS_LAYER.put("B04", "mid");  // 离心
        IPC_CLASS_LAYER.put("B05", "mid");  // 喷涂
        IPC_CLASS_LAYER.put("B07", "mid");  // 分级
        IPC_CLASS_LAYER.put("B08", "mid");  // 清洗
        IPC_CLASS_LAYER.put("B09", "mid");  // 固废
        IPC_CLASS_LAYER.put("F01", "mid");  // 动力机械
        IPC_CLASS_LAYER.put("F02", "mid");  // 内燃机
        IPC_CLASS_LAYER.put("F03", "mid");  // 流体机械
        IPC_CLASS_LAYER.put("F04", "mid");  // 泵
        IPC_CLASS_LAYER.put("F15", "mid");  // 液压
        IPC_CLASS_LAYER.put("F16", "mid");  // 零部件
        IPC_CLASS_LAYER.put("F17", "mid");  // 储气
        IPC_CLASS_LAYER.put("F21", "mid");  // 照明
        IPC_CLASS_LAYER.put("F22", "mid");  // 蒸汽
        IPC_CLASS_LAYER.put("F23", "mid");  // 燃烧
        IPC_CLASS_LAYER.put("F24", "mid");  // 供热
        IPC_CLASS_LAYER.put("F25", "mid");  // 制冷
        IPC_CLASS_LAYER.put("F26", "mid");  // 干燥
        IPC_CLASS_LAYER.put("F27", "mid");  // 炉窑
        IPC_CLASS_LAYER.put("F28", "mid");  // 热交换
        IPC_CLASS_LAYER.put("F42", "mid");  // 武器
        IPC_CLASS_LAYER.put("E01", "mid");  // 道路
        IPC_CLASS_LAYER.put("E02", "mid");  // 水利
        IPC_CLASS_LAYER.put("E03", "mid");  // 给排水
        IPC_CLASS_LAYER.put("E04", "mid");  // 房屋
        IPC_CLASS_LAYER.put("E05", "mid");  // 门窗
        IPC_CLASS_LAYER.put("E06", "mid");  // 门窗
        IPC_CLASS_LAYER.put("D06", "mid");  // 印染
        IPC_CLASS_LAYER.put("D21", "mid");  // 造纸

        // ── 下游：成品/检测/电气/应用 ──
        IPC_CLASS_LAYER.put("G01", "down"); // 测量
        IPC_CLASS_LAYER.put("G02", "down"); // 光学
        IPC_CLASS_LAYER.put("G03", "down"); // 摄影
        IPC_CLASS_LAYER.put("G04", "down"); // 钟表
        IPC_CLASS_LAYER.put("G05", "down"); // 控制
        IPC_CLASS_LAYER.put("G06", "down"); // 计算
        IPC_CLASS_LAYER.put("G07", "down"); // 检验
        IPC_CLASS_LAYER.put("G08", "down"); // 报警
        IPC_CLASS_LAYER.put("G09", "down"); // 显示
        IPC_CLASS_LAYER.put("G10", "down"); // 声学
        IPC_CLASS_LAYER.put("G12", "down"); // 仪器
        IPC_CLASS_LAYER.put("G16", "down"); // 数据
        IPC_CLASS_LAYER.put("H01", "down"); // 电气元件
        IPC_CLASS_LAYER.put("H02", "down"); // 电力
        IPC_CLASS_LAYER.put("H03", "down"); // 电路
        IPC_CLASS_LAYER.put("H04", "down"); // 通信
        IPC_CLASS_LAYER.put("H05", "down"); // 电气杂项
        IPC_CLASS_LAYER.put("H10", "down"); // 半导体
        IPC_CLASS_LAYER.put("A47", "down"); // 家具
        IPC_CLASS_LAYER.put("A61", "down"); // 医疗
        IPC_CLASS_LAYER.put("A62", "down"); // 救生
        IPC_CLASS_LAYER.put("A63", "down"); // 运动
        IPC_CLASS_LAYER.put("B60", "down"); // 车辆
        IPC_CLASS_LAYER.put("B61", "down"); // 铁路
        IPC_CLASS_LAYER.put("B62", "down"); // 车辆
        IPC_CLASS_LAYER.put("B63", "down"); // 船舶
        IPC_CLASS_LAYER.put("B64", "down"); // 航空
        IPC_CLASS_LAYER.put("C02", "down"); // 水处理
        IPC_CLASS_LAYER.put("A41", "down"); // 服装
        IPC_CLASS_LAYER.put("A42", "down"); // 帽
        IPC_CLASS_LAYER.put("A43", "down"); // 鞋
        IPC_CLASS_LAYER.put("A44", "down"); // 服饰
        IPC_CLASS_LAYER.put("A45", "down"); // 手携
        IPC_CLASS_LAYER.put("A46", "down"); // 刷
    }

    /**
     * 产业链 × 上中下游环节 × 领域标签 映射（公司所属领域划分）
     * 每条产业链定义上游/中游/下游环节，每环节映射领域标签（企业标签）。
     * 节点 = 领域标签，层级 = 环节定义。领域内必须要有公司（标签企业数 0 不建节点）。
     */
    private static final Map<String, List<ChainStage>> CHAIN_STAGE_TAGS = new LinkedHashMap<>();
    static {
        // 建筑工程：上游材料 → 中游施工 → 下游装饰服务
        CHAIN_STAGE_TAGS.put("建筑工程", List.of(
            new ChainStage("上游·建筑材料", "up", List.of("建材", "玻璃", "陶瓷", "涂料")),
            new ChainStage("中游·施工设计", "mid", List.of("建筑施工/钢结构", "工程设计", "地基基础", "市政/轨道交通", "公路工程", "机电安装", "消防工程", "水利水电")),
            new ChainStage("下游·装饰服务", "down", List.of("建筑装饰", "园林古建", "园林绿化", "房地产开发", "家具制造", "物业管理"))
        ));
        // 软件与信息技术：上游基础 → 中游开发 → 下游应用
        CHAIN_STAGE_TAGS.put("软件与信息技术", List.of(
            new ChainStage("上游·基础平台", "up", List.of("云计算", "大数据", "地理信息/遥感")),
            new ChainStage("中游·软件研发", "mid", List.of("软件开发", "系统集成", "信息安全", "区块链")),
            new ChainStage("下游·智能应用", "down", List.of("人工智能", "物联网", "数字孪生/VR", "通信技术"))
        ));
        // 电子电气制造：上游元件 → 中游电路 → 下游设备
        CHAIN_STAGE_TAGS.put("电子电气制造", List.of(
            new ChainStage("上游·电子元件", "up", List.of("电子元器件", "半导体/芯片", "电子化学品")),
            new ChainStage("中游·电气制造", "mid", List.of("电线电缆", "电器/数码")),
            new ChainStage("下游·整机与检测", "down", List.of("通信技术", "检验检测/认证"))
        ));
        // 汽车及新能源：上游能源材料 → 中游整车 → 下游销售服务
        CHAIN_STAGE_TAGS.put("汽车及新能源", List.of(
            new ChainStage("上游·能源材料", "up", List.of("光伏/太阳能", "储能/氢能", "电池材料", "风电")),
            new ChainStage("中游·整车制造", "mid", List.of("新能源汽车", "充电/换电", "零部件")),
            new ChainStage("下游·销售服务", "down", List.of("汽车销售/服务", "碳中和"))
        ));
        // 装备制造：上游材料成型 → 中游机械制造 → 下游整机
        CHAIN_STAGE_TAGS.put("装备制造", List.of(
            new ChainStage("上游·材料成型", "up", List.of("先进复合材料", "精密加工")),
            new ChainStage("中游·机械制造", "mid", List.of("机械设备", "机器人/自动化", "智能制造", "模具制造")),
            new ChainStage("下游·整机装备", "down", List.of("3D打印", "制造业", "技术服务"))
        ));
        // 金属材料：上游冶炼 → 中游加工 → 下游应用
        CHAIN_STAGE_TAGS.put("金属材料", List.of(
            new ChainStage("上游·金属冶炼", "up", List.of("钢铁冶炼", "金属材料", "有色金属", "稀土/磁性材料")),
            new ChainStage("中游·材料加工", "mid", List.of("高性能合金", "先进复合材料")),
            new ChainStage("下游·再生利用", "down", List.of("再生资源", "矿产品", "矿山工程"))
        ));
        // 化工新材料：上游基础化工 → 中游化工产品 → 下游应用
        CHAIN_STAGE_TAGS.put("化工新材料", List.of(
            new ChainStage("上游·基础化工", "up", List.of("化工", "精细化工", "石油/石化", "煤炭/煤化工")),
            new ChainStage("中游·化工产品", "mid", List.of("涂料", "塑料制品", "橡胶制品")),
            new ChainStage("下游·化工应用", "down", List.of("煤炭", "再生资源"))
        ));
        // 食品加工：上游农产品 → 中游加工 → 下游零售餐饮
        CHAIN_STAGE_TAGS.put("食品加工", List.of(
            new ChainStage("上游·农产品原料", "up", List.of("农产品", "农产品加工", "畜牧/水产养殖", "种业/育种")),
            new ChainStage("中游·食品加工", "mid", List.of("食品加工", "食品/零售")),
            new ChainStage("下游·零售餐饮", "down", List.of("餐饮/酒店", "日用消费品"))
        ));
        // 生物医药：上游研发 → 中游制药 → 下游医疗服务
        CHAIN_STAGE_TAGS.put("生物医药", List.of(
            new ChainStage("上游·生物研发", "up", List.of("生物技术", "健康/养老")),
            new ChainStage("中游·制药生产", "mid", List.of("制药", "医药制造")),
            new ChainStage("下游·医疗服务", "down", List.of("医疗器械", "医疗服务"))
        ));
        // 环保水务：上游环保材料 → 中游处理工艺 → 下游监测
        CHAIN_STAGE_TAGS.put("环保水务", List.of(
            new ChainStage("上游·环保材料", "up", List.of("环保", "土壤修复")),
            new ChainStage("中游·处理工艺", "mid", List.of("污水处理", "水务/水环境", "废气治理", "固废处理")),
            new ChainStage("下游·环保服务", "down", List.of("危险品运输"))
        ));
        // 物流仓储：上游仓储 → 中游运输 → 下游配送
        CHAIN_STAGE_TAGS.put("物流仓储", List.of(
            new ChainStage("上游·仓储设施", "up", List.of("仓储", "冷链物流")),
            new ChainStage("中游·干线运输", "mid", List.of("物流/仓储", "多式联运/货代", "港口物流", "港口航道")),
            new ChainStage("下游·末端配送", "down", List.of("快递配送"))
        ));
        // 能源动力：上游能源 → 中游电力 → 下游供应
        CHAIN_STAGE_TAGS.put("能源动力", List.of(
            new ChainStage("上游·能源开采", "up", List.of("天然气", "煤炭")),
            new ChainStage("中游·电力生产", "mid", List.of("电力/供电", "电力工程", "电力设备", "新能源")),
            new ChainStage("下游·能源供应", "down", List.of("燃气供应", "供热", "碳中和"))
        ));
        // 交通装备：上游部件 → 中游制造 → 下游应用
        CHAIN_STAGE_TAGS.put("交通装备", List.of(
            new ChainStage("上游·交通部件", "up", List.of("航空航天", "船舶/海工")),
            new ChainStage("中游·装备制造", "mid", List.of("无人机", "测绘")),
            new ChainStage("下游·检验服务", "down", List.of("检验检测/认证"))
        ));
        // 建材家居：上游材料 → 中游制品 → 下游家居
        CHAIN_STAGE_TAGS.put("建材家居", List.of(
            new ChainStage("上游·建材原料", "up", List.of("建材", "玻璃", "陶瓷", "涂料")),
            new ChainStage("中游·家居制造", "mid", List.of("家具制造", "印刷包装")),
            new ChainStage("下游·家居零售", "down", List.of("日用消费品"))
        ));
    }

    /** 产业链环节（环节名 + 层级 + 领域标签列表） */
    private static final class ChainStage {
        final String stageName;
        final String layer;
        final List<String> tags;
        ChainStage(String stageName, String layer, List<String> tags) {
            this.stageName = stageName;
            this.layer = layer;
            this.tags = tags;
        }
    }

    /** 通用服务标签（非产业属性，不归类到任何产业群） */
    private static final List<String> GENERIC_SERVICE_TAGS = List.of(
        "人力资源", "劳务/人力资源", "咨询服务", "贸易", "企业服务", "租赁服务", "投资管理", "普惠金融",
        "银行", "证券/基金", "保险", "金融服务", "法律服务", "财会/评估", "教育培训", "广告/会展",
        "广告/传媒", "酒店/公寓", "物业管理", "物业/保洁", "安保服务", "殡葬服务", "旅游/景区", "拍卖");

    /** IPC class → 产业链环节角色（上游/中游/下游） */
    private String ipcToLayer(String ipc) {
        if (ipc == null || ipc.isEmpty()) return "mid";
        // 优先用 IPC class（3位）映射
        if (ipc.length() >= 3) {
            String cls = ipc.substring(0, 3);
            String layer = IPC_CLASS_LAYER.get(cls);
            if (layer != null) return layer;
        }
        // 回退：按 section 字母
        char section = ipc.charAt(0);
        // 原材料/化学/冶金 → 上游
        if (section == 'C' || section == 'A' || section == 'D') return "up";
        // 加工/制造/机械 → 中游
        if (section == 'B' || section == 'F' || section == 'E') return "mid";
        // 检测/电气/通信 → 下游
        if (section == 'G' || section == 'H') return "down";
        return "mid";
    }

    /**
     * 启动时自动构建（industry_chain 空 或 chain_node 空）
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            List<IndustryChain> chains = chainInfoMapper.selectAllOrdered();
            Long nodeCount = chainNodeMapper.selectCount(null);
            boolean chainEmpty = chains == null || chains.isEmpty();
            boolean nodeEmpty = nodeCount == null || nodeCount == 0;
            if (!chainEmpty && !nodeEmpty) {
                log.info("产业链图谱已存在（industry_chain={}, chain_node={}），跳过自动构建", chains.size(), nodeCount);
                return;
            }
            log.info("产业链数据为空（chain={}, node={}），开始自动聚合构建...", chainEmpty ? 0 : chains.size(), nodeCount);
            rebuildGraph();
            log.info("产业链全景图谱自动聚合构建完成");
        } catch (Exception e) {
            log.error("产业链自动聚合构建失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 重建产业链图谱（幂等：链定义若存在则保留，节点/关联重建）
     */
    public void rebuildGraph() {
        long start = System.currentTimeMillis();
        // 1. 链定义：industry_chain 为空时自动聚合生成
        int chainCount = seedChainsFromData();
        // 2. 清空图谱相关表（保留 industry_chain）
        clearGraph();
        // 3. 节点：每个领域标签 → chain_node（层级=环节定义）
        int nodeCount = buildNodesFromTags();
        // 4. 连线：同链内按层串连 + 跨链共现连线
        int linkCount = buildLinks();
        // 5. 企业归属
        int entNodeCount = buildEnterpriseNodes();
        // 6. 专利映射
        int ipcRuleCount = seedIpcChainMapping();
        int patentNodeCount = buildPatentNodes();
        // 7. 诊断
        int diagCount = buildDiagnosis();
        // 8. Neo4j 同步
        Neo4jSyncService neo4jSync = neo4jSyncServiceProvider.getIfAvailable();
        if (neo4jSync != null) {
            try {
                neo4jSync.syncAll();
            } catch (Exception e) {
                log.warn("Neo4j 图谱同步失败（可忽略）: {}", e.getMessage());
            }
        }
        long elapsed = System.currentTimeMillis() - start;
        log.info("图谱重建完成: 链={}, 节点={}, 连线={}, 企业关联={}, IPC规则={}, 专利映射={}, 诊断={}, 耗时={}ms",
                chainCount, nodeCount, linkCount, entNodeCount, ipcRuleCount, patentNodeCount, diagCount, elapsed);
    }

    /**
     * 产业链生成：按「产业链 × 上中下游环节 × 领域标签」映射生成
     * 每条产业链含上/中/下游环节，每环节映射领域标签。
     * 仅当 industry_chain 为空时执行（保证 id 稳定）
     */
    private int seedChainsFromData() {
        List<IndustryChain> existing = chainInfoMapper.selectAllOrdered();
        if (existing != null && !existing.isEmpty()) {
            return existing.size();
        }

        // 规模企业 ID 集合（产业链存在性判定：有规模企业支撑）
        Set<Long> scaleEnterpriseIds = new HashSet<>();
        List<Long> scaleIds = enterpriseMapper.selectScaleEnterpriseIds();
        if (scaleIds != null) scaleEnterpriseIds.addAll(scaleIds);

        int chainId = 1;
        int generated = 0;
        chainIdByIpcPrefix = new HashMap<>();

        for (Map.Entry<String, List<ChainStage>> entry : CHAIN_STAGE_TAGS.entrySet()) {
            String chainName = entry.getKey();
            List<ChainStage> stages = entry.getValue();

            // 该链所有环节的领域标签（去重）
            Set<String> chainTags = new LinkedHashSet<>();
            for (ChainStage stage : stages) {
                chainTags.addAll(stage.tags);
            }

            // 该链覆盖的企业（所有环节标签的本地企业并集 — 产业链只用本地企业构建）
            Set<Long> chainEnts = new HashSet<>();
            for (String tag : chainTags) {
                List<Long> ents = enterpriseTagMapper.selectLocalEnterpriseIdsByTagName(tag);
                if (ents != null) chainEnts.addAll(ents);
            }
            // 存在性判定：企业数 ≥5 且至少 1 家规模企业
            if (chainEnts.size() < 5 || !chainEnts.stream().anyMatch(scaleEnterpriseIds::contains)) {
                log.debug("产业链不存在（企业数={}, 无规模企业），跳过: {}", chainEnts.size(), chainName);
                continue;
            }

            IndustryChain chain = IndustryChain.builder()
                    .id((long) chainId)
                    .name(chainName)
                    .code("AUTO_" + chainId)
                    .isDeleted(0)
                    .createdAt(NOW)
                    .updatedAt(NOW)
                    .build();
            chainInfoMapper.insert(chain);
            // 记录该链的标签 → 链 id 映射（buildNodesFromTags 用）
            for (String tag : chainTags) {
                chainIdByIpcPrefix.put("tag:" + tag, (long) chainId);
            }
            generated++;
            chainId++;
        }
        log.info("按环节标签生成产业链: {} 条", generated);
        return generated;
    }

    /**
     * 构建 IPC class → 企业集合（从专利归属，Jaccard 计算用）
     * 只纳入企业数 ≥ MIN_IPC_ENT 的 subclass（与节点生成粒度一致）
     */
    private Map<String, Set<Long>> buildClassToEnterprises() {
        Map<String, Set<Long>> result = new HashMap<>();
        List<Map<String, Object>> ipcStats = patentMapper.selectIpcSubclassStats();
        if (ipcStats == null) return result;
        for (Map<String, Object> stat : ipcStats) {
            String ipc = (String) stat.get("ipcSubclass");
            Number entCount = (Number) stat.get("entCount");
            if (ipc == null || !ipc.matches("^[A-H].*")) continue;
            int ents = entCount == null ? 0 : entCount.intValue();
            if (ents < MIN_IPC_ENT) continue;  // 与 buildNodesFromIpc 一致：企业数不足的 subclass 不建节点
            String cls = ipc.length() >= 3 ? ipc.substring(0, 3) : ipc;
            List<Long> entIds = patentMapper.selectEnterpriseIdsByIpcSubclass(ipc);
            if (entIds == null) continue;
            result.computeIfAbsent(cls, k -> new HashSet<>()).addAll(entIds);
        }
        return result;
    }

    /**
     * 簇 → 规范链名：取簇内企业数最多的 IPC class 的映射名
     */
    private String nameCluster(List<String> cluster, Map<String, Integer> classEnts) {
        String topClass = cluster.stream()
                .max(Comparator.comparingInt(c -> classEnts.getOrDefault(c, 0)))
                .orElse(null);
        if (topClass != null) {
            String chainName = IPC_CLASS_CHAIN.get(topClass);
            if (chainName != null) return chainName;
            return topClass;
        }
        return "综合产业链";
    }

    /**
     * 清空图谱相关表（保留 industry_chain）
     */
    private void clearGraph() {
        chainNodeMapper.delete(null);
        chainNodeLinkMapper.delete(null);
        enterpriseNodeMapper.delete(null);
        patentNodeMapper.delete(null);
        nodeDiagnosisMapper.delete(null);
        ipcChainMappingMapper.delete(null);
        log.info("已清空图谱相关表（保留产业链定义）");
    }

    /**
     * 从「产业链 × 环节 × 领域标签」生成产业链节点
     * 每条链的每个环节 → 该环节的领域标签各成一个节点（节点名 = 标签名）
     * 节点层级 = 环节定义（up/mid/down）。领域内必须要有公司（标签企业数为 0 不建节点）
     */
    private int buildNodesFromTags() {
        List<IndustryChain> chains = chainInfoMapper.selectAllOrdered();
        if (chains == null || chains.isEmpty()) {
            log.warn("无产业链定义，跳过节点生成");
            return 0;
        }
        // 链 id → 链名
        Map<Long, String> chainNameById = new HashMap<>();
        for (IndustryChain chain : chains) chainNameById.put(chain.getId(), chain.getName());

        int inserted = 0;
        for (IndustryChain chain : chains) {
            List<ChainStage> stages = CHAIN_STAGE_TAGS.get(chain.getName());
            if (stages == null) continue;

            for (ChainStage stage : stages) {
                for (String tag : stage.tags) {
                    // 领域必须要有公司：查该标签本地企业数
                    List<Long> tagEnts = enterpriseTagMapper.selectLocalEnterpriseIdsByTagName(tag);
                    int entCount = tagEnts == null ? 0 : tagEnts.size();
                    if (entCount == 0) continue;

                    ChainNode node = ChainNode.builder()
                            .chainId(chain.getId())
                            .nodeName(tag)
                            .layer(stage.layer)
                            .description(stage.stageName + "领域: " + tag)
                            .sortOrder(0)
                            .isDeleted(0)
                            .createdAt(NOW)
                            .updatedAt(NOW)
                            .build();
                    chainNodeMapper.insert(node);
                    inserted++;
                }
            }
        }
        log.info("生成产业链节点（领域标签）: {}", inserted);
        return inserted;
    }

    /** 每层节点数上限（三列均衡） */
    private static final int MAX_NODES_PER_LAYER = 8;

    /** 链 → 覆盖企业（buildNodesFromTags 构建，buildEnterpriseNodes 消费） */
    private Map<Long, Set<Long>> chainIdToEnts = new HashMap<>();

    /** IPC class 前缀 → 链 id 的映射（由 seedChainsFromData 构建，buildNodesFromTags 消费） */
    private Map<String, Long> chainIdByIpcPrefix = new HashMap<>();

    /**
     * 生成连线：同链内按层串连（up→mid→down SUPPLIES）
     */
    private int buildLinks() {
        List<ChainNode> allNodes = chainNodeMapper.selectList(null);
        if (allNodes == null || allNodes.isEmpty()) return 0;

        Map<Long, List<ChainNode>> byChain = allNodes.stream()
                .collect(Collectors.groupingBy(ChainNode::getChainId, LinkedHashMap::new, Collectors.toList()));

        int inserted = 0;
        for (List<ChainNode> chainNodes : byChain.values()) {
            if (chainNodes.size() < 2) continue;
            // 按层分组：上游/中游/下游
            Map<String, List<ChainNode>> byLayer = new LinkedHashMap<>();
            for (ChainNode n : chainNodes) {
                byLayer.computeIfAbsent(n.getLayer() != null ? n.getLayer() : "mid", k -> new ArrayList<>()).add(n);
            }
            List<ChainNode> up = byLayer.getOrDefault("up", new ArrayList<>());
            List<ChainNode> mid = byLayer.getOrDefault("mid", new ArrayList<>());
            List<ChainNode> down = byLayer.getOrDefault("down", new ArrayList<>());

            // 同一层的领域不相连，只跨层相连：
            // 上游 → 中游（每个上游连所有中游，或按序配对）
            for (ChainNode u : up) {
                for (ChainNode m : mid) {
                    insertLink(u, m);
                    inserted++;
                }
            }
            // 中游 → 下游
            for (ChainNode m : mid) {
                for (ChainNode d : down) {
                    insertLink(m, d);
                    inserted++;
                }
            }
            // 若缺中游：上游 → 下游直接相连
            if (mid.isEmpty()) {
                for (ChainNode u : up) {
                    for (ChainNode d : down) {
                        insertLink(u, d);
                        inserted++;
                    }
                }
            }
        }
        log.info("生成产业链连线（跨层依赖）: {}", inserted);
        return inserted;
    }

    private void insertLink(ChainNode src, ChainNode tgt) {
        if (src.getId().equals(tgt.getId())) return;
        ChainNodeLink link = ChainNodeLink.builder()
                .sourceNodeId(src.getId())
                .targetNodeId(tgt.getId())
                .relationType("SUPPLIES")
                .isDeleted(0)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
        chainNodeLinkMapper.insert(link);
    }

    private int layerRank(String layer) {
        if ("up".equals(layer)) return 1;
        if ("mid".equals(layer)) return 2;
        return 3;
    }

    /**
     * 企业归属：企业按其领域标签归入对应节点（节点名 = 标签名）
     * 同名标签可能出现在多条链 → 逐个节点独立归属（不用 map 覆盖）
     */
    private int buildEnterpriseNodes() {
        List<ChainNode> allNodes = chainNodeMapper.selectList(null);
        if (allNodes == null || allNodes.isEmpty()) return 0;

        int inserted = 0;
        List<EnterpriseNode> batch = new ArrayList<>();
        for (ChainNode node : allNodes) {
            List<Long> enterpriseIds = enterpriseTagMapper.selectLocalEnterpriseIdsByTagName(node.getNodeName());
            if (enterpriseIds == null || enterpriseIds.isEmpty()) continue;
            for (Long eid : enterpriseIds) {
                batch.add(EnterpriseNode.builder()
                        .enterpriseId(eid)
                        .nodeId(node.getId())
                        .matchType("auto")
                        .createdAt(NOW)
                        .updatedAt(NOW)
                        .build());
                inserted++;
                if (batch.size() >= 500) {
                    enterpriseNodeMapper.batchInsert(batch);
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            enterpriseNodeMapper.batchInsert(batch);
            batch.clear();
        }
        log.info("生成企业-节点关联（领域标签归属）: {}", inserted);
        return inserted;
    }

    /**
     * 专利归属：每个节点（领域标签）的专利 = 该领域覆盖企业的专利
     * 直接按企业归属，不依赖 A1 算法（A1 依赖 IPC 规则，标签节点匹配不上）
     */
    private int buildPatentNodes() {
        List<ChainNode> allNodes = chainNodeMapper.selectList(null);
        if (allNodes == null || allNodes.isEmpty()) return 0;

        int inserted = 0;
        List<PatentNode> batch = new ArrayList<>();
        for (ChainNode node : allNodes) {
            List<Long> enterpriseIds = enterpriseTagMapper.selectLocalEnterpriseIdsByTagName(node.getNodeName());
            if (enterpriseIds == null || enterpriseIds.isEmpty()) continue;
            // 分批查询该领域企业的专利
            for (int i = 0; i < enterpriseIds.size(); i += 500) {
                List<Long> subIds = enterpriseIds.subList(i, Math.min(i + 500, enterpriseIds.size()));
                List<Patent> patents = patentMapper.selectByEnterpriseIds(subIds);
                if (patents == null) continue;
                for (Patent patent : patents) {
                    batch.add(PatentNode.builder()
                            .patentId(patent.getId())
                            .nodeId(node.getId())
                            .matchType("auto")
                            .createdAt(NOW)
                            .updatedAt(NOW)
                            .build());
                    inserted++;
                    if (batch.size() >= 500) {
                        patentNodeMapper.batchInsert(batch);
                        batch.clear();
                    }
                }
            }
        }
        if (!batch.isEmpty()) {
            patentNodeMapper.batchInsert(batch);
            batch.clear();
        }
        log.info("生成专利-节点映射（企业专利归属）: {}", inserted);
        return inserted;
    }

    /**
     * 聚合生成节点诊断数据
     */
    private int buildDiagnosis() {
        List<ChainNode> allNodes = chainNodeMapper.selectList(null);
        if (allNodes == null || allNodes.isEmpty()) return 0;

        int inserted = 0;
        LocalDate today = LocalDate.now();
        for (ChainNode node : allNodes) {
            int enterpriseCount = enterpriseNodeMapper.countByNodeId(node.getId());
            int patentCount = patentNodeMapper.countByNodeId(node.getId());
            int highValueCount = patentNodeMapper.countHighValueByNodeId(node.getId());

            String tag;
            if (enterpriseCount >= 20 || patentCount >= 200) {
                tag = "强链";
            } else if (enterpriseCount >= 5 || patentCount >= 50) {
                tag = "补链";
            } else if (enterpriseCount > 0 || patentCount > 0) {
                tag = "弱链";
            } else {
                tag = "缺链";
            }

            NodeDiagnosis diag = NodeDiagnosis.builder()
                    .regionId(340500L)  // 马鞍山市
                    .nodeId(node.getId())
                    .diagnosisTag(tag)
                    .highValuePatentCount(highValueCount)
                    .totalPatentCount(patentCount)
                    .enterpriseCount(enterpriseCount)
                    .patentDensity(enterpriseCount > 0 ? new BigDecimal((double) patentCount / enterpriseCount) : BigDecimal.ZERO)
                    .diagnosisBasis("基于IPC专利聚类自动生成")
                    .diagnosisDate(today)
                    .isDeleted(0)
                    .createdAt(NOW)
                    .updatedAt(NOW)
                    .build();
            nodeDiagnosisMapper.insert(diag);
            inserted++;
        }
        log.info("生成节点诊断: {}", inserted);
        return inserted;
    }

    /**
     * 填充 IPC→节点映射规则（基于节点关联企业的专利 IPC 分布）
     */
    private int seedIpcChainMapping() {
        List<ChainNode> allNodes = chainNodeMapper.selectList(null);
        if (allNodes == null || allNodes.isEmpty()) return 0;

        int seeded = 0;
        for (ChainNode node : allNodes) {
            // 从节点名解析 IPC
            String name = node.getNodeName();
            if (name == null || !name.contains("（") || !name.endsWith("）")) continue;
            String ipc = name.substring(name.lastIndexOf('（') + 1, name.length() - 1);
            if (ipc.isEmpty()) continue;

            IpcChainMapping m = IpcChainMapping.builder()
                    .ipcCode(ipc)
                    .ipcLevel("SUB_CLASS")
                    .nodeId(node.getId())
                    .weight(0.9)
                    .description("IPC subclass 自动映射")
                    .build();
            ipcChainMappingMapper.insert(m);
            seeded++;
        }
        log.info("填充 IPC→节点映射规则: {}", seeded);
        return seeded;
    }
}
