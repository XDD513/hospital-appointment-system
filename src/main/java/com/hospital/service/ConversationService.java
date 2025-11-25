package com.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hospital.dto.request.ConversationCreateRequest;
import com.hospital.dto.request.SendMessageRequest;
import com.hospital.entity.Conversation;
import com.hospital.entity.ConversationMessage;

import java.util.Map;

/**
 * 医患对话服务
 */
public interface ConversationService extends IService<Conversation> {

    /**
     * 分页查询会话
     */
    IPage<Conversation> listConversations(Map<String, Object> params);

    /**
     * 创建会话
     * @param request 创建会话请求
     * @param currentUserId 当前登录用户ID（用于管理员创建会话时自动填充）
     * @param currentRoleType 当前登录用户角色类型（1=患者，2=医生，3=管理员）
     * @return 创建的会话对象
     */
    Conversation createConversation(ConversationCreateRequest request, Long currentUserId, Integer currentRoleType);

    /**
     * 分页查询消息
     */
    IPage<ConversationMessage> listMessages(Long conversationId, Map<String, Object> params);

    /**
     * 追加消息
     */
    ConversationMessage appendMessage(Long conversationId, SendMessageRequest request);

    /**
     * 清空会话消息
     */
    boolean clearMessages(Long conversationId);

    /**
     * 删除会话（单方面删除，已废弃，请使用实现类中的 deleteConversationByRole 方法）
     */
    @Deprecated
    boolean deleteConversation(Long conversationId);

    /**
     * 批量删除会话（根据角色删除所有会话）
     */
    boolean deleteAllConversations(Long participantId, String role);

    /**
     * 获取最近三次发送消息的用户信息（用于聊天窗口头像显示）
     */
    java.util.List<java.util.Map<String, Object>> getRecentSenders(Long conversationId);

    /**
     * 获取或创建与管理员对话的会话（多对一：多个发送者对一个管理员）
     * @param senderId 发送者用户ID（可以是患者、医生等任何角色）
     * @return 会话对象
     */
    Conversation getOrCreateAdminConversation(Long senderId);

    /**
     * 将会话标记为已读
     *
     * @param conversationId 会话ID
     * @param userId         当前阅读者的用户ID（优先使用）
     * @param role           当前阅读者的角色（PATIENT/DOCTOR/ADMIN），用于兼容旧数据
     * @return 是否更新成功
     */
    boolean markConversationAsRead(Long conversationId, Long userId, String role);

    /**
     * 批量标记所有会话为已读
     *
     * @param userId 当前阅读者的用户ID（优先使用）
     * @param role   当前阅读者的角色（PATIENT/DOCTOR/ADMIN），用于兼容旧数据
     * @return 标记成功的会话数量
     */
    int markAllConversationsAsRead(Long userId, String role);
}


