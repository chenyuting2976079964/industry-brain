package com.industrybrain.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Mapper 扫描配置
 * 注册所有 Mapper 接口为 Spring Bean
 */
@Configuration
@MapperScan(basePackages = {
    "com.industrybrain.mapper.mysql",
    "com.industrybrain.mapper.elasticsearch",
    "com.industrybrain.mapper.neo4j"
})
public class MyBatisMapperConfig {
}
