package com.hospital.controller;

import com.hospital.common.result.Result;
import com.hospital.dto.response.AreaInfoDTO;
import com.hospital.service.AreaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 区域管理控制器
 * 提供省市区数据查询接口
 *
 * @author Hospital Team
 * @since 2025-12-19
 */
@Slf4j
@RestController
@RequestMapping("/api/area")
public class AreaController {

    @Autowired
    private AreaService areaService;

    /**
     * 获取省份列表
     */
    @GetMapping("/provinces")
    public Result<List<AreaInfoDTO>> getProvinceList() {
        log.info("查询省份列表");
        return areaService.getProvinceList();
    }

    /**
     * 根据省份ID获取城市列表
     *
     * @param provinceId 省份ID
     */
    @GetMapping("/cities")
    public Result<List<AreaInfoDTO>> getCityList(@RequestParam String provinceId) {
        log.info("查询城市列表: provinceId={}", provinceId);
        return areaService.getCityList(provinceId);
    }

    /**
     * 根据城市ID获取区县列表
     *
     * @param cityId 城市ID
     */
    @GetMapping("/counties")
    public Result<List<AreaInfoDTO>> getCountyList(@RequestParam String cityId) {
        log.info("查询区县列表: cityId={}", cityId);
        return areaService.getCountyList(cityId);
    }
}

