package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据字典实体类
 */
@Data
@TableName("dictionary")
public class Dictionary {

    /**
     * 字典ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 字典类型
     */
    @TableField("dict_type")
    private String dictType;

    /**
     * 字典标签（数据库字段）
     */
    @TableField("dict_label")
    private String dictLabel;

    /**
     * 字典编码（兼容字段，映射到 dict_label）
     */
    @TableField(exist = false)
    private String dictCode;

    /**
     * 字典名称（兼容字段，映射到 dict_label）
     */
    @TableField(exist = false)
    private String dictName;

    /**
     * 字典值
     */
    @TableField("dict_value")
    private String dictValue;

    /**
     * 父级ID（兼容字段，不存在于数据库）
     */
    @TableField(exist = false)
    private Long parentId;

    /**
     * 排序
     */
    @TableField("dict_sort")
    private Integer sortOrder;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 状态 0-禁用 1-启用
     */
    @TableField("status")
    private Integer status;

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

    // 子项列表（不映射到数据库）
    @TableField(exist = false)
    private List<Dictionary> children;
}
