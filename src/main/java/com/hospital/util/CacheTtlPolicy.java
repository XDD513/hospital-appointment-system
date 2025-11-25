package com.hospital.util;

import com.hospital.common.constant.CacheConstants;

import java.util.concurrent.TimeUnit;

/**
 * 缓存TTL策略
 * 统一管理缓存过期时间，避免硬编码
 *
 * @author Hospital Team
 * @since 2025-11-25
 */
public enum CacheTtlPolicy {

    // ==================== 永久缓存 ====================
    /** 永久缓存（-1表示永不过期） */
    PERMANENT(-1, TimeUnit.SECONDS),

    // ==================== 短期缓存（秒级） ====================
    /** 会话消息列表缓存（1分钟） */
    CONVERSATION_MESSAGE_LIST(CacheConstants.CACHE_CONVERSATION_MESSAGE_LIST_TTL_SECONDS, TimeUnit.SECONDS),

    /** 会话列表缓存（2分钟） */
    CONVERSATION_LIST(CacheConstants.CACHE_CONVERSATION_LIST_TTL_SECONDS, TimeUnit.SECONDS),

    /** 会话总数缓存（2分钟） */
    CONVERSATION_COUNT(CacheConstants.CACHE_CONVERSATION_COUNT_TTL_SECONDS, TimeUnit.SECONDS),

    /** 会话信息缓存（5分钟） */
    CONVERSATION_DETAIL(CacheConstants.CACHE_CONVERSATION_TTL_SECONDS, TimeUnit.SECONDS),

    /** 操作日志热门分页（5分钟） */
    OPERATION_LOG_HOT(CacheConstants.OPLOG_TTL_SECONDS, TimeUnit.SECONDS),

    /** 医生评价热门分页（5分钟） */
    DOCTOR_REVIEWS_HOT(CacheConstants.DOCTOR_REVIEWS_HOT_TTL_SECONDS, TimeUnit.SECONDS),

    /** 管理员评价列表（5分钟） */
    ADMIN_REVIEWS(CacheConstants.ADMIN_REVIEWS_TTL_SECONDS, TimeUnit.SECONDS),

    /** 管理员排班列表（5分钟） */
    ADMIN_SCHEDULE_LIST(CacheConstants.ADMIN_SCHEDULE_LIST_TTL_SECONDS, TimeUnit.SECONDS),

    // ==================== 中期缓存（分钟级） ====================
    /** 医生信息缓存（30分钟） */
    DOCTOR_DETAIL(CacheConstants.CACHE_DOCTOR_TTL_SECONDS, TimeUnit.SECONDS),

    /** OSS签名URL缓存（55分钟） */
    OSS_SIGNED_URL(CacheConstants.CACHE_OSS_SIGNED_URL_TTL_SECONDS, TimeUnit.SECONDS),

    /** 评价详情（30分钟） */
    REVIEW_DETAIL(CacheConstants.REVIEW_DETAIL_TTL_SECONDS, TimeUnit.SECONDS),

    /** AI推荐结果缓存（30分钟） */
    AI_RECOMMENDATION(CacheConstants.AI_RECOMMENDATION_TTL_SECONDS, TimeUnit.SECONDS),

    // ==================== 长期缓存（小时级） ====================
    /** 体质类型缓存（24小时） */
    CONSTITUTION_TYPE(CacheConstants.CONSTITUTION_CACHE_TTL_HOURS, TimeUnit.HOURS);

    private final long duration;
    private final TimeUnit unit;

    CacheTtlPolicy(long duration, TimeUnit unit) {
        this.duration = duration;
        this.unit = unit;
    }

    /**
     * 获取过期时间（秒）
     *
     * @return 过期时间（秒），-1表示永不过期
     */
    public long getSeconds() {
        if (duration == -1) {
            return -1;
        }
        return unit.toSeconds(duration);
    }

    /**
     * 获取过期时间（毫秒）
     *
     * @return 过期时间（毫秒），-1表示永不过期
     */
    public long getMillis() {
        if (duration == -1) {
            return -1;
        }
        return unit.toMillis(duration);
    }

    /**
     * 获取时间单位
     *
     * @return 时间单位
     */
    public TimeUnit getUnit() {
        return unit;
    }

    /**
     * 获取持续时间
     *
     * @return 持续时间
     */
    public long getDuration() {
        return duration;
    }

    /**
     * 判断是否为永久缓存
     *
     * @return true-永久缓存，false-有过期时间
     */
    public boolean isPermanent() {
        return duration == -1;
    }
}

