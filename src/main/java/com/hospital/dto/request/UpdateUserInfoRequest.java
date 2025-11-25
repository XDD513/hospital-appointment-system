package com.hospital.dto.request;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 更新用户信息请求DTO
 *
 * @author Hospital Team
 */
@Data
public class UpdateUserInfoRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 性别（0-未知 1-男 2-女）
     */
    private Integer gender;

    /**
     * 出生日期
     */
    private LocalDate birthDate;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 地址
     */
    private String address;

    /**
     * 头像URL
     */
    private String avatar;
}

