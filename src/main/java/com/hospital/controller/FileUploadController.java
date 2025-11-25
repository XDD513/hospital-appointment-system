package com.hospital.controller;

import com.hospital.annotation.OperationLog;
import com.hospital.common.result.Result;
import com.hospital.service.OssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 *
 * @author Hospital Team
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Autowired
    private OssService ossService;

    /**
     * 上传头像
     */
    @OperationLog(module = "FILE", type = "INSERT", description = "上传头像")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件
            if (file == null || file.isEmpty()) {
                return Result.error(400, "文件不能为空");
            }

            // 验证文件类型
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return Result.error(400, "文件名不能为空");
            }

            String extension = "";
            int lastDotIndex = originalFilename.lastIndexOf(".");
            if (lastDotIndex > 0) {
                extension = originalFilename.substring(lastDotIndex).toLowerCase();
            }

            // 只允许图片格式
            if (!extension.matches("\\.(jpg|jpeg|png|gif|bmp|webp)")) {
                return Result.error(400, "只支持图片格式：jpg、jpeg、png、gif、bmp、webp");
            }

            // 验证文件大小（限制5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                return Result.error(400, "文件大小不能超过5MB");
            }

            // 上传到OSS
            String fileUrl = ossService.uploadFile(file, "avatar/");
            log.info("头像上传成功: url={}", fileUrl);
            return Result.success("上传成功", fileUrl);

        } catch (Exception e) {
            log.error("头像上传失败: {}", e.getMessage(), e);
            return Result.error(500, "上传失败: " + e.getMessage());
        }
    }
}

