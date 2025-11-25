package com.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.common.result.Result;
import com.hospital.entity.HealthCheckin;
import com.hospital.entity.HealthPlanRecord;
import com.hospital.entity.UserHealthProfile;
import com.hospital.service.HealthProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 健康档案控制器
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
public class HealthProfileController {

    @Autowired
    private HealthProfileService healthProfileService;

    /**
     * 获取用户健康档案
     *
     * @param userId 用户ID
     * @return 健康档案（包含用户基本信息、健康档案、体质类型）
     */
    @GetMapping("/profile")
    public Result<Map<String, Object>> getHealthProfile(@RequestParam Long userId) {
        log.info("查询用户健康档案：用户ID={}", userId);
        return healthProfileService.getHealthProfile(userId);
    }

    /**
     * 更新用户健康档案
     *
     * @param profile 健康档案信息
     * @return 更新结果
     */
    @PutMapping("/profile")
    public Result<UserHealthProfile> updateHealthProfile(@RequestBody UserHealthProfile profile) {
        log.info("更新用户健康档案：用户ID={}", profile.getUserId());
        return healthProfileService.updateHealthProfile(profile);
    }

    /**
     * 创建健康计划
     *
     * @param plan 健康计划信息
     * @return 创建结果
     */
    @PostMapping("/plan")
    public Result<HealthPlanRecord> createHealthPlan(@RequestBody HealthPlanRecord plan) {
        log.info("创建健康计划：用户ID={}，计划名称={}", plan.getUserId(), plan.getPlanName());
        return healthProfileService.createHealthPlan(plan);
    }

    /**
     * 更新健康计划
     *
     * @param id 计划ID
     * @param plan 健康计划信息
     * @return 更新结果
     */
    @PutMapping("/plan/{id}")
    public Result<HealthPlanRecord> updateHealthPlan(@PathVariable Long id, @RequestBody HealthPlanRecord plan) {
        plan.setId(id);
        log.info("更新健康计划：计划ID={}", id);
        return healthProfileService.updateHealthPlan(plan);
    }

    /**
     * 删除健康计划
     *
     * @param id 计划ID
     * @param userId 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/plan/{id}")
    public Result<Void> deleteHealthPlan(@PathVariable Long id, @RequestParam Long userId) {
        log.info("删除健康计划：计划ID={}，用户ID={}", id, userId);
        return healthProfileService.deleteHealthPlan(id, userId);
    }

    /**
     * 分页查询用户的健康计划
     *
     * @param userId 用户ID
     * @param status 状态（可选）
     * @param pageNum 页码（默认1）
     * @param pageSize 每页数量（默认10）
     * @return 健康计划列表
     */
    @GetMapping("/plan/list")
    public Result<IPage<HealthPlanRecord>> getHealthPlanList(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        log.info("查询用户健康计划：用户ID={}，状态={}，页码={}，每页={}条", userId, status, pageNum, pageSize);
        return healthProfileService.getHealthPlanList(userId, status, pageNum, pageSize);
    }

    /**
     * 健康打卡
     *
     * @param checkin 打卡信息
     * @return 打卡结果
     */
    @PostMapping("/checkin")
    public Result<HealthCheckin> healthCheckin(@RequestBody HealthCheckin checkin) {
        log.info("健康打卡：用户ID={}，打卡类型={}", checkin.getUserId(), checkin.getCheckinType());
        return healthProfileService.healthCheckin(checkin);
    }

    /**
     * 分页查询用户的打卡记录
     *
     * @param userId 用户ID
     * @param checkinType 打卡类型（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param pageNum 页码（默认1）
     * @param pageSize 每页数量（默认100）
     * @return 打卡记录列表
     */
    @GetMapping("/checkin/list")
    public Result<IPage<HealthCheckin>> getCheckinList(
            @RequestParam Long userId,
            @RequestParam(required = false) String checkinType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "100") Integer pageSize) {

        log.info("查询用户打卡记录：用户ID={}，类型={}，日期范围={} ~ {}，页码={}，每页={}条",
                userId, checkinType, startDate, endDate, pageNum, pageSize);
        return healthProfileService.getCheckinList(userId, checkinType, startDate, endDate, pageNum, pageSize);
    }

    /**
     * 查询用户指定日期的打卡记录
     *
     * @param userId 用户ID
     * @param date 日期
     * @return 打卡记录
     */
    @GetMapping("/checkin/date")
    public Result<HealthCheckin> getCheckinByDate(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        
        log.info("查询用户指定日期打卡记录：用户ID={}，日期={}", userId, date);
        return healthProfileService.getCheckinByDate(userId, date);
    }

    /**
     * 获取用户健康统计数据
     *
     * @param userId 用户ID
     * @param startDate 开始日期（可选，默认30天前）
     * @param endDate 结束日期（可选，默认今天）
     * @return 统计数据
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getHealthStatistics(
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30);
        }
        
        log.info("获取用户健康统计数据：用户ID={}，开始日期={}，结束日期={}", userId, startDate, endDate);
        return healthProfileService.getHealthStatistics(userId, startDate, endDate);
    }

    /**
     * 生成用户健康报告
     *
     * @param userId 用户ID
     * @return 健康报告
     */
    @GetMapping("/report")
    public Result<Map<String, Object>> generateHealthReport(@RequestParam Long userId) {
        log.info("生成用户健康报告：用户ID={}", userId);
        return healthProfileService.generateHealthReport(userId);
    }
}

