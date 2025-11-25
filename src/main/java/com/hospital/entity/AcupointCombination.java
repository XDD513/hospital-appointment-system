package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 穴位组合方案实体类
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Data
@TableName("acupoint_combination")
public class AcupointCombination implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 组合方案名称
     */
    private String combinationName;

    /**
     * 适用体质（逗号分隔）
     */
    private String constitutionType;

    /**
     * 适用症状
     */
    private String symptom;

    /**
     * 穴位ID列表（逗号分隔）
     */
    private String acupointIds;

    /**
     * 方案描述
     */
    private String description;

    /**
     * 按摩顺序（JSON数组）
     */
    private String massageSequence;

    /**
     * 总时长（分钟）
     */
    @TableField("duration")
    private Integer totalDuration;

    /**
     * 建议频率
     */
    private String frequency;

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

