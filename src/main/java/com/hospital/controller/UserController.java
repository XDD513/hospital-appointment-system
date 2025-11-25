package com.hospital.controller;

import com.hospital.annotation.OperationLog;
import com.hospital.annotation.RateLimit;
import com.hospital.common.result.Result;
import com.hospital.dto.request.LoginRequest;
import com.hospital.dto.request.RegisterRequest;
import com.hospital.dto.request.UpdateUserInfoRequest;
import com.hospital.dto.request.ChangePasswordRequest;
import com.hospital.dto.request.UserSettingsRequest;
import com.hospital.dto.response.UserSettingsResponse;
import com.hospital.dto.response.PatientAppointmentStatsResponse;
import com.hospital.dto.response.LoginResponse;
import com.hospital.dto.response.UserInfoResponse;
import com.hospital.service.OssService;
import com.hospital.service.UserService;
import com.hospital.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户控制器
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OssService ossService;

    /**
     * 用户登录
     */
    @OperationLog(module = "AUTH", type = "SELECT", description = "用户登录")
    @RateLimit(key = "user-login", limit = 20, windowSeconds = 60, perIp = true, perUser = false)
    @PostMapping("/login")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        log.info("用户登录请求: username={}", request.getUsername());
        return userService.login(request);
    }

    /**
     * 用户注册
     */
    @OperationLog(module = "AUTH", type = "INSERT", description = "用户注册")
    @RateLimit(key = "user-register", limit = 5, windowSeconds = 600, perIp = true, perUser = false)
    @PostMapping("/register")
    public Result<Void> register(@Validated @RequestBody RegisterRequest request) {
        log.info("用户注册请求: username={}, phone={}", request.getUsername(), request.getPhone());
        return userService.register(request);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<UserInfoResponse> getUserInfo(HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        return userService.getUserInfo(userId);
    }

    /**
     * 退出登录：删除Redis中的令牌与用户会话键
     */
    @OperationLog(module = "AUTH", type = "UPDATE", description = "用户退出登录")
    @PostMapping("/logout")
    public Result<Boolean> logout(HttpServletRequest request) {
        String token = jwtUtil.getTokenFromRequest(request);
        if (token == null) {
            return Result.error(401, "未携带有效令牌");
        }

        Long userId = jwtUtil.getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error(401, "令牌无效或已过期");
        }

        return userService.logout(userId, token);
    }

    /**
     * 检查用户名是否存在
     */
    @GetMapping("/check/username")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        boolean exists = userService.existsByUsername(username);
        return Result.success(exists);
    }

    /**
     * 检查手机号是否存在
     */
    @GetMapping("/check/phone")
    public Result<Boolean> checkPhone(@RequestParam String phone) {
        boolean exists = userService.existsByPhone(phone);
        return Result.success(exists);
    }

    /**
     * 更新用户信息
     */
    @OperationLog(module = "USER", type = "UPDATE", description = "更新用户信息")
    @PutMapping("/info")
    public Result<Void> updateUserInfo(@Validated @RequestBody UpdateUserInfoRequest request, HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getUserIdFromRequest(httpRequest);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        return userService.updateUserInfo(userId, request);
    }

    /**
     * 修改密码
     */
    @OperationLog(module = "USER", type = "UPDATE", description = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Validated @RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getUserIdFromRequest(httpRequest);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        return userService.changePassword(userId, request);
    }

    /**
     * 获取用户设置
     */
    @GetMapping("/settings")
    public Result<UserSettingsResponse> getUserSettings(HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        return userService.getUserSettings(userId);
    }

    /**
     * 更新用户设置
     */
    @OperationLog(module = "USER", type = "UPDATE", description = "更新用户设置")
    @PutMapping("/settings")
    public Result<Void> updateUserSettings(@RequestBody UserSettingsRequest request, HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getUserIdFromRequest(httpRequest);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        return userService.updateUserSettings(userId, request);
    }

    /**
     * 获取患者预约统计
     */
    @GetMapping("/appointment-stats")
    public Result<PatientAppointmentStatsResponse> getPatientAppointmentStats(HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        return userService.getPatientAppointmentStats(userId);
    }
}


