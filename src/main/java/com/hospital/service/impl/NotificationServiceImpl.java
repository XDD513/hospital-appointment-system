package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.common.constant.SystemSettingKeys;
import com.hospital.config.SystemSettingManager;
import com.hospital.entity.UserNotification;
import com.hospital.mapper.UserNotificationMapper;
import com.hospital.dto.NotificationMessageDTO;
import com.hospital.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户通知服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private final UserNotificationMapper userNotificationMapper;

    @Autowired
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    private final SystemSettingManager systemSettingManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createConstitutionReminder(UserNotification notification) {
        if (!Boolean.TRUE.equals(systemSettingManager.getBoolean(SystemSettingKeys.NOTIFICATION_APPOINTMENT_REMINDER, Boolean.TRUE))) {
            log.info("体质测试提醒功能已关闭，本次提醒未创建");
            return;
        }
        if (notification.getReadStatus() == null) {
            notification.setReadStatus(0);
        }
        userNotificationMapper.insert(notification);
        log.info("已为用户创建体质测试提醒：userId={}, notificationId={}, title={}",
                notification.getUserId(), notification.getId(), notification.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAndSendNotification(Long userId, String title, String content, String type) {
        if (userId == null) {
            log.warn("用户ID为空，无法发送通知");
            return;
        }
        if (!Boolean.TRUE.equals(systemSettingManager.getBoolean(SystemSettingKeys.NOTIFICATION_SYSTEM_ENABLED, Boolean.TRUE))) {
            log.info("系统内通知功能已关闭，跳过发送：userId={}, title={}", userId, title);
            return;
        }

        try {
            // 创建通知记录
            UserNotification notification = new UserNotification();
            notification.setUserId(userId);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setType(type);
            notification.setReadStatus(0);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setUpdatedAt(LocalDateTime.now());

            userNotificationMapper.insert(notification);
            log.info("已创建通知：userId={}, notificationId={}, title={}", userId, notification.getId(), title);

            // 通过WebSocket推送通知
            NotificationMessageDTO payload = NotificationMessageDTO.builder()
                    .id(notification.getId())
                    .title(notification.getTitle())
                    .content(notification.getContent())
                    .type(notification.getType())
                    .readStatus(notification.getReadStatus())
                    .createdAt(notification.getCreatedAt())
                    .updatedAt(notification.getUpdatedAt())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/notifications",
                    payload
            );
            log.info("已通过WebSocket推送通知：userId={}, notificationId={}", userId, notification.getId());
        } catch (Exception e) {
            log.error("创建并发送通知失败：userId={}, title={}, error={}", userId, title, e.getMessage(), e);
        }
    }

    @Override
    public IPage<UserNotification> queryNotifications(Long userId, Page<UserNotification> page, Integer readStatus, String type) {
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getUserId, userId)
                .orderByDesc(UserNotification::getCreatedAt);

        if (readStatus != null) {
            wrapper.eq(UserNotification::getReadStatus, readStatus);
        }

        if (type != null && !type.trim().isEmpty()) {
            wrapper.eq(UserNotification::getType, type);
        }

        return userNotificationMapper.selectPage(page, wrapper);
    }

    @Override
    public List<UserNotification> listUnreadNotifications(Long userId, int limit) {
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getReadStatus, 0)
                .orderByDesc(UserNotification::getCreatedAt)
                .last("LIMIT " + limit);
        return userNotificationMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long userId, Long notificationId) {
        UserNotification notification = userNotificationMapper.selectById(notificationId);
        if (notification == null || !notification.getUserId().equals(userId)) {
            return;
        }

        if (notification.getReadStatus() != null && notification.getReadStatus() == 1) {
            return;
        }

        notification.setReadStatus(1);
        notification.setReadAt(LocalDateTime.now());
        userNotificationMapper.updateById(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId, List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (Long notificationId : notificationIds) {
            UserNotification notification = userNotificationMapper.selectById(notificationId);
            if (notification != null && notification.getUserId().equals(userId)) {
                if (notification.getReadStatus() == null || notification.getReadStatus() == 0) {
                    notification.setReadStatus(1);
                    notification.setReadAt(now);
                    userNotificationMapper.updateById(notification);
                }
            }
        }
        log.info("批量标记通知为已读：userId={}, count={}", userId, notificationIds.size());
    }
}

