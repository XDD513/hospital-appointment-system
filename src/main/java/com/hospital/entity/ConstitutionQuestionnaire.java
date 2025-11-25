package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 中医体质测试问卷实体类
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
@Data
@TableName("constitution_questionnaire")
public class ConstitutionQuestionnaire implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 问题ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 所属体质类型ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long constitutionTypeId;

    /**
     * 问题文本
     */
    private String questionText;

    /**
     * 问题顺序
     */
    private Integer questionOrder;

    /**
     * 问题分类（如：精神状态、寒热、饮食等）
     */
    private String category;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}

