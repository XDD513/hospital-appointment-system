package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 穴位信息实体类
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Data
@TableName("acupoint")
public class Acupoint implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 穴位名称
     */
    private String acupointName;

    /**
     * 拼音
     */
    private String pinyin;

    /**
     * 英文名称
     */
    private String englishName;

    /**
     * 所属经络
     */
    private String meridian;

    /**
     * 定位描述
     */
    private String location;

    /**
     * 穴位分类
     */
    private String category;

    /**
     * 主治功效
     */
    @TableField("efficacy")
    private String indications;

    /**
     * 按摩方法
     */
    private String massageMethod;

    /**
     * 注意事项
     */
    private String precautions;

    /**
     * 适用体质（逗号分隔）
     */
    private String constitutionType;

    /**
     * 穴位图片URL
     */
    @TableField("image_url")
    private String image;

    /**
     * 视频教学链接
     */
    private String videoUrl;

    /**
     * 浏览次数
     */
    private Integer viewCount;

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

