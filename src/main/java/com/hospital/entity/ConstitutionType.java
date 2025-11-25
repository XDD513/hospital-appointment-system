package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 中医体质类型实体类
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
@Data
@TableName("constitution_type")
public class ConstitutionType implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 体质类型ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 体质代码（如：PINGHE）
     */
    private String typeCode;

    /**
     * 体质名称（如：平和质）
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

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}

