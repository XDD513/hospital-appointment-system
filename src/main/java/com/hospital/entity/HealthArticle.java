package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 养生文章实体类
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Data
@TableName("health_article")
public class HealthArticle implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章内容
     */
    private String content;

    /**
     * 文章摘要
     */
    private String summary;

    /**
     * 封面图片
     */
    private String coverImage;

    /**
     * 作者ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long authorId;

    /**
     * 作者姓名（非数据库字段，用于前端展示）
     */
    @TableField(exist = false)
    private String authorName;

    /**
     * 文章分类（CONSTITUTION-体质养生，DIET-饮食养生，EXERCISE-运动养生，ACUPOINT-穴位养生，SEASON-时令养生，OTHER-其他）
     */
    private String category;

    /**
     * 标签（逗号分隔）
     */
    private String tags;

    /**
     * 适用体质（逗号分隔）
     */
    private String constitutionType;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 点赞次数
     */
    private Integer likeCount;

    /**
     * 收藏次数
     */
    private Integer favoriteCount;

    /**
     * 评论次数
     */
    private Integer commentCount;

    /**
     * 是否置顶（0-否 1-是）
     */
    private Integer isTop;

    /**
     * 是否精选（0-否 1-是）
     */
    private Integer isFeatured;

    /**
     * 状态（0-草稿 1-已发布 2-已下架）
     */
    private Integer status;

    /**
     * 发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

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

