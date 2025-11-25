package com.hospital.controller;

import com.hospital.common.result.Result;
import com.hospital.dto.response.FrontendRuntimeConfig;
import com.hospital.service.FrontendConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 为前端提供运行时配置，统一来源于 Nacos。
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class FrontendConfigController {

    private final FrontendConfigService frontendConfigService;

    @GetMapping
    public Result<FrontendRuntimeConfig> getRuntimeConfig() {
        return Result.success(frontendConfigService.getRuntimeConfig());
    }
}

