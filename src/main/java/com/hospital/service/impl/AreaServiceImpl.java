package com.hospital.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.hospital.common.constant.AreaConstants;
import com.hospital.common.constant.CacheConstants;
import com.hospital.common.result.Result;
import com.hospital.config.AreaConfig;
import com.hospital.dto.response.AreaInfoDTO;
import com.hospital.dto.response.AreaResponse;
import com.hospital.service.AreaService;
import com.hospital.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 区域服务实现类
 *
 * @author Hospital Team
 * @since 2025-12-19
 */
@Slf4j
@Service
public class AreaServiceImpl implements AreaService {

    @Autowired
    private AreaConfig areaConfig;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 获取省份列表
     */
    @Override
    public Result<List<AreaInfoDTO>> getProvinceList() {
        try {
            // 1. 尝试从缓存获取
            String cacheKey = CacheConstants.AREA_CACHE_PREFIX + "province:list";
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<AreaInfoDTO> cachedList = (List<AreaInfoDTO>) cached;
                    return Result.success(cachedList);
                } catch (ClassCastException ignored) {}
            }

            // 2. 调用阿里云API获取省份列表（不传参数获取省份）
            List<AreaInfoDTO> provinceList = callAreaApi(null, null, null);

            // 3. 缓存数据
            if (provinceList != null && !provinceList.isEmpty()) {
                redisUtil.set(cacheKey, provinceList, areaConfig.getCacheTtlSeconds(), TimeUnit.SECONDS);
            }

