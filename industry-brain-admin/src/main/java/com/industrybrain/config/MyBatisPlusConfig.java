package com.industrybrain.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * MyBatis-Plus 配置类
 * 当 mysql.enabled=true 时创建 MySQL DataSource；
 * 当 mysql.enabled=false 时自动切换为 H2 嵌入式数据库（开发环境无 MySQL 时使用）。
 */
@Configuration
public class MyBatisPlusConfig {

    @Value("${mysql.enabled:true}")
    private boolean mysqlEnabled;

    /**
     * 数据源 Bean — 根据 mysql.enabled 选择 MySQL 或 H2
     * 由 MultiDataSourceConfig 在 mysql.enabled=true 时定义，此处仅在 false 时提供 H2 回退
     */
    @Bean
    @ConditionalOnProperty(name = "mysql.enabled", havingValue = "false")
    public DataSource h2DataSource() {
        com.zaxxer.hikari.HikariDataSource ds = new com.zaxxer.hikari.HikariDataSource();
        ds.setJdbcUrl("jdbc:h2:mem:industry_brain;DB_CLOSE_DELAY=-1;MODE=MySQL");
        ds.setDriverClassName("org.h2.Driver");
        ds.setUsername("sa");
        ds.setPassword("");
        ds.setMinimumIdle(2);
        ds.setMaximumPoolSize(10);
        ds.setIdleTimeout(600000);
        ds.setConnectionTimeout(10000);
        ds.setConnectionTestQuery("SELECT 1");
        return ds;
    }

    /**
     * MyBatis SqlSessionFactory
     * 当 mysql.enabled=false 时激活（此时 MySQL 数据源不存在，使用 H2）
     */
    @Bean
    @ConditionalOnProperty(name = "mysql.enabled", havingValue = "false")
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setJdbcTypeForNull(JdbcType.NULL);
        configuration.setLogImpl(org.apache.ibatis.logging.stdout.StdOutImpl.class);
        // 指定数据库类型以生成正确的 SQL（H2 模拟 MySQL 行为）
        configuration.setDatabaseId("h2");
        factory.setConfiguration(configuration);

        // Mapper XML 文件
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        factory.setMapperLocations(resolver.getResources("classpath:mapper/mysql/*.xml"));

        return factory.getObject();
    }
}
