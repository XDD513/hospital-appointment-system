package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 药膳食谱实体类
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
@Data
@TableName("herbal_recipe")
public class HerbalRecipe implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 食谱ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 食谱名称
     */
    private String recipeName;

    /**
     * 适用体质（逗号分隔）
     */
    private String constitutionType;

    /**
     * 适用季节（SPRING/SUMMER/AUTUMN/WINTER/ALL，可逗号分隔多个季节）
     */
    private String season;

    /**
     * 食谱分类（汤品/粥品/茶饮/炒菜/甜品等）
     */
    private String category;

    /**
     * 难度等级（1-5）
     */
    private Integer difficulty;

    /**
     * 烹饪时间（分钟）
     */
    private Integer cookingTime;

    /**
     * 份量（人份）
     */
    private Integer servings;

    /**
     * 食材清单（JSON数组）
     */
    private String ingredients;

    /**
     * 制作步骤（JSON数组）
     */
    private String steps;

    /**
     * 功效说明
     */
    private String efficacy;

    /**
     * 适用症状
     */
    private String suitableSymptoms;

    /**
     * 禁忌说明
     */
    private String contraindications;

    /**
     * 营养信息（JSON格式）
     */
    private String nutritionInfo;

    /**
     * 烹饪小贴士
     */
    private String tips;

    /**
     * 食谱图片
     */
    private String image;

    /**
     * 视频链接
     */
    private String videoUrl;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 收藏次数
     */
    private Integer favoriteCount;

    /**
     * 当前用户是否已收藏（非数据库字段）
     */
    @TableField(exist = false)
    private Boolean isFavorited;

    /**
     * AI生成的个性化推荐理由（非数据库字段）
     */
    @TableField(exist = false)
    private String recommendationReason;

    /**
     * 状态（1-发布，0-下架）
     */
    private Integer status;

    /**
     * 版本号（乐观锁）
     */
    @Version
    private Integer version;

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

