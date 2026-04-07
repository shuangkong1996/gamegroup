package com.gamegroup.service;

import com.gamegroup.config.MinioConfig;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * 上传文件
     * @param file 文件
     * @return 文件访问 URL (预签名 URL 或 文件名，视需求而定，这里返回文件名，配合 getUrl 使用)
     */
    public String uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : ".jpg";
        String fileName = UUID.randomUUID().toString() + extension;

        try {
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return fileName;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件访问 URL
     * 如果配置了外部地址，返回永久公共 URL (假设 bucket 是 public 的或通过 nginx 代理)
     * 否则返回 7 天有效的预签名 URL
     */
    public String getFileUrl(String fileName) {
        if (minioConfig.getExternalEndpoint() != null && !minioConfig.getExternalEndpoint().isEmpty()) {
            // 直接拼接: externalEndpoint + "/" + bucketName + "/" + fileName
            // 注意处理斜杠
            String host = minioConfig.getExternalEndpoint();
            if (host.endsWith("/")) {
                host = host.substring(0, host.length() - 1);
            }
            return String.format("%s/%s/%s", host, minioConfig.getBucketName(), fileName);
        }

        try {
            // 没有配置外部地址，使用 MinIO 预签名 URL (带签名参数，适用于私有 bucket)
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
            return url;
        } catch (Exception e) {
            log.error("获取文件URL失败", e);
            return "";
        }
    }

    /**
     * 下载文件流 (供后端处理用)
     */
    public InputStream getFileStream(String fileName) {
        try {
            return minioClient.getObject(
                    io.minio.GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            log.error("获取文件流失败", e);
            throw new RuntimeException("获取文件流失败");
        }
    }
    
    /**
     * 上传流 (供后端生成的图片上传)
     */
    public String uploadStream(InputStream stream, String contentType) {
        String fileName = UUID.randomUUID().toString() + ".jpg";
        try {
            // 需要先读取流计算大小，或者使用 -1 让 MinIO 客户端处理（需要足够内存缓冲）
            // 这里简单处理，假设内存足够
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .stream(stream, -1, 10485760) // -1 unknown size, part size 10MB
                            .contentType(contentType)
                            .build()
            );
            return fileName;
        } catch (Exception e) {
            log.error("流上传失败", e);
            throw new RuntimeException("流上传失败");
        }
    }

    /**
     * 从 URL 解析并下载文件流
     * 如果是本服务签发的 MinIO URL，尝试解析并直接下载
     */
    public InputStream getStreamFromUrl(String url) {
        try {
            // 简单判断：如果包含 bucketName，尝试提取 objectName
            // 更好的做法是正则匹配，这里做简单假设：
            // URL 格式: http://host:port/bucket/filename?params...
            // 或者 http://host:port/bucket/filename
            if (url.contains(minioConfig.getBucketName())) {
                String afterBucket = url.substring(url.indexOf(minioConfig.getBucketName()) + minioConfig.getBucketName().length());
                // afterBucket 可能是 /filename?algo... 或 /filename
                String fileName;
                if (afterBucket.startsWith("/")) {
                    fileName = afterBucket.substring(1);
                } else {
                    fileName = afterBucket;
                }
                
                if (fileName.contains("?")) {
                    fileName = fileName.substring(0, fileName.indexOf("?"));
                }
                
                // 去掉可能的路径分隔符
                if (fileName.contains("/")) {
                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                }

                log.info("从 URL 解析文件名: {}", fileName);
                return getFileStream(fileName);
            } else {
                // 外部 URL，直接下载
                return new java.net.URL(url).openStream();
            }
        } catch (Exception e) {
            log.error("从 URL 下载失败: {}", url, e);
            throw new RuntimeException("图片下载失败");
        }
    }
}
