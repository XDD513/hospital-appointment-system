package com.hospital.common.constant;

/**
 * 缓存常量类
 * 定义所有Redis缓存相关的键前缀和TTL时间
 *
 * @author Hospital Team
 * @since 2025-12-19
 */
public class CacheConstants {

    // ==================== 缓存键前缀 ====================

    /**
     * Redis Key前缀（已在SystemConstants中定义，此处仅为分类使用）
     */
    public static final String REDIS_KEY_PREFIX = "hospital:";

    /**
     * 会话相关缓存键前缀
     */
    public static final String CACHE_CONVERSATION_PREFIX = REDIS_KEY_PREFIX + "conversation:detail:id:";
    public static final String CACHE_DOCTOR_PREFIX = REDIS_KEY_PREFIX + "doctor:detail:id:";
    public static final String CACHE_OSS_SIGNED_URL_PREFIX = REDIS_KEY_PREFIX + "oss:signed:url:";
    public static final String CACHE_CONVERSATION_LIST_PREFIX = REDIS_KEY_PREFIX + "conversation:list:userId:";
    public static final String CACHE_CONVERSATION_COUNT_PREFIX = REDIS_KEY_PREFIX + "conversation:count:userId:";
    public static final String CACHE_CONVERSATION_MESSAGE_LIST_PREFIX = REDIS_KEY_PREFIX + "conversation:messages:conversationId:";

    /**
     * 体质相关缓存键
     */
    public static final String CONSTITUTION_TYPES_CACHE_KEY = REDIS_KEY_PREFIX + "common:constitution:type:list";
    public static final String CONSTITUTION_TYPE_CACHE_PREFIX = REDIS_KEY_PREFIX + "common:constitution:type:detail:code:";

    /**
     * 系统管理相关缓存键前缀
     */
    public static final String USER_LIST_CACHE_PREFIX = REDIS_KEY_PREFIX + "admin:user:list";
    public static final String USER_LIST_CACHE_PATTERN = USER_LIST_CACHE_PREFIX + "*";
    public static final String SYSTEM_CONFIG_CACHE_PREFIX = REDIS_KEY_PREFIX + "admin:config:key:";
    public static final String DICT_LIST_CACHE_KEY = REDIS_KEY_PREFIX + "admin:dict:list";

    /**
     * 医生相关缓存键
     */
    public static final String DOCTOR_LIST_CACHE_KEY = REDIS_KEY_PREFIX + "common:doctor:list";
    public static final String DOCTOR_LIST_ENABLED_CACHE_KEY = REDIS_KEY_PREFIX + "common:doctor:list:enabled";
    public static final String DOCTOR_LIST_BY_DEPT_CACHE_PREFIX = REDIS_KEY_PREFIX + "common:doctor:list:dept:";

    /**
     * AI推荐相关缓存键前缀
     */
    public static final String AI_RECOMMENDATION_REASON_CACHE_PREFIX = REDIS_KEY_PREFIX + "ai:recommendation:reason:";
    public static final String AI_CONVERSATION_CACHE_PREFIX = REDIS_KEY_PREFIX + "ai:conversation:";
    public static final String AI_QUESTION_CACHE_PREFIX = REDIS_KEY_PREFIX + "ai:question:";
    public static final String AI_CF_RECOMMEND_CACHE_PREFIX = REDIS_KEY_PREFIX + "ai:recommendation:cf:user:";
    public static final String AI_CONTENT_RECOMMEND_CACHE_PREFIX = REDIS_KEY_PREFIX + "ai:recommendation:content:user:";
    public static final String AI_PERSONALIZED_RECOMMEND_CACHE_PREFIX = REDIS_KEY_PREFIX + "ai:recommendation:personal:user:";

    // ==================== 缓存过期时间（秒） ====================

    /**
     * 会话相关缓存TTL
     */
    public static final long CACHE_CONVERSATION_TTL_SECONDS = 300; // 会话信息缓存5分钟（会话信息会频繁更新）
    public static final long CACHE_DOCTOR_TTL_SECONDS = 1800; // 医生信息缓存30分钟（医生信息变化不频繁）
    public static final long CACHE_OSS_SIGNED_URL_TTL_SECONDS = 3300; // OSS签名URL缓存55分钟（略小于签名URL的60分钟有效期）
    public static final long CACHE_CONVERSATION_LIST_TTL_SECONDS = 120; // 会话列表缓存2分钟（会话列表更新频繁）
    public static final long CACHE_CONVERSATION_COUNT_TTL_SECONDS = 120; // 会话总数缓存2分钟
    public static final long CACHE_CONVERSATION_MESSAGE_LIST_TTL_SECONDS = 60; // 会话消息列表缓存1分钟

    /**
     * 体质相关缓存TTL（小时）
     */
    public static final long CONSTITUTION_CACHE_TTL_HOURS = 24; // 体质类型缓存24小时

    /**
     * 系统管理相关缓存TTL
     */
    public static final long SETTINGS_TTL_SECONDS = -1; // 系统设置（永不过期）
    public static final long DICT_TTL_SECONDS = -1; // 数据字典列表（永不过期）
    public static final long OPLOG_TTL_SECONDS = 300; // 操作日志热门分页（前2页，5分钟）
    public static final long USER_LIST_TTL_SECONDS = -1; // 用户列表缓存（永不过期）

    /**
     * 评价相关缓存TTL
     */
    public static final long DOCTOR_REVIEWS_HOT_TTL_SECONDS = 300; // 医生评价热门分页（5分钟）
    public static final long REVIEW_DETAIL_TTL_SECONDS = 1800; // 评价详情（30分钟）
    public static final long ADMIN_REVIEWS_TTL_SECONDS = 300; // 管理员端列表短缓存（5分钟）

    /**
     * 排班相关缓存TTL
     */
    public static final long ADMIN_SCHEDULE_LIST_TTL_SECONDS = 300; // 管理员排班列表缓存（5分钟）

    /**
     * AI推荐缓存TTL
     */
    public static final long AI_RECOMMENDATION_TTL_SECONDS = 1800; // AI推荐结果缓存30分钟

    /**
     * 区域相关缓存键前缀
     */
    public static final String AREA_CACHE_PREFIX = REDIS_KEY_PREFIX + "area:cache:";

    // 注意：区域数据缓存TTL应从配置读取，使用 AreaConfig.getCacheTtlSeconds()
    // 不再在此处硬编码缓存TTL

    // ==================== 缓存热门页范围 ====================

    /**
     * 用户列表热门页范围
     */
    public static final int USER_LIST_HOT_PAGES = 2;

    /**
     * 管理员排班列表热门页范围
     */
    public static final int ADMIN_SCHEDULE_LIST_HOT_PAGES = 2;

    private CacheConstants() {
        // 防止实例化
    }
}

