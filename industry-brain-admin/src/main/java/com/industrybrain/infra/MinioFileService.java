package com.industrybrain.infra;

import com.industrybrain.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 文件存储服务（基础设施）
 * 封装文件上传/下载/删除等通用操作，确保存储桶存在
 * 当 minio.enabled=false 时跳过（无 MinIO 环境）
 */
@Service
public class MinioFileService {

    private static final Logger log = LoggerFactory.getLogger(MinioFileService.class);

    @Resource
    private MinioClient minioClient;

    @Resource
    private MinioConfig minioConfig;

    /**
     * 启动时确保默认存储桶存在
     */
    @PostConstruct
    public void ensureBucket() {
        try {
            String bucket = minioConfig.getBucketName();
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO 存储桶已创建: {}", bucket);
            } else {
                log.info("MinIO 存储桶已存在: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("MinIO 存储桶检查失败（MinIO 服务可能未启动）: {}", e.getMessage());
        }
    }

    /**
     * 上传文件到 MinIO
     *
     * @param objectName 对象名称（路径），如 "reports/2024/diagnosis_20240724.xlsx"
     * @param data       文件字节数组
     * @param contentType MIME 类型
     * @return 预签名 URL（7天有效）
     */
    public String upload(String objectName, byte[] data, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(new ByteArrayInputStream(data), data.length, -1)
                            .contentType(contentType)
                            .build());

            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .method(Method.GET)
                            .expiry(7, TimeUnit.DAYS)
                            .build());

            log.info("文件上传成功: object={}, size={} bytes", objectName, data.length);
            return url;
        } catch (Exception e) {
            log.error("MinIO 文件上传失败: object={}, error={}", objectName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 上传文件（自动生成 /分类/日期/ 前缀）
     */
    public String uploadToCategory(String category, String fileName, byte[] data, String contentType) {
        String datePrefix = java.time.LocalDate.now().toString().replace("-", "/");
        String objectName = category + "/" + datePrefix + "/" + fileName;
        return upload(objectName, data, contentType);
    }

    /**
     * 删除文件
     */
    public boolean delete(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build());
            log.info("文件删除成功: object={}", objectName);
            return true;
        } catch (Exception e) {
            log.error("MinIO 文件删除失败: object={}, error={}", objectName, e.getMessage());
            return false;
        }
    }
}
