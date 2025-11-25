package com.hospital.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于标记需要记录操作日志的方法
 * 
 * @author Hospital System
 * @since 2025-11-06
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    
    /**
     * 操作模块
     * 例如: USER, DOCTOR, APPOINTMENT, SYSTEM
     */
    String module();
    
    /**
     * 操作类型
     * 例如: INSERT, UPDATE, DELETE, SELECT
     */
    String type();
    
    /**
     * 操作描述
     * 例如: "添加用户", "更新医生信息"
     */
    String description();
}

