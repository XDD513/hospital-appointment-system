package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评价实体类
 */
@Data
@TableName("review")
public class Review {

    /**
     * 评价ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 预约ID
     */
    @TableField("appointment_id")
    private Long appointmentId;

    /**
     * 接诊记录ID
     */
    @TableField("consultation_record_id")
    private Long consultationRecordId;

    /**
     * 患者ID
     */
    @TableField("patient_id")
    private Long patientId;

    /**
     * 医生ID
     */
    @TableField("doctor_id")
    private Long doctorId;

    /**
     * 分类ID（中医分类）
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 科室ID（兼容字段，不存在于数据库）
     */
    @TableField(exist = false)
    private Long deptId;

    /**
     * 评分（1-5分）
     */
    @TableField("rating")
    private Integer rating;

    /**
     * 服务态度评分（1-5分）
     */
    @TableField("service_rating")
    private Integer serviceRating;

    /**
     * 专业水平评分（1-5分）
     */
    @TableField("professional_rating")
    private Integer professionalRating;

    /**
     * 环境评分（1-5分）
     */
    @TableField("environment_rating")
    private Integer environmentRating;

    /**
     * 评价内容
     */
    @TableField("content")
    private String content;

    /**
     * 是否匿名 0-否 1-是
     */
    @TableField("is_anonymous")
    private Integer isAnonymous;

    /**
     * 标签（JSON格式）
     */
    @TableField("tags")
    private String tags;

    /**
     * 图片（JSON格式）
     */
    @TableField("images")
    private String images;

    /**
     * 医生回复
     */
    @TableField("doctor_reply")
    private String doctorReply;

    /**
     * 医生回复时间
     */
    @TableField("doctor_reply_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime doctorReplyTime;

    /**
     * 状态 PENDING-待发布 PUBLISHED-已发布 HIDDEN-已隐藏
     */
    @TableField("status")
    private String status;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // 关联查询字段（不映射到数据库）
    @TableField(exist = false)
    private String patientName;

    @TableField(exist = false)
    private String patientAvatar;

    @TableField(exist = false)
    private String doctorName;

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private String deptName;
}
