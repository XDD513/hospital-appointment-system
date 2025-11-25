package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户健康档案实体类
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Data
@TableName("user_health_profile")
public class UserHealthProfile implements Serializable {

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
     * 身高（cm）
     */
    private Integer height;

    /**
     * 体重（kg）
     */
    private Double weight;

    /**
     * BMI指数
     */
    private Double bmi;

    /**
     * 血型
     */
    private String bloodType;

    /**
     * 过敏史（JSON数组）
     */
    private String allergies;

    /**
     * 既往病史（JSON数组）
     */
    private String medicalHistory;

    /**
     * 家族病史（JSON数组）
     */
    private String familyHistory;

    /**
     * 当前用药（JSON数组）
     */
    private String currentMedications;

    /**
     * 生活习惯（JSON对象）
     */
    private String lifestyle;

    /**
     * 饮食偏好（JSON对象）
     */
    private String dietPreference;

    /**
     * 运动习惯（JSON对象）
     */
    private String exerciseHabit;

    /**
     * 睡眠质量（1-5分）
     */
    private Integer sleepQuality;

    /**
     * 压力水平（1-5分）
     */
    private Integer stressLevel;

    /**
     * 最近体检日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastCheckupDate;

    /**
     * 体检报告（JSON对象）
     */
    private String checkupReport;

    /**
     * 健康目标
     */
    private String healthGoals;

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

