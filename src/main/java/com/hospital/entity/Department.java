package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 科室实体类（映射到中医分类表）
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Data
@TableName("tcm_category")
public class Department implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 分类名称（中医分类）
     */
    @TableField("category_name")
    private String categoryName;

    /**
     * 分类描述
     */
    @TableField("category_desc")
    private String categoryDesc;

    /**
     * 图标
     */
    @TableField("icon")
    private String icon;

    /**
     * 科室名称（兼容字段，映射到 category_name）
     */
    @TableField(exist = false)
    private String deptName;

    /**
     * 科室描述（兼容字段，映射到 category_desc）
     */
    @TableField(exist = false)
    private String deptDesc;

    /**
     * 负责人姓名
     */
    @TableField("dept_head")
    private String deptHead;

    /**
     * 联系电话
     */
    @TableField("contact_phone")
    private String contactPhone;

    /**
     * 科室位置
     */
    @TableField("location")
    private String location;

    /**
     * 科室分类ID（兼容字段，不存在于数据库）
     */
    @TableField(exist = false)
    private Integer categoryId;

    /**
     * 分类描述（兼容字段，映射到 category_desc）
     */
    @TableField(exist = false)
    private String categoryDescription;

    /**
     * 状态（0-禁用 1-启用）
     */
    @TableField("status")
    private Integer status;

    /**
     * 排序
     */
    @TableField("sort_order")
    private Integer sortOrder;

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
}

