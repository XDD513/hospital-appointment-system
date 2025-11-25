package com.hospital.dto.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 区域信息DTO（用于前端展示）
 *
 * @author Hospital Team
 * @since 2025-12-19
 */
@Data
public class AreaInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 区域ID
     */
    private String id;

    /**
     * 区域名称
     */
    private String name;

    /**
     * 区域级别（1-省，2-市，3-区县，4-乡镇，5-村社区）
     */
    private Integer level;

    /**
     * 父级区域ID
     */
    private String parentId;

    /**
     * 是否为叶子节点（用于级联选择器）
     */
    private Boolean isLeaf;

    public AreaInfoDTO() {
    }

    public AreaInfoDTO(String id, String name, Integer level, String parentId) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.parentId = parentId;
        // 默认村社区级别为叶子节点
        this.isLeaf = level != null && level >= 5;
    }
}