            return Result.success(provinceList);
        } catch (Exception e) {
            log.error("获取省份列表失败", e);
            return Result.error("获取省份列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据省份ID获取城市列表
     */
    @Override
    public Result<List<AreaInfoDTO>> getCityList(String provinceId) {
        if (!StringUtils.hasText(provinceId)) {
            return Result.error("省份ID不能为空");
        }

        try {
            // 1. 尝试从缓存获取
            String cacheKey = CacheConstants.AREA_CACHE_PREFIX + "city:list:province:" + provinceId;
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<AreaInfoDTO> cachedList = (List<AreaInfoDTO>) cached;
                    return Result.success(cachedList);
                } catch (ClassCastException ignored) {}
            }

            // 2. 调用阿里云API获取城市列表
            List<AreaInfoDTO> cityList = callAreaApi(provinceId, null, null);

            // 3. 缓存数据
            if (cityList != null && !cityList.isEmpty()) {
                redisUtil.set(cacheKey, cityList, areaConfig.getCacheTtlSeconds(), TimeUnit.SECONDS);
            }

            return Result.success(cityList);
        } catch (Exception e) {
            log.error("获取城市列表失败: provinceId={}", provinceId, e);
            return Result.error("获取城市列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据城市ID获取区县列表
     */
    @Override
    public Result<List<AreaInfoDTO>> getCountyList(String cityId) {
        if (!StringUtils.hasText(cityId)) {
            return Result.error("城市ID不能为空");
        }

        try {
            // 1. 尝试从缓存获取
            String cacheKey = CacheConstants.AREA_CACHE_PREFIX + "county:list:city:" + cityId;
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<AreaInfoDTO> cachedList = (List<AreaInfoDTO>) cached;
                    return Result.success(cachedList);
                } catch (ClassCastException ignored) {}
            }

            // 2. 调用阿里云API获取区县列表
            List<AreaInfoDTO> countyList = callAreaApi(null, cityId, null);

            // 3. 缓存数据
            if (countyList != null && !countyList.isEmpty()) {
                redisUtil.set(cacheKey, countyList, areaConfig.getCacheTtlSeconds(), TimeUnit.SECONDS);
            }

            return Result.success(countyList);
        } catch (Exception e) {
            log.error("获取区县列表失败: cityId={}", cityId, e);
            return Result.error("获取区县列表失败: " + e.getMessage());
        }
    }


    /**
     * 调用阿里云区域API
     * 只支持省市区三级数据
     *
     * @param provinceId 省份ID
     * @param cityId     城市ID
     * @param countyId   区县ID
     * @return 区域信息列表
     */
    private List<AreaInfoDTO> callAreaApi(String provinceId, String cityId, String countyId) {
        try {
            // 构建请求URL
            String url = areaConfig.getHost() + areaConfig.getPath();

            // 构建请求参数
            HttpRequest request = HttpUtil.createGet(url);

            // 设置请求头（使用AppCode认证）
            request.header(AreaConstants.AUTH_HEADER_NAME, AreaConstants.APPCODE_PREFIX + areaConfig.getAppCode());

            // 添加查询参数
            if (StringUtils.hasText(provinceId)) {
                request.form(AreaConstants.PARAM_PROVINCE_ID, provinceId);
            }
            if (StringUtils.hasText(cityId)) {
                request.form(AreaConstants.PARAM_CITY_ID, cityId);
            }
            if (StringUtils.hasText(countyId)) {
                request.form(AreaConstants.PARAM_COUNTY_ID, countyId);
            }

            // 执行请求
            log.info("调用阿里云区域API: url={}, provinceId={}, cityId={}, countyId={}",
                    url, provinceId, cityId, countyId);

            String responseBody = request.execute().body();

            // 解析响应
            AreaResponse areaResponse = JSON.parseObject(responseBody, AreaResponse.class);

            if (areaResponse == null || areaResponse.getCode() == null || areaResponse.getCode() != 200) {
                log.error("阿里云区域API调用失败: code={}, msg={}", 
                        areaResponse != null ? areaResponse.getCode() : "null",
                        areaResponse != null ? areaResponse.getMsg() : "响应为空");
                return new ArrayList<>();
            }

            // 转换为DTO列表
            if (areaResponse.getData() == null || areaResponse.getData().getList() == null) {
                return new ArrayList<>();
            }

            // 根据传入的参数判断应该返回哪个级别的数据
            // 只支持省市区三级：countyId > cityId > provinceId > null（省份列表）
            int targetLevel;
            if (StringUtils.hasText(countyId)) {
                targetLevel = 3; // 获取区县列表（不应该到这里，因为区县是最后一级）
            } else if (StringUtils.hasText(cityId)) {
                targetLevel = 3; // 获取区县列表
            } else if (StringUtils.hasText(provinceId)) {
                targetLevel = 2; // 获取城市列表
            } else {
                targetLevel = 1; // 获取省份列表
            }

            final int finalTargetLevel = targetLevel;
            List<AreaInfoDTO> areaList = areaResponse.getData().getList().stream()
                    .map(item -> {
                        AreaInfoDTO dto = new AreaInfoDTO();
                        
                        // 根据目标级别提取对应的ID和名称（只处理省市区三级）
                        switch (finalTargetLevel) {
                            case 1: // 省份
                                if (StringUtils.hasText(item.getProvinceId()) && StringUtils.hasText(item.getProvinceName())) {
                                    dto.setId(item.getProvinceId());
                                    dto.setName(item.getProvinceName());
                                    dto.setLevel(1);
                                    dto.setParentId(null);
                                }
                                break;
                            case 2: // 城市
                                if (StringUtils.hasText(item.getCityId()) && StringUtils.hasText(item.getCityName())) {
                                    dto.setId(item.getCityId());
                                    dto.setName(item.getCityName());
                                    dto.setLevel(2);
                                    dto.setParentId(item.getProvinceId());
                                }
                                break;
                            case 3: // 区县
                                if (StringUtils.hasText(item.getCountyId()) && StringUtils.hasText(item.getCountyName())) {
                                    dto.setId(item.getCountyId());
                                    dto.setName(item.getCountyName());
                                    dto.setLevel(3);
                                    dto.setParentId(item.getCityId());
                                }
                                break;
                        }
                        
                        // 设置为叶子节点（区县级别是最后一级）
                        dto.setIsLeaf(dto.getLevel() != null && dto.getLevel() >= 3);
                        
                        return dto;
                    })
                    .filter(dto -> dto.getId() != null && dto.getName() != null)
                    .collect(Collectors.toList());

            return areaList;

        } catch (Exception e) {
            log.error("调用阿里云区域API异常", e);
            throw new RuntimeException("调用区域API失败: " + e.getMessage(), e);
        }
    }
}

