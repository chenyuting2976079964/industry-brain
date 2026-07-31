package com.industrybrain.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Mapper 扫描配置
 * 注册 MySQL Mapper 接口为 Spring Bean。
 * 注意：elasticsearch/neo4j 包下的接口由各自的 @Component 实现类提供
 * （@ConditionalOnBean 保护），不在此处注册为 MyBatis 代理，避免
 * 与 @Resource 按字段名解析冲突。
 */
@Configuration
@MapperScan(basePackages = {
    "com.industrybrain.mapper.mysql"
})
public class MyBatisMapperConfig {
}
