package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 健康计划记录实体类
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Data
@TableName("health_plan_record")
public class HealthPlanRecord implements Serializable {

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
     * 计划类型（DIET-饮食，EXERCISE-运动，ACUPOINT-穴位按摩，RECIPE-药膳，SLEEP-睡眠，OTHER-其他）
     */
    private String planType;

    /**
     * 计划名称
     */
    private String planName;

    /**
     * 计划描述
     */
    private String description;

    /**
     * 目标内容（JSON对象）
     */
    private String targetContent;

    /**
     * 开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 执行频率（DAILY-每日，WEEKLY-每周，MONTHLY-每月）
     */
    private String frequency;

    /**
     * 提醒时间（JSON数组）
     */
    private String reminderTime;

    /**
     * 完成次数
     */
    private Integer completedCount;

    /**
     * 目标次数
     */
    private Integer targetCount;

    /**
     * 完成率（%）
     */
    private Double completionRate;

    /**
     * 状态（0-未开始 1-进行中 2-已完成 3-已放弃）
     */
    private Integer status;

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

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}

