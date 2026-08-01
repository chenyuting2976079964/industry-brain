package com.industrybrain.service.cluster;

import com.industrybrain.config.EmbeddingConfig;
import com.industrybrain.mapper.mysql.EnterpriseMapper;
import com.industrybrain.mapper.mysql.EnterpriseTagMapper;
import com.industrybrain.mapper.mysql.PatentMapper;
import com.industrybrain.service.embedding.EmbeddingProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * 产业链自动聚类引擎单元测试
 * 注入 fake EmbeddingProvider 与 mock Mapper，验证聚类行为。
 */
class IndustryClusterEngineTest {

    private EmbeddingConfig config(boolean enabled, double threshold) {
        EmbeddingConfig c = Mockito.mock(EmbeddingConfig.class);
        when(c.isEnabled()).thenReturn(enabled);
        when(c.getDimension()).thenReturn(64);
        when(c.getDescriptionLimit()).thenReturn(2);
        when(c.getVectorWeight()).thenReturn(0.7);
        when(c.getRuleWeight()).thenReturn(0.3);
        when(c.getClusterThreshold()).thenReturn(threshold);
        when(c.getMaxRetries()).thenReturn(0);
        when(c.getRetryBackoffMs()).thenReturn(50);
        when(c.getTimeoutMs()).thenReturn(1000);
        when(c.getBatchSize()).thenReturn(16);
        when(c.getModel()).thenReturn("fake");
        when(c.getBaseUrl()).thenReturn("http://fake");
        when(c.getApiKey()).thenReturn("k");
        return c;
    }

    private EnterpriseTagMapper tagMapper(List<Map<String, Object>> stats,
                                          Map<String, List<Long>> entByTag) {
        EnterpriseTagMapper m = Mockito.mock(EnterpriseTagMapper.class);
        when(m.selectTagStats()).thenReturn(stats);
        when(m.selectLocalEnterpriseIdsByTagName(anyString())).thenAnswer(inv -> {
            String tag = inv.getArgument(0);
            return entByTag.getOrDefault(tag, Collections.emptyList());
        });
        return m;
    }

    private EnterpriseMapper entMapper() {
        EnterpriseMapper m = Mockito.mock(EnterpriseMapper.class);
        when(m.selectScaleEnterpriseIds()).thenReturn(List.of(1L, 2L, 11L, 18L, 30L));
        when(m.selectById(anyLong())).thenReturn(null); // 无 description
        return m;
    }

    private PatentMapper patentMapper() {
        PatentMapper m = Mockito.mock(PatentMapper.class);
        when(m.selectIpcSubclassStatsByEnterpriseIds(anyList())).thenReturn(Collections.emptyList());
        return m;
    }

    /** fake embedding：同 bucket 内文本同向量 */
    private EmbeddingProvider fakeProvider(boolean enabled, Map<String, float[]> vectors) {
        return new EmbeddingProvider() {
            @Override public boolean isEnabled() { return enabled; }
            @Override public List<float[]> embed(List<String> texts) {
                List<float[]> out = new ArrayList<>();
                for (String t : texts) out.add(vectors.getOrDefault(t, new float[64]));
                return out;
            }
            @Override public float[] embed(String text) { return vectors.getOrDefault(text, new float[64]); }
        };
    }

    private float[] vec(List<Integer> hotIdx) {
        float[] v = new float[64];
        for (int i : hotIdx) v[i] = 1f;
        return v;
    }

    /** 基础标签库：AI 系 + 建筑系 + 通用服务 */
    private Map<String, List<Long>> baseEnts() {
        Map<String, List<Long>> m = new HashMap<>();
        m.put("人工智能", List.of(1L, 2L, 3L, 4L, 5L));
        m.put("大数据", List.of(1L, 2L, 3L, 6L));
        m.put("云计算", List.of(2L, 3L, 7L));
        m.put("软件开发", List.of(8L, 9L, 10L));
        m.put("建筑装饰", List.of(11L, 12L, 13L, 14L, 15L));
        m.put("建筑施工/钢结构", List.of(11L, 12L, 16L, 17L));
        m.put("建材", List.of(18L, 19L, 20L, 21L, 22L));
        m.put("人力资源", List.of(30L, 31L, 32L)); // 通用服务
        return m;
    }

