package com.hospital.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.PutObjectRequest;
import com.hospital.config.OssConfig;
import com.hospital.service.OssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

/**
 * OSS服务实现类
 *
 * @author Hospital Team
 */
@Slf4j
@Service
public class OssServiceImpl implements OssService {

    @Autowired
    private OssConfig ossConfig;

    /**
     * 上传文件到OSS
     */
    @Override
    public String uploadFile(MultipartFile file, String folder) {
        OSS ossClient = null;
        try {
            // 创建OSS客户端
            ossClient = new OSSClientBuilder().build(
                    ossConfig.getEndpoint(),
                    ossConfig.getAccessKeyId(),
                    ossConfig.getAccessKeySecret()
            );

            // 获取原始文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new RuntimeException("文件名不能为空");
            }

            // 获取文件扩展名
            String extension = "";
            int lastDotIndex = originalFilename.lastIndexOf(".");
            if (lastDotIndex > 0) {
                extension = originalFilename.substring(lastDotIndex);
            }

            // 生成唯一文件名
            String fileName = folder + UUID.randomUUID().toString() + extension;

            // 上传文件 - 使用try-with-resources确保流被正确关闭
            try (InputStream inputStream = file.getInputStream()) {
                PutObjectRequest putObjectRequest = new PutObjectRequest(
                        ossConfig.getBucketName(),
                        fileName,
                        inputStream
                );
                ossClient.putObject(putObjectRequest);
            }

            // 构建文件访问URL
            String fileUrl = ossConfig.getUrlProtocol() + "://" + 
                             ossConfig.getBucketName() + "." + 
                             ossConfig.getEndpoint() + "/" + 
                             fileName;

            log.info("文件上传成功: fileName={}, url={}", fileName, fileUrl);
            return fileUrl;

        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 删除OSS中的文件
     */
    @Override
    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return false;
        }

        OSS ossClient = null;
        try {
            // 创建OSS客户端
            ossClient = new OSSClientBuilder().build(
                    ossConfig.getEndpoint(),
                    ossConfig.getAccessKeyId(),
                    ossConfig.getAccessKeySecret()
            );

            // 从URL中提取文件路径
            // URL格式: https://bucket-name.endpoint/file-path
            String bucketEndpoint = ossConfig.getBucketName() + "." + ossConfig.getEndpoint();
            String filePath;
            
            if (fileUrl.contains(bucketEndpoint)) {
                filePath = fileUrl.substring(fileUrl.indexOf(bucketEndpoint) + bucketEndpoint.length() + 1);
            } else if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
                // 如果URL格式不标准，尝试从末尾提取
                int lastSlashIndex = fileUrl.lastIndexOf("/");
                if (lastSlashIndex > 0) {
                    filePath = fileUrl.substring(lastSlashIndex + 1);
                } else {
                    log.warn("无法从URL中提取文件路径: {}", fileUrl);
                    return false;
                }
            } else {
                // 直接是文件路径
                filePath = fileUrl;
            }

            // 删除文件
            ossClient.deleteObject(ossConfig.getBucketName(), filePath);
            log.info("文件删除成功: filePath={}", filePath);
            return true;

        } catch (Exception e) {
            log.error("文件删除失败: url={}, error={}", fileUrl, e.getMessage(), e);
            return false;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 生成带签名的URL（用于私有Bucket访问）
     */
    @Override
    public String generatePresignedUrl(String fileUrl, int expirationMinutes) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }

        // 如果URL不是OSS URL，直接返回
        if (!fileUrl.contains(ossConfig.getBucketName() + "." + ossConfig.getEndpoint())) {
            return fileUrl;
        }

        OSS ossClient = null;
        try {
            // 创建OSS客户端
            ossClient = new OSSClientBuilder().build(
                    ossConfig.getEndpoint(),
                    ossConfig.getAccessKeyId(),
                    ossConfig.getAccessKeySecret()
            );

            // 从URL中提取文件路径
            // URL格式: https://bucket-name.endpoint/file-path
            String filePath;
            
            // 移除协议前缀
            String urlWithoutProtocol = fileUrl;
            if (fileUrl.startsWith("https://")) {
                urlWithoutProtocol = fileUrl.substring(8);
            } else if (fileUrl.startsWith("http://")) {
                urlWithoutProtocol = fileUrl.substring(7);
            }
            
            // 查找bucket和endpoint后的路径
            String bucketEndpoint = ossConfig.getBucketName() + "." + ossConfig.getEndpoint();
            int bucketIndex = urlWithoutProtocol.indexOf(bucketEndpoint);
            
            if (bucketIndex >= 0) {
                // 找到bucket位置，提取后面的路径
                int pathStart = bucketIndex + bucketEndpoint.length();
                if (pathStart < urlWithoutProtocol.length() && urlWithoutProtocol.charAt(pathStart) == '/') {
                    pathStart++; // 跳过斜杠
                }
                filePath = urlWithoutProtocol.substring(pathStart);
            } else {
                // 如果找不到bucket，尝试从最后一个斜杠提取文件名
                int lastSlashIndex = urlWithoutProtocol.lastIndexOf("/");
                if (lastSlashIndex > 0 && lastSlashIndex < urlWithoutProtocol.length() - 1) {
                    filePath = urlWithoutProtocol.substring(lastSlashIndex + 1);
                } else {
                    log.warn("无法从URL中提取文件路径: {}", fileUrl);
                    return fileUrl;
                }
            }
            

            // 设置过期时间
            Date expiration = new Date(new Date().getTime() + expirationMinutes * 60 * 1000L);
            
            // 生成签名URL
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    ossConfig.getBucketName(),
                    filePath
            );
            request.setExpiration(expiration);
            request.setMethod(com.aliyun.oss.HttpMethod.GET);
            
            URL signedUrl = ossClient.generatePresignedUrl(request);
            String signedUrlStr = signedUrl.toString();
            if ("https".equalsIgnoreCase(ossConfig.getUrlProtocol()) && signedUrlStr.startsWith("http://")) {
                signedUrlStr = signedUrlStr.replaceFirst("http://", "https://");
            }
            
            log.info("生成签名URL成功: filePath={}, expirationMinutes={}", filePath, expirationMinutes);
            return signedUrlStr;

        } catch (Exception e) {
            log.error("生成签名URL失败: url={}, error={}", fileUrl, e.getMessage(), e);
            return fileUrl; // 失败时返回原URL
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}

