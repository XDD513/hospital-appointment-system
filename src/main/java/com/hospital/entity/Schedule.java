package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 排班实体类
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Data
@TableName("schedule")
public class Schedule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 排班ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 医生ID
     */
    @TableField("doctor_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long doctorId;

    /**
     * 医生姓名（关联查询字段，不存储在数据库中）
     */
    @TableField(exist = false)
    private String doctorName;

    /**
     * 分类名称（关联查询字段，不存储在数据库中）
     */
    @TableField(exist = false)
    private String categoryName;

    /**
     * 科室名称（兼容字段，关联查询字段，不存储在数据库中）
     */
    @TableField(exist = false)
    private String deptName;

    /**
     * 排班日期
     */
    @TableField("schedule_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    /**
     * 时段（MORNING-上午 AFTERNOON-下午 EVENING-晚间）
     */
    @TableField("time_slot")
    private String timeSlot;

    /**
     * 开始时间
     */
    @TableField("start_time")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    /**
     * 总号源数
     */
    @TableField("total_quota")
    private Integer totalQuota;

    /**
     * 已预约数
     */
    @TableField("booked_quota")
    private Integer bookedQuota;

    /**
     * 剩余号源数
     */
    @TableField("remaining_quota")
    private Integer remainingQuota;

    /**
     * 状态（AVAILABLE-可预约 FULL-已满 CLOSED-停诊）
     */
    private String status;

    /**
     * 备注
     */
    private String note;

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
}

