package com.hospital.service;

import com.hospital.common.result.Result;
import com.hospital.dto.response.AreaInfoDTO;

import java.util.List;

/**
 * 区域服务接口
 * 用于获取省市区数据
 *
 * @author Hospital Team
 * @since 2025-12-19
 */
public interface AreaService {

    /**
     * 获取省份列表
     *
     * @return 省份列表
     */
    Result<List<AreaInfoDTO>> getProvinceList();

    /**
     * 根据省份ID获取城市列表
     *
     * @param provinceId 省份ID
     * @return 城市列表
     */
    Result<List<AreaInfoDTO>> getCityList(String provinceId);

    /**
     * 根据城市ID获取区县列表
     *
     * @param cityId 城市ID
     * @return 区县列表
     */
    Result<List<AreaInfoDTO>> getCountyList(String cityId);
}

