package com.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.entity.UserNotification;

import java.util.List;

/**
 * 用户通知服务接口。
 */
public interface NotificationService {

    /**
     * 创建体质测试提醒通知。
     * @param notification 通知对象
     * @return void
     * @throws Exception 如果创建体质测试提醒通知失败
     */
    void createConstitutionReminder(UserNotification notification);

    /**
     * 创建并发送通知（保存到数据库并通过WebSocket推送）。
     * @param userId 接收通知的用户ID
     * @param title 通知标题
     * @param content 通知内容
     * @param type 通知类型
     */
    void createAndSendNotification(Long userId, String title, String content, String type);

    /**
     * 查询用户通知（支持分页）。
     */
    IPage<UserNotification> queryNotifications(Long userId, Page<UserNotification> page, Integer readStatus, String type);

    /**
     * 获取用户未读通知列表（默认最新若干条）。
     */
    List<UserNotification> listUnreadNotifications(Long userId, int limit);

    /**
     * 将通知标记为已读。
     */
    void markAsRead(Long userId, Long notificationId);

    /**
     * 批量将通知标记为已读。
     */
    void markAllAsRead(Long userId, List<Long> notificationIds);
}

