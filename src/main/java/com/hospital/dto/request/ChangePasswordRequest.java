package com.hospital.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 修改密码请求DTO
 *
 * @author Hospital Team
 */
@Data
public class ChangePasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度必须在6-20位之间")
    private String newPassword;
}

