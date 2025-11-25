package com.hospital.dto.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 体质类型响应DTO
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
@Data
public class ConstitutionTypeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 体质类型ID
     */
    private Long id;

    /**
     * 体质代码
     */
    private String typeCode;

    /**
     * 体质名称
     */
    private String typeName;

    /**
     * 体质描述
     */
    private String description;

    /**
     * 主要特征
     */
    private String characteristics;

    /**
     * 易患疾病
     */
    private String susceptibleDiseases;

    /**
     * 养生建议
     */
    private String healthAdvice;

    /**
     * 饮食建议
     */
    private String dietAdvice;

    /**
     * 运动建议
     */
    private String exerciseAdvice;

    /**
     * 情志调节建议
     */
    private String emotionAdvice;

    /**
     * 体质图标
     */
    private String icon;

    /**
     * 主题颜色
     */
    private String color;

    /**
     * 排序顺序
     */
    private Integer sortOrder;
}

