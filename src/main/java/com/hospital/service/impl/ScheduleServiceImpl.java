package com.hospital.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.common.exception.BusinessException;
import com.hospital.common.result.Result;
import com.hospital.common.result.ResultCode;
import com.hospital.entity.Schedule;
import com.hospital.dto.request.BatchCreateScheduleRequest;
import com.hospital.mapper.ScheduleMapper;
import com.hospital.service.ScheduleService;
import com.hospital.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.time.LocalTime;

@Slf4j
@Service
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, Schedule> implements ScheduleService {

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public Result<List<Schedule>> getDoctorScheduleByDate(Long doctorId, LocalDate scheduleDate) {
        String cacheKey = "hospital:common:schedule:doctor:" + doctorId + ":date:" + scheduleDate;
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<Schedule> list = (List<Schedule>) cached;
                return Result.success(list);
            } catch (ClassCastException ignored) {}
        }

        List<Schedule> schedules = scheduleMapper.selectByDoctorIdAndDate(doctorId, scheduleDate);
        // 医生某日排班缓存（永不过期）
        redisUtil.set(cacheKey, schedules);
        return Result.success(schedules);
    }

    @Override
    public Result<List<Schedule>> getDoctorScheduleByMonth(Long doctorId, String month) {
        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        String cacheKey = "hospital:common:schedule:doctor:" + doctorId + ":month:" + month;
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<Schedule> list = (List<Schedule>) cached;
                return Result.success(list);
            } catch (ClassCastException ignored) {}
        }

        List<Schedule> schedules = scheduleMapper.selectByDoctorIdAndDateRange(doctorId, startDate, endDate);
        // 医生某月排班缓存（永不过期）
        redisUtil.set(cacheKey, schedules);
        return Result.success(schedules);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> addSchedule(Schedule schedule) {
        // 检查是否已存在
        Schedule exist = scheduleMapper.selectByDoctorDateSlot(
            schedule.getDoctorId(), schedule.getScheduleDate(), schedule.getTimeSlot()
        );
        if (exist != null) {
            return Result.error(ResultCode.SCHEDULE_CONFLICT);
        }

        schedule.setRemainingQuota(schedule.getTotalQuota());
        schedule.setStatus("AVAILABLE"); // 可预约
        
        scheduleMapper.insert(schedule);
        // 失效该医生的排班缓存
        if (schedule.getDoctorId() != null) {
            String prefix = "hospital:common:schedule:doctor:" + schedule.getDoctorId() + ":";
            redisUtil.deleteByPattern(prefix + "date:*");
            redisUtil.deleteByPattern(prefix + "month:*");
        }
        return Result.success("添加成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateSchedule(Schedule schedule) {
        scheduleMapper.updateById(schedule);
        // 失效该医生的排班缓存
        if (schedule.getDoctorId() != null) {
            String prefix = "hospital:common:schedule:doctor:" + schedule.getDoctorId() + ":";
            redisUtil.deleteByPattern(prefix + "date:*");
            redisUtil.deleteByPattern(prefix + "month:*");
        }
        return Result.success("更新成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteSchedule(Long id) {
        // 删除前查出医生ID以便失效缓存
        Schedule exist = scheduleMapper.selectById(id);
        scheduleMapper.deleteById(id);
        if (exist != null && exist.getDoctorId() != null) {
            String prefix = "schedule:doctor:" + exist.getDoctorId() + ":";
            redisUtil.deleteByPattern(prefix + "date:*");
            redisUtil.deleteByPattern(prefix + "month:*");
        }
        return Result.success("删除成功");
    }

    @Override
    public boolean checkQuotaAvailable(Long scheduleId) {
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        return schedule != null && schedule.getRemainingQuota() > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> decreaseQuota(Long scheduleId) {
        int result = scheduleMapper.decreaseQuota(scheduleId);
        if (result > 0) {
            // 失效该排班所属医生的缓存
            Schedule exist = scheduleMapper.selectById(scheduleId);
            if (exist != null && exist.getDoctorId() != null) {
                String prefix = "schedule:doctor:" + exist.getDoctorId() + ":";
                redisUtil.deleteByPattern(prefix + "date:*");
                redisUtil.deleteByPattern(prefix + "month:*");
            }
            return Result.success();
        }
        return Result.error(ResultCode.SCHEDULE_FULL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> increaseQuota(Long scheduleId) {
        scheduleMapper.increaseQuota(scheduleId);
        // 失效该排班所属医生的缓存
        Schedule exist = scheduleMapper.selectById(scheduleId);
        if (exist != null && exist.getDoctorId() != null) {
            String prefix = "schedule:doctor:" + exist.getDoctorId() + ":";
            redisUtil.deleteByPattern(prefix + "date:*");
            redisUtil.deleteByPattern(prefix + "month:*");
        }
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> batchCreateSchedules(BatchCreateScheduleRequest request) {
        // 校验
        if (request.getDoctorIds() == null || request.getDoctorIds().isEmpty()) {
            return Result.error("医生ID列表不能为空");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            return Result.error("开始/结束日期不能为空");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            return Result.error("开始日期不能大于结束日期");
        }
        if (request.getTimeSlots() == null || request.getTimeSlots().isEmpty()) {
            return Result.error("至少选择一个时段");
        }
        if (request.getTotalQuota() == null || request.getTotalQuota() <= 0) {
            return Result.error("号源数量必须大于0");
        }

        int created = 0;
        int skipped = 0;
        List<Map<String, Object>> conflicts = new ArrayList<>();

        // 时段映射为开始/结束时间
        Map<String, LocalTime> startTimeMap = new HashMap<>();
        Map<String, LocalTime> endTimeMap = new HashMap<>();
        startTimeMap.put("MORNING", LocalTime.of(8, 0));
        endTimeMap.put("MORNING", LocalTime.of(12, 0));
        startTimeMap.put("AFTERNOON", LocalTime.of(14, 0));
        endTimeMap.put("AFTERNOON", LocalTime.of(18, 0));
        startTimeMap.put("EVENING", LocalTime.of(19, 0));
        endTimeMap.put("EVENING", LocalTime.of(22, 0));

        // 遍历日期范围
        LocalDate current = request.getStartDate();
        while (!current.isAfter(request.getEndDate())) {
            for (String slot : request.getTimeSlots()) {
                LocalTime startTime = startTimeMap.getOrDefault(slot, LocalTime.of(8, 0));
                LocalTime endTime = endTimeMap.getOrDefault(slot, LocalTime.of(12, 0));

                for (Long doctorId : request.getDoctorIds()) {
                    // 冲突检查
                    Schedule exist = scheduleMapper.selectByDoctorDateSlot(doctorId, current, slot);
                    if (exist != null) {
                        skipped++;
                        Map<String, Object> conflict = new HashMap<>();
                        conflict.put("doctorId", doctorId);
                        conflict.put("date", current.toString());
                        conflict.put("timeSlot", slot);
                        conflicts.add(conflict);
                        continue;
                    }

                    Schedule s = new Schedule();
                    s.setDoctorId(doctorId);
                    s.setScheduleDate(current);
                    s.setTimeSlot(slot);
                    s.setStartTime(startTime);
                    s.setEndTime(endTime);
                    s.setTotalQuota(request.getTotalQuota());
                    s.setRemainingQuota(request.getTotalQuota());
                    s.setBookedQuota(0);
                    s.setStatus((request.getStatus() == null || request.getStatus().isEmpty()) ? "AVAILABLE" : request.getStatus());
                    s.setNote(request.getNote());

                    scheduleMapper.insert(s);
                    created++;
                }
            }
            current = current.plusDays(1);
        }

        // 失效相关医生缓存
        for (Long doctorId : new HashSet<>(request.getDoctorIds())) {
            String prefix = "hospital:common:schedule:doctor:" + doctorId + ":";
            redisUtil.deleteByPattern(prefix + "date:*");
            redisUtil.deleteByPattern(prefix + "month:*");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("created", created);
        data.put("skipped", skipped);
        data.put("conflicts", conflicts);
        return Result.success(data);
    }
}

