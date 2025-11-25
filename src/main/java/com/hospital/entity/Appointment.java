package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约挂号实体类
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Data
@TableName("appointment")
public class Appointment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 预约ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 患者ID（用户ID）
     */
    @TableField("user_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long patientId;

    /**
     * 医生ID
     */
    @TableField("doctor_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long doctorId;

    /**
     * 分类ID（中医分类）
     */
    @TableField("category_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long categoryId;

    /**
     * 科室ID（兼容字段，不存在于数据库）
     */
    @TableField(exist = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long deptId;

    /**
     * 排班ID
     */
    @TableField("schedule_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long scheduleId;

    /**
     * 预约日期
     */
    @TableField("appointment_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;

    /**
     * 时段
     */
    @TableField("time_slot")
    private String timeSlot;

    /**
     * 排队号
     */
    @TableField("queue_number")
    private Integer queueNumber;

    /**
     * 患者姓名（关联查询字段，不存储在数据库中）
     */
    @TableField(exist = false)
    private String patientName;

    /**
     * 患者手机（关联查询字段，不存储在数据库中）
     */
    @TableField(exist = false)
    private String patientPhone;

    /**
     * 患者身份证（关联查询字段，不存储在数据库中）
     */
    @TableField(exist = false)
    private String patientIdCard;

    /**
     * 症状描述（关联查询字段，不存储在数据库中）
     */
    @TableField(exist = false)
    private String symptomDesc;

    /**
     * 状态（PENDING-待确认 CONFIRMED-已确认 IN_PROGRESS-就诊中 COMPLETED-已完成 CANCELLED-已取消 NO_SHOW-爽约）
     */
    private String status;

    /**
     * 取消原因
     */
    @TableField("cancel_reason")
    private String cancelReason;

    /**
     * 创建时间
     */
    @TableField("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField("version")
    private Integer version;

    // 以下字段用于关联查询，不存在于数据库表中
    
    /**
     * 医生姓名（关联查询）
     */
    @TableField(exist = false)
    private String doctorName;
    
    /**
     * 医生职称（关联查询）
     */
    @TableField(exist = false)
    private String doctorTitle;
    
    /**
     * 分类名称（关联查询）
     */
    @TableField(exist = false)
    private String categoryName;

    /**
     * 科室名称（兼容字段，关联查询）
     */
    @TableField(exist = false)
    private String deptName;
    
    /**
     * 挂号费（关联查询）
     */
    @TableField(exist = false)
    private BigDecimal consultationFee;
}

