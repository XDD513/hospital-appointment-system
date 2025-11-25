package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 食材库实体类
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
@Data
@TableName("ingredient")
public class Ingredient implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 食材ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 食材名称
     */
    private String name;

    /**
     * 食材分类（谷物/肉类/蔬菜/药材等）
     */
    private String category;

    /**
     * 性味（寒/凉/平/温/热）
     */
    private String properties;

    /**
     * 味道（甘/辛/酸/苦/咸）
     */
    private String flavor;

    /**
     * 归经
     */
    private String meridian;

    /**
     * 功效说明
     */
    private String efficacy;

    /**
     * 适用体质（逗号分隔）
     */
    private String suitableConstitution;

    /**
     * 不适用体质（逗号分隔）
     */
    private String unsuitableConstitution;

    /**
     * 食材图片
     */
    private String image;

    /**
     * 状态（1-启用，0-禁用）
     */
    private Integer status;

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

