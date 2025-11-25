package com.hospital.service;

import com.hospital.common.result.Result;
import com.hospital.dto.response.ConstitutionTypeResponse;

import java.util.List;

/**
 * 体质类型服务接口
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
public interface ConstitutionService {

    /**
     * 获取所有体质类型列表
     *
     * @return 体质类型列表
     */
    Result<List<ConstitutionTypeResponse>> getConstitutionTypes();

    /**
     * 根据体质代码获取体质详情
     *
     * @param typeCode 体质代码
     * @return 体质详情
     */
    Result<ConstitutionTypeResponse> getConstitutionDetail(String typeCode);
}

