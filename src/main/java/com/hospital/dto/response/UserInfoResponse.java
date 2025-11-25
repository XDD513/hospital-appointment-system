package com.hospital.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户信息响应DTO
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Data
public class UserInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 身份证号（脱敏）
     */
    private String idCard;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 性别（0-未知 1-男 2-女）
     */
    private Integer gender;

    /**
     * 出生日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 角色类型
     */
    private Integer roleType;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /**
     * 医生ID（仅医生角色有值）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long doctorId;
}


