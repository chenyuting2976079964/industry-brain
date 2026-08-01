package com.industrybrain.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Embedding 向量嵌入配置类
 * 绑定 embedding.* 配置项，供自动聚类构建产业链时调用外部 LLM 嵌入 API。
 * 未配置 api-key（enabled=false）时走纯规则聚类降级，不阻断构建。
 *
 * @author 产业大脑平台
 */
@Configuration
public class EmbeddingConfig {
    private static final Logger log = LoggerFactory.getLogger(EmbeddingConfig.class);

    /** 是否启用外部 embedding API（配好 key 后改为 true） */
    @Value("${embedding.enabled:false}")
    private boolean enabled;

    /** 嵌入服务基础地址（OpenAI 兼容，POST {baseUrl}/v1/embeddings） */
    @Value("${embedding.base-url:http://localhost:8000}")
    private String baseUrl;

    /** 嵌入服务 API Key */
    @Value("${embedding.api-key:}")
    private String apiKey;

    /** 嵌入模型名 */
    @Value("${embedding.model:text-embedding-3-small}")
    private String model;

    /** 向量维度 */
    @Value("${embedding.dimension:1536}")
    private int dimension;

    /** 每批嵌入的文本数 */
    @Value("${embedding.batch-size:16}")
    private int batchSize;

    /** 请求超时（毫秒） */
    @Value("${embedding.timeout-ms:15000}")
    private int timeoutMs;

    /** 失败重试次数 */
    @Value("${embedding.max-retries:2}")
    private int maxRetries;

    /** 重试退避基数（毫秒） */
    @Value("${embedding.retry-backoff-ms:500}")
    private int retryBackoffMs;

    /** 聚类亲和度阈值（同簇判定） */
    @Value("${embedding.cluster-threshold:0.42}")
    private double clusterThreshold;

    /** 亲和矩阵中向量余弦权重 */
    @Value("${embedding.vector-weight:0.7}")
    private double vectorWeight;

    /** 亲和矩阵中规则亲和权重 */
    @Value("${embedding.rule-weight:0.3}")
    private double ruleWeight;

    /** 每标签聚合的企业简介条数上限 */
    @Value("${embedding.description-limit:5}")
    private int descriptionLimit;

    public boolean isEnabled() { return enabled; }
    public String getBaseUrl() { return baseUrl; }
    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
    public int getDimension() { return dimension; }
    public int getBatchSize() { return batchSize; }
    public int getTimeoutMs() { return timeoutMs; }
    public int getMaxRetries() { return maxRetries; }
    public int getRetryBackoffMs() { return retryBackoffMs; }
    public double getClusterThreshold() { return clusterThreshold; }
    public double getVectorWeight() { return vectorWeight; }
    public double getRuleWeight() { return ruleWeight; }
    public int getDescriptionLimit() { return descriptionLimit; }
}