    @Test
    void fallback_embedding_is_deterministic() {
        EmbeddingConfig c = config(false, 0.42);
        // FallbackEmbeddingProvider 走其自身逻辑
        var fb = new com.industrybrain.service.embedding.FallbackEmbeddingProvider(c);
        assertFalse(fb.isEnabled());
        float[] a1 = fb.embed("人工智能");
        float[] a2 = fb.embed("人工智能");
        float[] b = fb.embed("大数据");
        assertArrayEquals(a1, a2, "同标签向量应确定");
        assertFalse(Arrays.equals(a1, b), "不同标签向量应不同");
        assertEquals(64, a1.length);
    }

    @Test
    void transitive_closure_merges_semantic_neighbors() {
        // 向量模式：AI 系标签同向量，建筑系同向量
        EmbeddingConfig c = config(true, 0.42);
        Map<String, float[]> vecs = new HashMap<>();
        vecs.put("人工智能", vec(List.of(0, 1)));
        vecs.put("大数据", vec(List.of(0, 1)));
        vecs.put("云计算", vec(List.of(0, 1)));
        vecs.put("软件开发", vec(List.of(0, 1)));
        vecs.put("建筑装饰", vec(List.of(20, 21)));
        vecs.put("建筑施工/钢结构", vec(List.of(20, 21)));
        vecs.put("建材", vec(List.of(20, 21)));

        List<Map<String, Object>> stats = new ArrayList<>();
        for (String t : vecs.keySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("tagName", t);
            stats.add(row);
        }
        EnterpriseTagMapper tagMapper = tagMapper(stats, baseEnts());
        IndustryClusterEngine engine = new IndustryClusterEngine(
                tagMapper, entMapper(), patentMapper(), fakeProvider(true, vecs), c);

        ClusterResult result = engine.buildClusters();
        assertFalse(result.getChains().isEmpty());
        boolean foundAi = false, foundBld = false;
        for (ClusterResult.ChainDef ch : result.getChains()) {
            Set<String> tags = new HashSet<>();
            ch.getTagsByLayer().values().forEach(tags::addAll);
            if (tags.contains("人工智能")) {
                foundAi = true;
                assertTrue(tags.contains("大数据") && tags.contains("云计算"), "AI 系应同簇");
            }
            if (tags.contains("建筑装饰")) {
                foundBld = true;
                assertTrue(tags.contains("建材"), "建筑系应同簇");
            }
        }
        assertTrue(foundAi, "应生成 AI 链");
        assertTrue(foundBld, "应生成建筑链");
    }

    @Test
    void generic_service_tags_are_filtered() {
        EmbeddingConfig c = config(true, 0.42);
        Map<String, float[]> vecs = new HashMap<>();
        for (String t : baseEnts().keySet()) vecs.put(t, vec(List.of(30)));
        List<Map<String, Object>> stats = new ArrayList<>();
        for (String t : baseEnts().keySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("tagName", t);
            stats.add(row);
        }
        IndustryClusterEngine engine = new IndustryClusterEngine(
                tagMapper(stats, baseEnts()), entMapper(), patentMapper(), fakeProvider(true, vecs), c);
        ClusterResult result = engine.buildClusters();
        for (ClusterResult.ChainDef ch : result.getChains()) {
            Set<String> tags = new HashSet<>();
            ch.getTagsByLayer().values().forEach(tags::addAll);
            assertFalse(tags.contains("人力资源"), "通用服务标签不应归链");
        }
    }

    @Test
    void cluster_below_min_tags_is_dropped() {
        // 只有 2 个标签（<3 不成链）
        EmbeddingConfig c = config(true, 0.42);
        Map<String, float[]> vecs = new HashMap<>();
        vecs.put("人工智能", vec(List.of(0)));
        vecs.put("大数据", vec(List.of(0)));
        List<Map<String, Object>> stats = new ArrayList<>();
        for (String t : vecs.keySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("tagName", t);
            stats.add(row);
        }
        Map<String, List<Long>> ents = Map.of(
                "人工智能", List.of(1L, 2L, 3L),
                "大数据", List.of(1L, 2L, 4L));
        IndustryClusterEngine engine = new IndustryClusterEngine(
                tagMapper(stats, ents), entMapper(), patentMapper(), fakeProvider(true, vecs), c);
        ClusterResult result = engine.buildClusters();
        assertTrue(result.getChains().isEmpty(), "标签数<3 的簇应被过滤");
    }
}
