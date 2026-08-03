package com.industrybrain.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * LLM 大模型对话配置类
 * 绑定 llm.* 配置项，用于报告内容增强生成。
 * 使用 SiliconFlow OpenAI 兼容接口（支持 DeepSeek/Qwen/GLM 等模型）。
 * enabled=false 时报告生成降级为纯规则模板，不阻断流程。
 *
 * @author 产业大脑平台
 */
@Configuration
public class LLMConfig {

    private static final Logger log = LoggerFactory.getLogger(LLMConfig.class);

    @Value("${llm.enabled:false}")
    private boolean enabled;

    @Value("${llm.base-url:https://api.siliconflow.cn/v1}")
    private String baseUrl;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model:deepseek-ai/DeepSeek-V3}")
    private String model;

    @Value("${llm.max-tokens:2000}")
    private int maxTokens;

    @Value("${llm.temperature:0.3}")
    private double temperature;

    @Value("${llm.timeout-ms:30000}")
    private int timeoutMs;

    @Value("${llm.max-retries:2}")
    private int maxRetries;

    @Value("${llm.retry-backoff-ms:500}")
    private int retryBackoffMs;

    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getRetryBackoffMs() {
        return retryBackoffMs;
    }
}
