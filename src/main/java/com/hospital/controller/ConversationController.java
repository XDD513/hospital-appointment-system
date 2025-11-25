package com.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.common.result.Result;
import com.hospital.dto.request.ConversationCreateRequest;
import com.hospital.dto.request.SendMessageRequest;
import com.hospital.entity.Conversation;
import com.hospital.entity.ConversationMessage;
import com.hospital.service.ConversationService;
import com.hospital.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 医患对话控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 分页查询会话
     */
    @GetMapping
    public Result<IPage<Conversation>> listConversations(@RequestParam Map<String, Object> params) {
        IPage<Conversation> conversations = conversationService.listConversations(params);
        return Result.success(conversations);
    }

    /**
     * 创建会话
     */
    @PostMapping
    public Result<Conversation> createConversation(@Validated @RequestBody ConversationCreateRequest request,
                                                     HttpServletRequest httpRequest) {
        try {
            // 获取当前登录用户ID和角色类型
            Long currentUserId = jwtUtil.getUserIdFromRequest(httpRequest);
            Integer currentRoleType = jwtUtil.getRoleTypeFromRequest(httpRequest);
            
            Conversation conversation = conversationService.createConversation(request, currentUserId, currentRoleType);
            return Result.success(conversation);
        } catch (IllegalArgumentException ex) {
            log.warn("创建会话失败: {}", ex.getMessage());
            return Result.error(ex.getMessage());
        }
    }

    /**
     * 查询会话消息
     */
    @GetMapping("/{conversationId}/messages")
    public Result<IPage<ConversationMessage>> listMessages(@PathVariable Long conversationId,
                                                           @RequestParam Map<String, Object> params) {
        params.putIfAbsent("conversationId", conversationId);
        IPage<ConversationMessage> messages = conversationService.listMessages(conversationId, params);
        return Result.success(messages);
    }

    /**
     * 发送消息
     */
    @PostMapping("/{conversationId}/messages")
    public Result<ConversationMessage> sendMessage(@PathVariable Long conversationId,
                                                   @Validated @RequestBody SendMessageRequest request) {
        try {
            ConversationMessage message = conversationService.appendMessage(conversationId, request);
            return Result.success(message);
        } catch (IllegalArgumentException ex) {
            log.warn("发送消息失败: {}", ex.getMessage());
            return Result.error(ex.getMessage());
        }
    }

    /**
     * 清空会话消息
     */
    @DeleteMapping("/{conversationId}/messages")
    public Result<Boolean> clearMessages(@PathVariable Long conversationId) {
        boolean cleared = conversationService.clearMessages(conversationId);
        return cleared ? Result.success(true) : Result.error("清空会话失败");
    }

    /**
     * 删除会话（单方面删除，根据角色标记删除）
     */
    @DeleteMapping("/{conversationId}")
    public Result<Boolean> deleteConversation(@PathVariable Long conversationId, @RequestParam String role) {
        try {
            boolean deleted = ((com.hospital.service.impl.ConversationServiceImpl) conversationService)
                    .deleteConversationByRole(conversationId, role);
            return deleted ? Result.success(true) : Result.error("删除会话失败");
        } catch (IllegalArgumentException ex) {
            log.warn("删除会话失败: {}", ex.getMessage());
            return Result.error(ex.getMessage());
        }
    }

    /**
     * 批量删除会话（根据角色删除所有会话）
     */
    @DeleteMapping
    public Result<Boolean> deleteAllConversations(@RequestParam Long participantId, @RequestParam String role) {
        try {
            boolean deleted = conversationService.deleteAllConversations(participantId, role);
            return deleted ? Result.success(true) : Result.error("清空历史对话失败");
        } catch (IllegalArgumentException ex) {
            log.warn("清空历史对话失败: {}", ex.getMessage());
            return Result.error(ex.getMessage());
        }
    }

    /**
     * 获取最近三次发送消息的用户信息（用于聊天窗口头像显示）
     */
    @GetMapping("/{conversationId}/recent-senders")
    public Result<java.util.List<java.util.Map<String, Object>>> getRecentSenders(@PathVariable Long conversationId) {
        try {
            java.util.List<java.util.Map<String, Object>> senders = conversationService.getRecentSenders(conversationId);
            return Result.success(senders);
        } catch (IllegalArgumentException ex) {
            log.warn("获取最近发送者失败: {}", ex.getMessage());
            return Result.error(ex.getMessage());
        }
    }

    /**
     * 获取或创建与管理员对话的会话（多对一：多个发送者对一个管理员）
     */
    @GetMapping("/admin-conversation")
    public Result<Conversation> getOrCreateAdminConversation(@RequestParam Long senderId) {
        try {
            Conversation conversation = conversationService.getOrCreateAdminConversation(senderId);
            return Result.success(conversation);
        } catch (IllegalArgumentException ex) {
            log.warn("获取或创建管理员对话失败: {}", ex.getMessage());
            return Result.error(ex.getMessage());
        }
    }

    /**
     * 将会话标记为已读
     */
    @PutMapping("/{conversationId}/read")
    public Result<Boolean> markConversationAsRead(@PathVariable Long conversationId,
                                                  @RequestParam(required = false) Long userId,
                                                  @RequestParam(required = false) String role) {
        try {
            boolean success = conversationService.markConversationAsRead(conversationId, userId, role);
            return success ? Result.success(true) : Result.error("更新未读状态失败");
        } catch (IllegalArgumentException ex) {
            log.warn("标记会话已读失败: {}", ex.getMessage());
            return Result.error(ex.getMessage());
        }
    }

    /**
     * 批量标记所有会话为已读
     */
    @PutMapping("/read-all")
    public Result<Integer> markAllConversationsAsRead(@RequestParam(required = false) Long userId,
                                                     @RequestParam(required = false) String role) {
        try {
            int count = conversationService.markAllConversationsAsRead(userId, role);
            return Result.success(count);
        } catch (IllegalArgumentException ex) {
            log.warn("批量标记会话已读失败: {}", ex.getMessage());
            return Result.error(ex.getMessage());
        }
    }
}


