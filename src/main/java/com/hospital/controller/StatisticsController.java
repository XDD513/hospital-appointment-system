package com.hospital.controller;

import com.hospital.common.result.Result;
import com.hospital.dto.StatisticsDTO;
import com.hospital.service.StatisticsService;
import com.hospital.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计数据控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取管理员统计数据
     */
    @GetMapping("/admin")
    public Result<StatisticsDTO.AdminStats> getAdminStats(HttpServletRequest request) {
        // 验证管理员权限
        Integer roleType = jwtUtil.getRoleTypeFromRequest(request);
        if (roleType == null || roleType != 3) {
            return Result.error(403, "权限不足，仅管理员可访问");
        }
        
        StatisticsDTO.AdminStats stats = statisticsService.getAdminStats();
        return Result.success(stats);
    }

    /**
     * 获取患者统计数据
     */
    @GetMapping("/patient")
    public Result<StatisticsDTO.PatientStats> getPatientStats(HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        StatisticsDTO.PatientStats stats = statisticsService.getPatientStats(userId);
        return Result.success(stats);
    }

    /**
     * 获取医生今日统计
     */
    @GetMapping("/doctor/{doctorId}/today")
    public Result<StatisticsDTO.DoctorTodayStats> getDoctorTodayStats(@PathVariable Long doctorId) {
        StatisticsDTO.DoctorTodayStats stats = statisticsService.getDoctorTodayStats(doctorId);
        return Result.success(stats);
    }

    /**
     * 获取医生评价统计
     */
    @GetMapping("/doctor/{doctorId}/reviews")
    public Result<StatisticsDTO.DoctorReviewStats> getDoctorReviewStats(@PathVariable Long doctorId) {
        StatisticsDTO.DoctorReviewStats stats = statisticsService.getDoctorReviewStats(doctorId);
        return Result.success(stats);
    }

    /**
     * 获取最近预约列表
     */
    @GetMapping("/recent-appointments")
    public Result<List<StatisticsDTO.RecentAppointment>> getRecentAppointments(HttpServletRequest request) {
        // 验证管理员权限
        Integer roleType = jwtUtil.getRoleTypeFromRequest(request);
        if (roleType == null || roleType != 3) {
            return Result.error(403, "权限不足，仅管理员可访问");
        }
        
        List<StatisticsDTO.RecentAppointment> appointments = statisticsService.getRecentAppointments();
        return Result.success(appointments);
    }

    /**
     * 获取患者最近预约
     */
    @GetMapping("/patient/recent-appointments")
    public Result<List<StatisticsDTO.RecentAppointment>> getPatientRecentAppointments(HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        List<StatisticsDTO.RecentAppointment> appointments = statisticsService.getPatientRecentAppointments(userId);
        return Result.success(appointments);
    }

    /**
     * 获取月度统计
     */
    @GetMapping("/monthly")
    public Result<StatisticsDTO.MonthlyStats> getMonthlyStats(HttpServletRequest request) {
        // 验证管理员权限
        Integer roleType = jwtUtil.getRoleTypeFromRequest(request);
        if (roleType == null || roleType != 3) {
            return Result.error(403, "权限不足，仅管理员可访问");
        }
        
        StatisticsDTO.MonthlyStats stats = statisticsService.getMonthlyStats();
        return Result.success(stats);
    }

    /**
     * 获取科室统计排行
     */
    @GetMapping("/department")
    public Result<List<StatisticsDTO.DepartmentStats>> getDepartmentStats(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        // 验证管理员权限
        Integer roleType = jwtUtil.getRoleTypeFromRequest(request);
        if (roleType == null || roleType != 3) {
            return Result.error(403, "权限不足，仅管理员可访问");
        }
        
        List<StatisticsDTO.DepartmentStats> stats = statisticsService.getDepartmentStats(params);
        return Result.success(stats);
    }

    /**
     * 获取医生统计排行
     */
    @GetMapping("/doctor")
    public Result<List<StatisticsDTO.DoctorStats>> getDoctorStats(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        // 验证管理员权限
        Integer roleType = jwtUtil.getRoleTypeFromRequest(request);
        if (roleType == null || roleType != 3) {
            return Result.error(403, "权限不足，仅管理员可访问");
        }
        
        List<StatisticsDTO.DoctorStats> stats = statisticsService.getDoctorStats(params);
        return Result.success(stats);
    }

    /**
     * 获取预约趋势数据
     */
    @GetMapping("/appointment-trend")
    public Result<List<StatisticsDTO.TrendData>> getAppointmentTrend(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        // 验证管理员权限
        Integer roleType = jwtUtil.getRoleTypeFromRequest(request);
        if (roleType == null || roleType != 3) {
            return Result.error(403, "权限不足，仅管理员可访问");
        }
        
        List<StatisticsDTO.TrendData> trends = statisticsService.getAppointmentTrend(params);
        return Result.success(trends);
    }
}
