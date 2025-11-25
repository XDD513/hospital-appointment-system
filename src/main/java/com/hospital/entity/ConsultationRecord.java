package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * 接诊记录实体类
 */
@Data
@TableName("consultation_record")
public class ConsultationRecord {
    
    /**
     * 接诊记录ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 预约ID
     */
    @TableField("appointment_id")
    private Long appointmentId;
    
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
     * 接诊日期
     */
    @TableField("consultation_date")
    private LocalDate consultationDate;
    
    /**
     * 接诊时间
     */
    @TableField("consultation_time")
    private LocalTime consultationTime;
    
    /**
     * 主诉
     */
    @TableField("chief_complaint")
    private String chiefComplaint;
    
    /**
     * 现病史
     */
    @TableField("present_illness")
    private String presentIllness;
    
    /**
     * 既往史
     */
    @TableField("past_history")
    private String pastHistory;
    
    /**
     * 体格检查
     */
    @TableField("physical_examination")
    private String physicalExamination;
    
    /**
     * 辅助检查
     */
    @TableField("auxiliary_examination")
    private String auxiliaryExamination;
    
    /**
     * 诊断
     */
    @TableField("diagnosis")
    private String diagnosis;
    
    /**
     * 治疗方案
     */
    @TableField("treatment_plan")
    private String treatmentPlan;
    
    /**
     * 处方（JSON格式）
     */
    @TableField("prescription")
    private String prescription;
    
    /**
     * 随访建议
     */
    @TableField("follow_up_advice")
    private String followUpAdvice;
    
    /**
     * 诊疗费
     */
    @TableField("consultation_fee")
    private BigDecimal consultationFee;
    
    /**
     * 接诊时长（分钟）
     */
    @TableField("duration_minutes")
    private Integer durationMinutes;
    
    /**
     * 状态 IN_PROGRESS-进行中 COMPLETED-已完成
     */
    @TableField("status")
    private String status;
    
    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 乐观锁版本号
     */
    @Version
    @TableField("version")
    private Integer version;
    
    // 关联查询字段（不映射到数据库）
    @TableField(exist = false)
    private String patientName;

    @TableField(exist = false)
    private String doctorName;

    @TableField(exist = false)
    private String categoryName;

    /**
     * 预约日期（关联查询字段，来自appointment表）
     */
    @TableField(exist = false)
    private LocalDate appointmentDate;

    /**
     * 时段（关联查询字段，来自appointment表）
     */
    @TableField(exist = false)
    private String timeSlot;

    /**
     * 排队号（关联查询字段，来自appointment表）
     */
    @TableField(exist = false)
    private Integer queueNumber;

    /**
     * 患者性别（关联查询字段，来自user表）
     */
    @TableField(exist = false)
    private String gender;

    /**
     * 患者年龄（关联查询字段，来自user表）
     */
    @TableField(exist = false)
    private Integer age;

    /**
     * 完整接诊时间（关联查询字段，组合consultationDate和consultationTime的字符串）
     */
    @TableField(exist = false)
    private String consultationDateTime;

    @TableField(exist = false)
    private String deptName;
}
