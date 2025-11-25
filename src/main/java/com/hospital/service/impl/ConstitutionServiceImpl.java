package com.hospital.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hospital.common.result.Result;
import com.hospital.common.result.ResultCode;
import com.hospital.dto.response.ConstitutionTypeResponse;
import com.hospital.entity.ConstitutionType;
import com.hospital.mapper.ConstitutionTypeMapper;
import com.hospital.common.constant.CacheConstants;
import com.hospital.service.ConstitutionService;
import com.hospital.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 体质类型服务实现类
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
@Slf4j
@Service
public class ConstitutionServiceImpl implements ConstitutionService {

    @Autowired
    private ConstitutionTypeMapper constitutionTypeMapper;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 获取所有体质类型列表
     */
    @Override
    public Result<List<ConstitutionTypeResponse>> getConstitutionTypes() {
        try {
            // 1. 尝试从缓存获取
            Object cached = redisUtil.get(CacheConstants.CONSTITUTION_TYPES_CACHE_KEY);
            if (cached != null) {
                log.info("从缓存获取体质类型列表");
                @SuppressWarnings("unchecked")
                List<ConstitutionTypeResponse> cachedList = (List<ConstitutionTypeResponse>) cached;
                return Result.success(cachedList);
            }

            // 2. 从数据库查询
            List<ConstitutionType> constitutionTypes = constitutionTypeMapper.selectAllOrdered();
            
            // 3. 转换为DTO
            List<ConstitutionTypeResponse> responseList = constitutionTypes.stream()
                    .map(type -> BeanUtil.copyProperties(type, ConstitutionTypeResponse.class))
                    .collect(Collectors.toList());

            // 4. 存入缓存（永久）
            redisUtil.set(CacheConstants.CONSTITUTION_TYPES_CACHE_KEY, responseList);

            log.info("成功获取体质类型列表，共{}种体质", responseList.size());
            return Result.success(responseList);

        } catch (Exception e) {
            log.error("获取体质类型列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 根据体质代码获取体质详情
     */
    @Override
    public Result<ConstitutionTypeResponse> getConstitutionDetail(String typeCode) {
        try {
            // 1. 尝试从缓存获取
            String cacheKey = CacheConstants.CONSTITUTION_TYPE_CACHE_PREFIX + typeCode;
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                log.info("从缓存获取体质详情: {}", typeCode);
                return Result.success((ConstitutionTypeResponse) cached);
            }

            // 2. 从数据库查询
            ConstitutionType constitutionType = constitutionTypeMapper.selectByTypeCode(typeCode);
            if (constitutionType == null) {
                log.warn("体质类型不存在: {}", typeCode);
                return Result.error(ResultCode.DATA_NOT_FOUND);
            }

            // 3. 转换为DTO
            ConstitutionTypeResponse response = BeanUtil.copyProperties(constitutionType, ConstitutionTypeResponse.class);

            // 4. 存入缓存（永久）
            redisUtil.set(cacheKey, response);

            log.info("成功获取体质详情: {}", typeCode);
            return Result.success(response);

        } catch (Exception e) {
            log.error("获取体质详情失败: {}", typeCode, e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }
}

