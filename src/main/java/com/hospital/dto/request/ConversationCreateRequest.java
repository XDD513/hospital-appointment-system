package com.hospital.dto.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 创建医患对话会话请求
 */
@Data
public class ConversationCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "患者ID不能为空")
    private Long patientId;

    /**
     * 医生ID（在管理员创建会话时可以为空，系统会自动使用管理员ID）
     */
    private Long doctorId;

    /**
     * 会话类型：PATIENT_DOCTOR（医患对话）或 ADMIN_USER（管理员与用户对话）
     */
    private String conversationType;

    @Size(max = 120, message = "标题长度不能超过120个字符")
    private String title;

    @Size(max = 255, message = "摘要长度不能超过255个字符")
    private String summary;
}


