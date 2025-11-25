package com.hospital.common.constant;

/**
 * 默认值常量类
 * 定义系统中使用的默认值常量
 *
 * @author Hospital Team
 * @since 2025-12-19
 */
public class DefaultConstants {

    /**
     * 默认头像TTL（分钟）
     */
    public static final int DEFAULT_AVATAR_TTL_MINUTES = 60;

    /**
     * 默认患者头像URL
     */
    public static final String DEFAULT_PATIENT_AVATAR = "https://api.dicebear.com/7.x/thumbs/svg?seed=patient";

    /**
     * 默认医生头像URL
     */
    public static final String DEFAULT_DOCTOR_AVATAR = "https://api.dicebear.com/7.x/thumbs/svg?seed=doctor";

    /**
     * 默认管理员头像URL
     */
    public static final String DEFAULT_ADMIN_AVATAR = "https://api.dicebear.com/7.x/thumbs/svg?seed=admin";

    private DefaultConstants() {
        // 防止实例化
    }
}

