package com.industrybrain.service.embedding;

import com.industrybrain.config.EmbeddingConfig;
import com.industrybrain.utils.VectorOps;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 降级向量提供者：基于标签文本哈希生成确定性伪向量。
 * 无 embedding key 或 API 不可用时使用，保证聚类流程完整跑通（dry-run 依赖）。
 *
 * 伪向量含关键词 bucket 增强：同产业关键词（AI/数据/材料/化工等）的标签向量更接近。
 *
 * @author 产业大脑平台
 */
@Component
public class FallbackEmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingConfig config;

    public FallbackEmbeddingProvider(EmbeddingConfig config) {
        this.config = config;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        List<float[]> result = new ArrayList<>(texts.size());
        for (String t : texts) {
            result.add(embed(t));
        }
        return result;
    }

    @Override
    public float[] embed(String text) {
        float[] v = VectorOps.hashVector(text, config.getDimension());
        // 关键词 bucket 增强：语义近亲关键词共享同一方向
        addKeywordBias(v, text, config.getDimension());
        return v;
    }

    private void addKeywordBias(float[] v, String text, int dim) {
        if (text == null) return;
        for (String[] bucket : KEYWORD_BUCKETS) {
            for (String kw : bucket) {
                if (text.contains(kw)) {
                    int idx = Math.floorMod(kw.hashCode(), dim);
                    v[idx] += 3f;
                    break;
                }
            }
        }
    }

    /** 关键词 → 产业 bucket：同一 bucket 内标签向量相近 */
    private static final String[][] KEYWORD_BUCKETS = {
        {"人工智能", "智能", "机器人", "自动化", "大数据", "云计算", "物联网", "数字"},
        {"软件", "开发", "系统", "集成", "区块链", "信息", "通信", "互联网"},
        {"材料", "化工", "化学", "金属", "钢铁", "冶金", "合金", "涂料", "塑料", "橡胶"},
        {"光伏", "太阳能", "新能源", "风电", "储能", "氢能", "电池", "充电"},
        {"医药", "制药", "生物", "医疗", "健康"},
        {"机械", "设备", "制造", "加工", "模具", "精密"},
        {"建筑", "施工", "工程", "装饰", "园林", "建材", "市政", "路桥"},
        {"食品", "农产品", "养殖", "餐饮", "零售"},
        {"环保", "水务", "污水", "固废", "废气", "土壤"},
        {"物流", "仓储", "运输", "配送", "港口", "冷链"},
        {"汽车", "车辆", "交通", "航空", "船舶"},
    };
}
