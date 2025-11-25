package com.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.common.result.Result;
import com.hospital.entity.HealthCheckin;
import com.hospital.entity.HealthPlanRecord;
import com.hospital.entity.UserHealthProfile;

import java.time.LocalDate;
import java.util.Map;

/**
 * 健康档案服务接口
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
public interface HealthProfileService {

    /**
     * 获取用户健康档案
     *
     * @param userId 用户ID
     * @return 健康档案（包含用户基本信息、健康档案、体质类型）
     */
    Result<Map<String, Object>> getHealthProfile(Long userId);

    /**
     * 更新用户健康档案
     *
     * @param profile 健康档案信息
     * @return 更新结果
     */
    Result<UserHealthProfile> updateHealthProfile(UserHealthProfile profile);

    /**
     * 创建健康计划
     *
     * @param plan 健康计划信息
     * @return 创建结果
     */
    Result<HealthPlanRecord> createHealthPlan(HealthPlanRecord plan);

    /**
     * 更新健康计划
     *
     * @param plan 健康计划信息
     * @return 更新结果
     */
    Result<HealthPlanRecord> updateHealthPlan(HealthPlanRecord plan);

    /**
     * 删除健康计划
     *
     * @param id 计划ID
     * @param userId 用户ID
     * @return 删除结果
     */
    Result<Void> deleteHealthPlan(Long id, Long userId);

    /**
     * 分页查询用户的健康计划
     *
     * @param userId 用户ID
     * @param status 状态（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 健康计划列表
     */
    Result<IPage<HealthPlanRecord>> getHealthPlanList(Long userId, Integer status, Integer pageNum, Integer pageSize);

    /**
     * 健康打卡
     *
     * @param checkin 打卡信息
     * @return 打卡结果
     */
    Result<HealthCheckin> healthCheckin(HealthCheckin checkin);

    /**
     * 分页查询用户的打卡记录
     *
     * @param userId 用户ID
     * @param checkinType 打卡类型（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 打卡记录列表
     */
    Result<IPage<HealthCheckin>> getCheckinList(Long userId, String checkinType, LocalDate startDate, LocalDate endDate, Integer pageNum, Integer pageSize);

    /**
     * 查询用户指定日期的打卡记录
     *
     * @param userId 用户ID
     * @param date 日期
     * @return 打卡记录列表
     */
    Result<HealthCheckin> getCheckinByDate(Long userId, LocalDate date);

    /**
     * 获取用户健康统计数据
     *
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计数据
     */
    Result<Map<String, Object>> getHealthStatistics(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 生成用户健康报告
     *
     * @param userId 用户ID
     * @return 健康报告
     */
    Result<Map<String, Object>> generateHealthReport(Long userId);
}

