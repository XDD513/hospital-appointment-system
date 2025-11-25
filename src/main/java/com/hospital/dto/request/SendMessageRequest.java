package com.hospital.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 发送医患对话消息请求
 */
@Data
public class SendMessageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "发送者角色不能为空")
    private String senderRole;

    private Long senderId;

    @Size(max = 80, message = "发送者昵称长度不能超过80个字符")
    private String senderName;

    @Size(max = 255, message = "头像地址过长")
    private String senderAvatar;

    @NotBlank(message = "消息内容不能为空")
    private String content;

    @Size(max = 16, message = "消息类型过长")
    private String contentType;

    @Size(max = 255, message = "附件地址过长")
    private String attachmentUrl;

    private String metadata;
}


