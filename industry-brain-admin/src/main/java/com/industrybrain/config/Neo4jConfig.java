package com.industrybrain.config;

import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Neo4j 图数据库配置类
 * 条件加载：仅当 neo4j.enabled=true 时激活
 */
@Configuration
@ConditionalOnProperty(name = "neo4j.enabled", havingValue = "true", matchIfMissing = false)
public class Neo4jConfig {

    @Value("${neo4j.uri}")
    private String neo4jUri;

    @Value("${neo4j.username}")
    private String neo4jUsername;

    @Value("${neo4j.password}")
    private String neo4jPassword;

    /**
     * Neo4j Driver Bean
     */
    @Bean
    public Driver neo4jDriver() {
        return GraphDatabase.driver(neo4jUri,
                org.neo4j.driver.AuthTokens.basic(neo4jUsername, neo4jPassword));
    }
}
