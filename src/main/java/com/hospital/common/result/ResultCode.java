package com.hospital.common.result;

import lombok.Getter;

/**
 * 响应状态码枚举
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Getter
public enum ResultCode {

    // 通用状态码 (200-299)
    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    SYSTEM_ERROR(500, "系统错误"),
    OPERATION_FAILED(500, "操作失败"),

    // 客户端错误 (400-499)
    BAD_REQUEST(400, "请求参数错误"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "没有权限访问"),
    NOT_FOUND(404, "请求的资源不存在"),
    DATA_NOT_FOUND(404, "数据不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "数据冲突"),
    
    // 服务器错误 (500-599)
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),
    
    // 业务错误码 (1000+)
    // 用户相关 (1000-1099)
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    USERNAME_OR_PASSWORD_ERROR(1003, "用户名或密码错误"),
    USER_ACCOUNT_DISABLED(1004, "用户账号已被禁用"),
    USER_PHONE_EXISTS(1005, "手机号已被注册"),
    PASSWORD_ERROR(1007, "密码错误"),
    
    // Token相关 (1100-1199)
    TOKEN_INVALID(1101, "Token无效"),
    TOKEN_EXPIRED(1102, "Token已过期"),
    TOKEN_MISSING(1103, "Token缺失"),
    
    // 医生相关 (1200-1299)
    DOCTOR_NOT_FOUND(1201, "医生不存在"),
    DOCTOR_NOT_AVAILABLE(1202, "医生暂不接诊"),
    DOCTOR_HAS_UNFINISHED_APPOINTMENTS(1203, "医生存在未完成的预约"),
    DOCTOR_HAS_RELATED_DATA(1204, "医生存在关联数据"),
    
    // 科室相关 (1300-1399)
    DEPARTMENT_NOT_FOUND(1301, "科室不存在"),
    DEPARTMENT_CODE_EXISTS(1302, "科室编码已存在"),
    DEPARTMENT_HAS_DOCTORS(1303, "科室存在关联的医生"),
    
    // 患者相关 (1700-1799)
    PATIENT_HAS_UNFINISHED_APPOINTMENTS(1701, "患者存在未完成的预约"),
    PATIENT_HAS_RELATED_DATA(1702, "患者存在关联数据"),
    
    // 用户相关 (1000-1099) - 补充
    USER_HAS_RELATED_DATA(1006, "用户存在关联数据"),
    
    // 排班相关 (1400-1499)
    SCHEDULE_NOT_FOUND(1401, "排班不存在"),
    SCHEDULE_CONFLICT(1402, "排班时间冲突"),
    SCHEDULE_FULL(1403, "号源已满"),
    
    // 预约相关 (1500-1599)
    APPOINTMENT_NOT_FOUND(1501, "预约记录不存在"),
    APPOINTMENT_QUOTA_INSUFFICIENT(1502, "号源不足"),
    APPOINTMENT_TIME_INVALID(1503, "预约时间无效"),
    APPOINTMENT_ALREADY_EXISTS(1504, "已存在相同时间的预约"),
    APPOINTMENT_CANCEL_TIMEOUT(1505, "超过取消时限，无法取消"),
    APPOINTMENT_STATUS_ERROR(1506, "预约状态错误"),
    
    // 支付相关 (1600-1699)
    PAYMENT_FAILED(1601, "支付失败"),
    PAYMENT_TIMEOUT(1602, "支付超时"),
    PAYMENT_AMOUNT_ERROR(1603, "支付金额错误"),
    REFUND_FAILED(1604, "退款失败"),
    
    // 数据库相关 (2000-2099)
    DB_QUERY_ERROR(2001, "数据查询失败"),
    DB_INSERT_ERROR(2002, "数据插入失败"),
    DB_UPDATE_ERROR(2003, "数据更新失败"),
    DB_DELETE_ERROR(2004, "数据删除失败");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 状态消息
     */
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}

