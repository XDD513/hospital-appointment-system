package com.hospital.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hospital.common.constant.AppointmentStatus;
import com.hospital.entity.Appointment;
import com.hospital.mapper.AppointmentMapper;
import com.hospital.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 预约爽约自动检测定时任务
 * 如果超过预约时间仍然没有开始接诊，自动将状态修改为NO_SHOW
 *
 * @author Hospital Team
 * @since 2025-11-30
 */
@Slf4j
@Component
public class AppointmentNoShowTask {

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 定时检查并更新爽约预约
     * 每30分钟执行一次
     */
    @Scheduled(cron = "0 */30 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void checkAndUpdateNoShowAppointments() {
        log.info("开始检查爽约预约...");

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        // 查询需要检查的预约：状态为CONFIRMED或IN_PROGRESS，且预约日期是今天或之前
        QueryWrapper<Appointment> wrapper = new QueryWrapper<>();
        wrapper.in("status", AppointmentStatus.CONFIRMED.getCode(), AppointmentStatus.IN_PROGRESS.getCode());
        wrapper.le("appointment_date", today); // 预约日期小于等于今天

        List<Appointment> appointments = appointmentMapper.selectList(wrapper);

        int updatedCount = 0;
        int failedCount = 0;

        for (Appointment appointment : appointments) {
            if (shouldMarkAsNoShow(appointment, now)) {
                try {
                    // 更新数据库状态为NO_SHOW（显式SQL，确保更新成功）
                    int affected = appointmentMapper.updateStatusById(appointment.getId(), AppointmentStatus.NO_SHOW.getCode());

                    if (affected > 0) {
                        // 数据库更新成功
                        updatedCount++;

                        log.info("预约已标记为爽约（数据库已更新）: appointmentId={}, patientId={}, doctorId={}, appointmentDate={}, timeSlot={}",
                                appointment.getId(), appointment.getPatientId(), appointment.getDoctorId(),
                                appointment.getAppointmentDate(), appointment.getTimeSlot());

                        // 失效相关缓存
                        invalidateCache(appointment);
                    } else {
                        // 数据库更新失败（可能已被其他操作更新）
                        failedCount++;
                        log.warn("预约状态更新失败（可能已被修改）: appointmentId={}, 影响行数={}",
                                appointment.getId(), affected);
                    }

                } catch (Exception e) {
                    failedCount++;
                    log.error("更新预约状态为NO_SHOW失败: appointmentId={}, error={}",
                            appointment.getId(), e.getMessage(), e);
                }
            }
        }

        if (updatedCount > 0) {
            log.info("爽约检查完成，共更新{}条预约为NO_SHOW状态（数据库已同步）", updatedCount);
        }
        if (failedCount > 0) {
            log.warn("爽约检查完成，有{}条预约更新失败", failedCount);
        }
        if (updatedCount == 0 && failedCount == 0) {
            log.debug("爽约检查完成，无需更新的预约");
        }
    }

    /**
     * 判断预约是否应该标记为爽约
     *
     * @param appointment 预约信息
     * @param now 当前时间
     * @return true表示应该标记为爽约
     */
    private boolean shouldMarkAsNoShow(Appointment appointment, LocalDateTime now) {
        if (appointment.getAppointmentDate() == null || appointment.getTimeSlot() == null) {
            return false;
        }

        // 计算预约时段的结束时间
        LocalDateTime appointmentEndTime = calculateAppointmentEndTime(appointment);

        if (appointmentEndTime == null) {
            return false;
        }

        // 如果当前时间已经超过预约时段结束时间，则标记为爽约
        return now.isAfter(appointmentEndTime);
    }

    /**
     * 计算预约时段的结束时间
     *
     * @param appointment 预约信息
     * @return 预约时段结束时间
     */
    private LocalDateTime calculateAppointmentEndTime(Appointment appointment) {
        LocalDate appointmentDate = appointment.getAppointmentDate();
        String timeSlot = appointment.getTimeSlot();

        if (appointmentDate == null || timeSlot == null) {
            return null;
        }

        LocalTime endTime;

        // 根据时段确定结束时间
        switch (timeSlot.toUpperCase()) {
            case "MORNING":
                // 上午：9:00-12:00
                endTime = LocalTime.of(12, 0);
                break;
            case "AFTERNOON":
                // 下午：14:00-18:00
                endTime = LocalTime.of(18, 0);
                break;
            case "EVENING":
                // 晚间：19:00-22:00
                endTime = LocalTime.of(22, 0);
                break;
            default:
                // 默认使用晚间结束时间
                endTime = LocalTime.of(22, 0);
                break;
        }

        return appointmentDate.atTime(endTime);
    }

    /**
     * 失效相关缓存并确保数据库同步
     */
    private void invalidateCache(Appointment appointment) {
        try {
            // 失效预约详情缓存
            redisUtil.delete(String.format("hospital:appointment:detail:%d", appointment.getId()));

            // 失效患者预约列表缓存
            if (appointment.getPatientId() != null) {
                redisUtil.deleteByPattern(String.format("hospital:patient:appointment:list:*:patient:%d:*", appointment.getPatientId()));
                redisUtil.deleteByPattern(String.format("hospital:patient:stats:appointments:recent:patient:%d", appointment.getPatientId()));
            }

            // 失效医生预约列表缓存
            if (appointment.getDoctorId() != null) {
                redisUtil.deleteByPattern(String.format("hospital:doctor:appointment:list:*:doctor:%d:*", appointment.getDoctorId()));
                redisUtil.delete(String.format("hospital:doctor:patient:list:pending:doctor:%d", appointment.getDoctorId()));
                redisUtil.delete(String.format("hospital:doctor:patient:list:today:doctor:%d", appointment.getDoctorId()));
                redisUtil.delete(String.format("hospital:doctor:patient:list:completed:doctor:%d", appointment.getDoctorId()));
                // 失效医生今日统计缓存
                String todayKey = String.format("hospital:doctor:stats:today:doctor:%d:date:%s",
                        appointment.getDoctorId(), LocalDate.now());
                redisUtil.delete(todayKey);
            }

            // 失效管理员预约列表缓存
            redisUtil.deleteByPattern("hospital:admin:appointment:list:*");

            // 失效统计缓存（包括最近预约、月度统计等）
            redisUtil.deleteByPattern("hospital:stats:*");
            redisUtil.deleteByPattern("hospital:common:review:v2:*");

            log.debug("缓存已失效: appointmentId={}", appointment.getId());

        } catch (Exception e) {
            log.warn("失效缓存失败: appointmentId={}, error={}", appointment.getId(), e.getMessage());
        }
    }
}

