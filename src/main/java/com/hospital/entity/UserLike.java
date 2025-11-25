package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户点赞实体类
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Data
@TableName("user_like")
public class UserLike implements Serializable {

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
     * 目标ID（文章ID或评论ID）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long targetId;

    /**
     * 目标类型（ARTICLE-文章，COMMENT-评论）
     */
    private String targetType;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}

