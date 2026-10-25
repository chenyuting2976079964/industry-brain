package com.industrybrain.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrybrain.config.LLMConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * LLM 对话客户端
 * 封装 SiliconFlow / OpenAI 兼容接口，用于报告内容增强生成。
 * 使用 JDK17 内置 HttpClient，无新增依赖。
 * 失败时返回 null，调用方应降级到规则模板。
 *
 * @author 企开得胜
 */
@Slf4j
@Component
public class LLMClient {

    private final LLMConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    public LLMClient(LLMConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    /**
     * 判断 LLM 服务是否可用
     */
    public boolean isEnabled() {
        return config.isEnabled();
    }

    /**
     * 生成对话响应（带重试和降级）
     *
     * @param prompt 用户提示词
     * @return LLM 生成的文本，失败返回 null
     */
    public String generate(String prompt) {
        if (!isEnabled()) {
            log.debug("LLM 服务未启用，跳过 AI 分析生成");
            return null;
        }

        for (int attempt = 0; attempt <= config.getMaxRetries(); attempt++) {
            try {
                String result = callChatApi(prompt);
                if (result != null && !result.isBlank()) {
                    return result.trim();
                }
                log.warn("LLM 返回空响应 (attempt={})", attempt + 1);
            } catch (Exception e) {
                if (attempt < config.getMaxRetries()) {
                    long backoff = config.getRetryBackoffMs() * (1L << attempt);
                    log.warn("LLM 调用失败（第{}次）: {}，{}ms 后重试",
                            attempt + 1, e.getMessage(), backoff);
                    try {
                        TimeUnit.MILLISECONDS.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                } else {
                    log.error("LLM 调用失败（已重试{}次）: {}", config.getMaxRetries() + 1, e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * 调用 Chat Completions API（OpenAI 兼容格式）
     */
    private String callChatApi(String prompt) throws Exception {
        String url = config.getBaseUrl() + "/chat/completions";

        // 构建请求体
        JsonNode body = objectMapper.createObjectNode()
                .put("model", config.getModel())
                .put("max_tokens", config.getMaxTokens())
                .put("temperature", config.getTemperature())
                .set("messages", objectMapper.createArrayNode()
                        .add(objectMapper.createObjectNode()
                                .put("role", "user")
                                .put("content", prompt)));

        String json = objectMapper.writeValueAsString(body);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(config.getTimeoutMs()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));

        HttpResponse<String> resp = httpClient.send(builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (resp.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + resp.statusCode() + ": "
                    + truncate(resp.body(), 300));
        }

        JsonNode root = objectMapper.readTree(resp.body());
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            return choices.get(0).path("message").path("content").asText(null);
        }
        return null;
    }

    private String truncate(String s, int max) {
        return s == null ? "" : (s.length() > max ? s.substring(0, max) + "…" : s);
    }
}
