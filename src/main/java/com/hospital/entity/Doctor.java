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
 * 中医师实体类
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Data
@TableName("tcm_doctor")
public class Doctor implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 中医师ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 用户ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;

    /**
     * 中医分类ID
     */
    @TableField("category_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long categoryId;

    /**
     * 中医分类名称（关联查询字段，不存储在数据库中）
     */
    @TableField(exist = false)
    private String categoryName;

    /**
     * 科室ID（兼容旧字段，映射到category_id）
     */
    @TableField(exist = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long deptId;

    /**
     * 科室名称（兼容旧字段，映射到categoryName）
     */
    @TableField(exist = false)
    private String deptName;

    /**
     * 中医师姓名
     */
    @TableField("doctor_name")
    private String doctorName;

    /**
     * 手机号（关联查询字段，从user表获取）
     */
    @TableField(exist = false)
    private String phone;

    /**
     * 头像（关联查询字段，从user表获取）
     */
    @TableField(exist = false)
    private String avatar;

    /**
     * 性别（0-未知 1-男 2-女）（关联查询字段，从user表获取）
     */
    @TableField(exist = false)
    private Integer gender;

    /**
     * 出生日期（关联查询字段，从user表获取）
     */
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    /**
     * 职称
     */
    private String title;

    /**
     * 专长
     */
    private String specialty;

    /**
     * 个人简介
     */
    private String introduction;

    /**
     * 挂号费
     */
    @TableField("consultation_fee")
    private BigDecimal consultationFee;

    /**
     * 从业年限
     */
    @TableField("years_of_experience")
    private Integer yearsOfExperience;

    /**
     * 学历
     */
    private String education;

    /**
     * 资质证书（JSON格式）
     */
    private String certificates;

    /**
     * 擅长体质（逗号分隔）
     */
    @TableField("good_at_constitution")
    private String goodAtConstitution;

    /**
     * 评分（0-5.00）
     */
    private BigDecimal rating;

    /**
     * 接诊总数
     */
    @TableField("consultation_count")
    private Integer consultationCount;

    /**
     * 状态（0-禁用 1-启用）
     */
    private Integer status;

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

