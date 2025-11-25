package com.hospital.dto.request;

import lombok.Data;
import java.util.Map;

/**
 * 系统设置请求DTO
 */
@Data
public class SystemSettingsRequest {
    
    /**
     * 设置类型（basic, notification, security, email）
     */
    private String type;
    
    /**
     * 设置数据
     */
    private Map<String, Object> data;
}

