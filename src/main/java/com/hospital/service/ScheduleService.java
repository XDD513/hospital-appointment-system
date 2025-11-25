package com.hospital.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hospital.common.result.Result;
import com.hospital.entity.Schedule;
import com.hospital.dto.request.BatchCreateScheduleRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 排班服务接口
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
public interface ScheduleService extends IService<Schedule> {

    /**
     * 查询医生某天的排班
     */
    Result<List<Schedule>> getDoctorScheduleByDate(Long doctorId, LocalDate scheduleDate);

    /**
     * 查询医生某个月的排班
     */
    Result<List<Schedule>> getDoctorScheduleByMonth(Long doctorId, String month);

    /**
     * 添加排班
     */
    Result<Void> addSchedule(Schedule schedule);

    /**
     * 更新排班
     */
    Result<Void> updateSchedule(Schedule schedule);

    /**
     * 删除排班
     */
    Result<Void> deleteSchedule(Long id);

    /**
     * 检查号源是否充足
     */
    boolean checkQuotaAvailable(Long scheduleId);

    /**
     * 扣减号源
     */
    Result<Void> decreaseQuota(Long scheduleId);

    /**
     * 增加号源（取消预约）
     */
    Result<Void> increaseQuota(Long scheduleId);

    /**
     * 批量创建排班（支持多医生、多日期、多时段）
     */
    Result<Map<String, Object>> batchCreateSchedules(BatchCreateScheduleRequest request);
}
