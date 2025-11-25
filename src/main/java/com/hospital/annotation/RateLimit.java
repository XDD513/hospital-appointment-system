package com.hospital.annotation;

import java.lang.annotation.*;

/**
 * 接口限流注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 时间窗口内允许的最大请求次数
     */
    int limit() default 60;

    /**
     * 时间窗口（秒）
     */
    long windowSeconds() default 60;

    /**
     * 自定义限流键前缀（可选）
     */
    String key() default "";

    /**
     * 是否按IP区分
     */
    boolean perIp() default true;

    /**
     * 是否按用户ID区分
     */
    boolean perUser() default false;
}


