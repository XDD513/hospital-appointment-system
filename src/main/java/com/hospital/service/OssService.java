package com.hospital.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * OSS服务接口
 *
 * @author Hospital Team
 */
public interface OssService {

    /**
     * 上传文件到OSS
     *
     * @param file 文件
     * @param folder 文件夹路径（如：avatar/）
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file, String folder);

    /**
     * 删除OSS中的文件
     *
     * @param fileUrl 文件URL
     * @return 是否删除成功
     */
    boolean deleteFile(String fileUrl);

    /**
     * 生成带签名的URL（用于私有Bucket访问）
     *
     * @param fileUrl 文件URL
     * @param expirationMinutes 过期时间（分钟），默认60分钟
     * @return 带签名的URL
     */
    String generatePresignedUrl(String fileUrl, int expirationMinutes);
}

