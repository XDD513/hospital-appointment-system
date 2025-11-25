package com.hospital.service;

import com.hospital.common.result.Result;
import com.hospital.dto.request.LoginRequest;
import com.hospital.dto.request.RegisterRequest;
import com.hospital.dto.response.LoginResponse;
import com.hospital.dto.response.UserInfoResponse;

/**
 * 用户服务接口
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
public interface UserService {

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应（包含Token）
     */
    Result<LoginResponse> login(LoginRequest request);

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 注册结果
     */
    Result<Void> register(RegisterRequest request);

    /**
     * 获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    Result<UserInfoResponse> getUserInfo(Long userId);

    /**
     * 根据用户名查询用户是否存在
     *
     * @param username 用户名
     * @return true-存在 false-不存在
     */
    boolean existsByUsername(String username);

    /**
     * 根据手机号查询用户是否存在
     *
     * @param phone 手机号
     * @return true-存在 false-不存在
     */
    boolean existsByPhone(String phone);

    /**
     * 退出登录（服务端会话失效）
     *
     * @param userId 用户ID
     * @param token 当前令牌
     * @return 操作结果
     */
    Result<Boolean> logout(Long userId, String token);

    /**
     * 更新用户信息
     *
     * @param userId 用户ID
     * @param request 更新请求
     * @return 操作结果
     */
    Result<Void> updateUserInfo(Long userId, com.hospital.dto.request.UpdateUserInfoRequest request);

    /**
     * 修改密码
     *
     * @param userId 用户ID
     * @param request 修改密码请求
     * @return 操作结果
     */
    Result<Void> changePassword(Long userId, com.hospital.dto.request.ChangePasswordRequest request);

    /**
     * 获取用户设置
     *
     * @param userId 用户ID
     * @return 用户设置
     */
    Result<com.hospital.dto.response.UserSettingsResponse> getUserSettings(Long userId);

    /**
     * 更新用户设置
     *
     * @param userId 用户ID
     * @param request 设置请求
     * @return 操作结果
     */
    Result<Void> updateUserSettings(Long userId, com.hospital.dto.request.UserSettingsRequest request);

    /**
     * 获取患者预约统计
     *
     * @param patientId 患者ID
     * @return 预约统计
     */
    Result<com.hospital.dto.response.PatientAppointmentStatsResponse> getPatientAppointmentStats(Long patientId);
}


