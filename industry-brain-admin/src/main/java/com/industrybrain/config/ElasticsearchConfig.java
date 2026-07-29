package com.industrybrain.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch 配置类
 * ES 8.x 使用新的 ElasticsearchClient（替代废弃的 RestHighLevelClient）
 * 条件加载：仅当 elasticsearch.enabled=true 时激活
 */
@Configuration
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public class ElasticsearchConfig {

    @Value("${elasticsearch.host}")
    private String esHost;

    @Value("${elasticsearch.port}")
    private int esPort;

    /**
     * ES RestClient Bean
     */
    @Bean
    public RestClient restClient() {
        return RestClient.builder(new HttpHost(esHost, esPort, "http"))
                .setRequestConfigCallback(req -> req
                        .setConnectTimeout(5000)
                        .setSocketTimeout(60000))
                .build();
    }

    /**
     * ES Transport Bean（带 Jackson 序列化支持）
     */
    @Bean
    public RestClientTransport restClientTransport(RestClient restClient) {
        return new RestClientTransport(restClient, new JacksonJsonpMapper());
    }

    /**
     * ES Client Bean
     */
    @Bean
    public ElasticsearchClient elasticsearchClient(RestClientTransport transport) {
        return new ElasticsearchClient(transport);
    }
}
