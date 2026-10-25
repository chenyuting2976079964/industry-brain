package com.industrybrain.service.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrybrain.config.EmbeddingConfig;
import com.industrybrain.utils.VectorOps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI 兼容向量嵌入客户端（POST {baseUrl}/embeddings）
 * 使用 JDK17 内置 HttpClient，无新增依赖。
 * 失败时按批次交给 FallbackEmbeddingProvider 生成确定性伪向量，不抛异常。
 *
 * @author 企开得胜
 */
@Slf4j
@Component
@Primary
public class EmbeddingClient implements EmbeddingProvider {

    private final EmbeddingConfig config;
    private final FallbackEmbeddingProvider fallback;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    /** 简单结果缓存：同文本同向量（一次 rebuild 内多次调用同一标签时复用） */
    private final Map<String, float[]> cache = new ConcurrentHashMap<>();

    public EmbeddingClient(EmbeddingConfig config, FallbackEmbeddingProvider fallback) {
        this.config = config;
        this.fallback = fallback;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled() && config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (!isEnabled()) {
            return fallback.embed(texts);
        }
        List<float[]> result = new ArrayList<>(texts.size());
        int success = 0;
        int batchSize = config.getBatchSize();
        for (int i = 0; i < texts.size(); i += batchSize) {
            List<String> batch = texts.subList(i, Math.min(i + batchSize, texts.size()));
            List<float[]> batchVecs = embedBatch(batch);
            if (batchVecs == null) {
                // 整批失败 → 该批交给 fallback
                batchVecs = fallback.embed(batch);
            }
            for (float[] v : batchVecs) {
                if (v != null) {
                    success++;
                    result.add(VectorOps.resize(v, config.getDimension()));
                } else {
                    result.add(null);
                }
            }
        }
        double failRate = texts.isEmpty() ? 0 : 1 - (double) success / texts.size();
        if (failRate > 0.5) {
            log.warn("embedding 失败率 {}（{}/{}），本次聚类降级为纯规则模式", String.format("%.0f%%", failRate * 100),
                    texts.size() - success, texts.size());
        }
        return result;
    }

    @Override
    public float[] embed(String text) {
        float[] cached = cache.get(text);
        if (cached != null) return cached;
        if (!isEnabled()) {
            cached = fallback.embed(text);
        } else {
            List<float[]> vecs = embedBatch(List.of(text));
            cached = (vecs != null && !vecs.isEmpty()) ? vecs.get(0) : fallback.embed(text);
        }
        if (cached != null) cache.put(text, cached);
        return cached;
    }

    /**
     * 单批嵌入，失败重试 maxRetries 次（指数退避），最终失败返回 null。
     */
    private List<float[]> embedBatch(List<String> texts) {
        for (int attempt = 0; attempt <= config.getMaxRetries(); attempt++) {
            try {
                return callEmbeddingApi(texts);
            } catch (Exception e) {
                if (attempt < config.getMaxRetries()) {
                    long backoff = config.getRetryBackoffMs() * (1L << attempt);
                    log.warn("embedding 调用失败（第{}次）: {}，{}ms 后重试", attempt + 1, e.getMessage(), backoff);
                    try { TimeUnit.MILLISECONDS.sleep(backoff); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                } else {
                    log.error("embedding 调用失败（已重试{}次）: {}", config.getMaxRetries() + 1, e.getMessage());
                }
            }
        }
        return null;
    }

    private List<float[]> callEmbeddingApi(List<String> texts) throws Exception {
        // base-url 为 OpenAI 兼容 API 根路径（含 /v1 或 /compatible-mode/v1），在此追加 /embeddings
        String url = config.getBaseUrl() + "/embeddings";
        Map<String, Object> body = Map.of(
                "model", config.getModel(),
                "input", texts
        );
        String json = objectMapper.writeValueAsString(body);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(config.getTimeoutMs()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            builder.header("Authorization", "Bearer " + config.getApiKey());
        }
        HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + resp.statusCode() + ": " + truncate(resp.body(), 200));
        }
        JsonNode root = objectMapper.readTree(resp.body());
        JsonNode data = root.path("data");
        List<float[]> vecs = new ArrayList<>(data.size());
        for (JsonNode item : data) {
            JsonNode emb = item.path("embedding");
            if (emb.isArray()) {
                float[] v = new float[emb.size()];
                for (int i = 0; i < emb.size(); i++) v[i] = (float) emb.get(i).asDouble();
                vecs.add(v);
            } else {
                vecs.add(null);
            }
        }
        return vecs;
    }

    private String truncate(String s, int max) {
        return s == null ? "" : (s.length() > max ? s.substring(0, max) + "…" : s);
    }
}
