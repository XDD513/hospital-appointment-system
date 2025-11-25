package com.hospital.controller;

import com.hospital.annotation.OperationLog;
import com.hospital.common.result.Result;
import com.hospital.service.OssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * OSS服务控制器
 *
 * @author Hospital Team
 */
@Slf4j
@RestController
@RequestMapping("/api/oss")
public class OssController {

    @Autowired
    private OssService ossService;

    /**
     * 生成带签名的URL
     *
     * @param url 原始URL
     * @param expirationMinutes 过期时间（分钟），默认60分钟
     * @return 带签名的URL
     */
    @GetMapping("/presigned-url")
    public Result<String> generatePresignedUrl(
            @RequestParam String url,
            @RequestParam(defaultValue = "60") int expirationMinutes) {
        try {
            if (url == null || url.isEmpty()) {
                return Result.error(400, "URL不能为空");
            }

            // 限制过期时间范围（1分钟到24小时）
            if (expirationMinutes < 1 || expirationMinutes > 1440) {
                expirationMinutes = 60;
            }

            String signedUrl = ossService.generatePresignedUrl(url, expirationMinutes);
            if (signedUrl == null) {
                return Result.error(500, "生成签名URL失败");
            }
            return Result.success("生成成功", signedUrl);
        } catch (Exception e) {
            log.error("生成签名URL失败: url={}, error={}", url, e.getMessage(), e);
            return Result.error(500, "生成签名URL失败: " + e.getMessage());
        }
    }
}

