package com.industrybrain.config;

import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 对象存储配置类
 * 负责初始化 MinioClient，供文件上传/下载使用
 * 条件加载：仅当 minio.enabled=true 时激活
 */
@Configuration
public class MinioConfig {
    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * MinIO Client Bean
     * 用于文件上传、下载、删除等对象存储操作
     */
    @Bean
    public MinioClient minioClient() {
        log.info("初始化 MinIO Client: endpoint={}, bucket={}", endpoint, bucketName);
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        log.info("MinIO Client 初始化成功");
        return client;
    }

    /**
     * 获取默认存储桶名称
     */
    public String getBucketName() {
        return bucketName;
    }
}
