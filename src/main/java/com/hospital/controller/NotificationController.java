package com.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.common.result.Result;
import com.hospital.entity.UserNotification;
import com.hospital.service.NotificationService;
import com.hospital.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户通知相关接口（患者端）。
 */
@Slf4j
@RestController
@RequestMapping("/api/patient/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    /**
     * 分页查询通知。
     */
    @GetMapping
    public Result<IPage<UserNotification>> pageNotifications(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer readStatus,
            @RequestParam(required = false) String type) {

        Long userId = jwtUtil.getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error(401, "未登录或登录信息已失效");
        }

        Page<UserNotification> pageParam = new Page<>(page, pageSize);
        IPage<UserNotification> notifications =
                notificationService.queryNotifications(userId, pageParam, readStatus, type);
        return Result.success(notifications);
    }

    /**
     * 查询未读通知（默认取最新5条）。
     */
    @GetMapping("/unread")
    public Result<List<UserNotification>> unreadNotifications(
            HttpServletRequest request,
            @RequestParam(defaultValue = "5") Integer limit) {

        Long userId = jwtUtil.getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error(401, "未登录或登录信息已失效");
        }

        if (limit == null || limit <= 0) {
            limit = 5;
        }

        List<UserNotification> notifications =
                notificationService.listUnreadNotifications(userId, limit);
        if (CollectionUtils.isEmpty(notifications)) {
            return Result.success(List.of());
        }
        return Result.success(notifications);
    }

    /**
     * 标记通知为已读。
     */
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(HttpServletRequest request, @PathVariable("id") Long notificationId) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error(401, "未登录或登录信息已失效");
        }

        notificationService.markAsRead(userId, notificationId);
        return Result.success();
    }

    /**
     * 批量标记通知为已读。
     */
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(HttpServletRequest request, @RequestBody List<Long> ids) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error(401, "未登录或登录信息已失效");
        }

        if (CollectionUtils.isEmpty(ids)) {
            return Result.error(400, "请选择要标记的消息");
        }

        notificationService.markAllAsRead(userId, ids);
        return Result.success();
    }
}

