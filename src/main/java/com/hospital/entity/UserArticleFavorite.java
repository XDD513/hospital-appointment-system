package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户收藏文章实体类
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Data
@TableName("user_article_favorite")
public class UserArticleFavorite implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 用户ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;

    /**
     * 文章ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long articleId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // ========== 以下字段来自关联查询，非数据库字段 ==========

    /**
     * 文章标题（来自health_article表）
     */
    @TableField(exist = false)
    private String title;

    /**
     * 文章摘要（来自health_article表）
     */
    @TableField(exist = false)
    private String summary;

    /**
     * 封面图片（来自health_article表）
     */
    @TableField(value = "cover_image", exist = false)
    private String coverImage;

    /**
     * 作者姓名（来自user表，非数据库字段）
     */
    @TableField(value = "author_name", exist = false)
    private String authorName;

    /**
     * 文章分类（来自health_article表）
     */
    @TableField(exist = false)
    private String category;

    /**
     * 浏览次数（来自health_article表）
     */
    @TableField(value = "view_count", exist = false)
    private Integer viewCount;

    /**
     * 点赞次数（来自health_article表）
     */
    @TableField(value = "like_count", exist = false)
    private Integer likeCount;
}

