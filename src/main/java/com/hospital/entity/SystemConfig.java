package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统配置实体类
 */
@Data
@TableName("system_config")
public class SystemConfig {
    
    /**
     * 配置ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 配置键
     */
    @TableField("config_key")
    private String configKey;
    
    /**
     * 配置值
     */
    @TableField("config_value")
    private String configValue;
    
    /**
     * 配置描述
     */
    @TableField("config_desc")
    private String configDesc;
    
    /**
     * 配置类型
     */
    @TableField("config_type")
    private String configType;

    /**
     * 配置分组（兼容字段，不存在于数据库）
     */
    @TableField(exist = false)
    private String configGroup;

    /**
     * 是否系统配置 0-否 1-是（兼容字段，不存在于数据库）
     */
    @TableField(exist = false)
    private Integer isSystem;

    /**
     * 排序（兼容字段，不存在于数据库）
     */
    @TableField(exist = false)
    private Integer sortOrder;

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
}
