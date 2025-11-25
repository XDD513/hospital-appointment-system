package com.hospital.controller;

import com.hospital.annotation.OperationLog;
import com.hospital.common.result.Result;
import com.hospital.service.AppointmentService;
import com.hospital.service.DepartmentService;
import com.hospital.service.DoctorService;
import com.hospital.service.ScheduleService;
import com.hospital.service.SystemService;
import com.hospital.util.JwtUtil;
import com.hospital.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 统一缓存管理控制器
 * 集中管理所有缓存刷新操作
 *
 * @author Hospital Team
 * @since 2025-12-19
 */
@Slf4j
@RestController
@RequestMapping("/api/cache")
public class CacheController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private SystemService systemService;

    /**
     * 刷新预约列表缓存
     */
    @OperationLog(module = "CACHE", type = "UPDATE", description = "刷新预约列表缓存")
    @PostMapping("/refresh/appointment")
    public Result<Void> refreshAppointmentCache(HttpServletRequest request) {
        // 验证管理员权限
        Integer roleType = jwtUtil.getRoleTypeFromRequest(request);
        if (roleType == null || roleType != 3) {
            return Result.error(403, "权限不足，仅管理员可访问");
        }

        try {
            redisUtil.deleteByPattern("hospital:admin:appointment:list:*");
            log.info("已刷新预约列表缓存");
            return Result.success("刷新成功");
        } catch (Exception e) {
            log.error("刷新预约列表缓存失败", e);
            return Result.error("刷新失败: " + e.getMessage());
        }
    }

    /**
     * 刷新医生列表缓存
     */
    @OperationLog(module = "CACHE", type = "UPDATE", description = "刷新医生列表缓存")
    @PostMapping("/refresh/doctor")
    public Result<Void> refreshDoctorCache(HttpServletRequest request) {
        // 验证管理员权限
        Integer roleType = jwtUtil.getRoleTypeFromRequest(request);
        if (roleType == null || roleType != 3) {
            return Result.error(403, "权限不足，仅管理员可访问");
        }

        try {
            doctorService.refreshAllDoctorCaches();
            log.info("已刷新医生列表缓存");
            return Result.success("刷新成功");
        } catch (Exception e) {
            log.error("刷新医生列表缓存失败", e);
            return Result.error("刷新失败: " + e.getMessage());
        }
    }

    /**
     * 刷新科室列表缓存
     */
    @OperationLog(module = "CACHE", type = "UPDATE", description = "刷新科室列表缓存")
    @PostMapping("/refresh/department")
    public Result<Void> refreshDepartmentCache(HttpServletRequest request) {
        // 验证管理员权限
        Integer roleType = jwtUtil.getRoleTypeFromRequest(request);
        if (roleType == null || roleType != 3) {
            return Result.error(403, "权限不足，仅管理员可访问");
        }

        try {
            departmentService.refreshAllDepartmentCaches();
            log.info("已刷新科室列表缓存");
            return Result.success("刷新成功");
        } catch (Exception e) {
            log.error("刷新科室列表缓存失败", e);
            return Result.error("刷新失败: " + e.getMessage());
        }
    }

    /**
     * 刷新排班列表缓存
     */
    @OperationLog(module = "CACHE", type = "UPDATE", description = "刷新排班列表缓存")
    @PostMapping("/refresh/schedule")
    public Result<Void> refreshScheduleCache(HttpServletRequest request) {
        // 验证管理员权限
        Integer roleType = jwtUtil.getRoleTypeFromRequest(request);
        if (roleType == null || roleType != 3) {
            return Result.error(403, "权限不足，仅管理员可访问");
        }

        try {
            redisUtil.deleteByPattern("hospital:admin:schedule:list:*");
            log.info("已刷新排班列表缓存");
            return Result.success("刷新成功");
        } catch (Exception e) {
            log.error("刷新排班列表缓存失败", e);
            return Result.error("刷新失败: " + e.getMessage());
        }
    }

    /**
     * 刷新用户列表缓存
     */
    @OperationLog(module = "CACHE", type = "UPDATE", description = "刷新用户列表缓存")
    @PostMapping("/refresh/user")
    public Result<Void> refreshUserCache(HttpServletRequest request) {
        // 验证管理员权限
        Integer roleType = jwtUtil.getRoleTypeFromRequest(request);
        if (roleType == null || roleType != 3) {
            return Result.error(403, "权限不足，仅管理员可访问");
        }

        try {
            systemService.refreshUserListCache();
            log.info("已刷新用户列表缓存");
            return Result.success("刷新成功");
        } catch (Exception e) {
            log.error("刷新用户列表缓存失败", e);
            return Result.error("刷新失败: " + e.getMessage());
        }
    }

    /**
     * 刷新所有缓存
     */
    @OperationLog(module = "CACHE", type = "UPDATE", description = "刷新所有缓存")
    @PostMapping("/refresh/all")
    public Result<Void> refreshAllCache(HttpServletRequest request) {
        // 验证管理员权限
        Integer roleType = jwtUtil.getRoleTypeFromRequest(request);
        if (roleType == null || roleType != 3) {
            return Result.error(403, "权限不足，仅管理员可访问");
        }

        try {
            // 刷新所有缓存
            redisUtil.deleteByPattern("hospital:admin:appointment:list:*");
            doctorService.refreshAllDoctorCaches();
            departmentService.refreshAllDepartmentCaches();
            redisUtil.deleteByPattern("hospital:admin:schedule:list:*");
            systemService.refreshUserListCache();
            log.info("已刷新所有缓存");
            return Result.success("刷新成功");
        } catch (Exception e) {
            log.error("刷新所有缓存失败", e);
            return Result.error("刷新失败: " + e.getMessage());
        }
    }
}

