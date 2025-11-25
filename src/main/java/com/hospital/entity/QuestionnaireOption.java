package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 问卷选项实体类
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
@Data
@TableName("questionnaire_option")
public class QuestionnaireOption implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 选项ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 所属问题ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long questionId;

    /**
     * 选项文本
     */
    private String optionText;

    /**
     * 选项分值（1-5分）
     */
    private Integer score;

    /**
     * 选项顺序
     */
    private Integer optionOrder;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}

