package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 健康打卡实体类
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Data
@TableName("health_checkin")
public class HealthCheckin implements Serializable {

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
     * 计划ID（可选）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long planId;

    /**
     * 打卡日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkinDate;

    /**
     * 打卡类型（DIET-饮食，EXERCISE-运动，ACUPOINT-穴位按摩，RECIPE-药膳，SLEEP-睡眠，MOOD-心情，OTHER-其他）
     */
    private String checkinType;

    /**
     * 打卡内容（JSON对象）
     */
    private String content;

    /**
     * 体重（kg）
     */
    private Double weight;

    /**
     * 血压（收缩压/舒张压）
     */
    private String bloodPressure;

    /**
     * 心率（次/分）
     */
    private Integer heartRate;

    /**
     * 睡眠时长（小时）
     */
    private Double sleepDuration;

    /**
     * 睡眠质量评分（1-5分）
     */
    private Integer sleepQuality;

    /**
     * 运动时长（分钟）
     */
    private Integer exerciseDuration;

    /**
     * 饮水量（ml）
     */
    private Integer waterIntake;

    /**
     * 心情评分（1-5分）
     */
    private Integer moodScore;

    /**
     * 身体状况评分（1-5分）
     */
    private Integer healthScore;

    /**
     * 备注
     */
    private String remark;

    /**
     * 图片（逗号分隔）
     */
    private String images;

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

