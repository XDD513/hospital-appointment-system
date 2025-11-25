package com.hospital.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.common.constant.AppointmentStatus;
import com.hospital.common.constant.SystemConstants;
import com.hospital.common.constant.SystemSettingKeys;
import com.hospital.common.exception.BusinessException;
import com.hospital.common.result.Result;
import com.hospital.common.result.ResultCode;
import com.hospital.config.SystemSettingManager;
import com.hospital.dto.AppointmentExportDTO;
import com.hospital.entity.Appointment;
import com.hospital.entity.Department;
import com.hospital.entity.Doctor;
import com.hospital.entity.User;
import com.hospital.mapper.AppointmentMapper;
import com.hospital.mapper.DepartmentMapper;
import com.hospital.mapper.DoctorMapper;
import com.hospital.mapper.UserMapper;
import com.hospital.service.AppointmentService;
import com.hospital.service.ScheduleService;
import com.hospital.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Slf4j
@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements AppointmentService {

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private ScheduleService scheduleService;
    
    @Autowired
    private com.hospital.mapper.ScheduleMapper scheduleMapper;
    
    @Autowired
    private DoctorMapper doctorMapper;
    
    @Autowired
    private DepartmentMapper departmentMapper;
    
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private com.hospital.service.NotificationService notificationService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Appointment> createAppointment(Appointment appointment) {
        try {
            if (Boolean.TRUE.equals(systemSettingManager.getBoolean(SystemSettingKeys.SYSTEM_MAINTENANCE_MODE, Boolean.FALSE))) {
                return Result.error(ResultCode.SERVICE_UNAVAILABLE.getCode(), "系统维护中，暂不支持创建预约");
            }

            if (appointment.getAppointmentDate() == null) {
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "预约日期不能为空");
            }
            LocalDate today = LocalDate.now();
            LocalDate appointmentDate = appointment.getAppointmentDate();
            if (appointmentDate.isBefore(today)) {
                return Result.error(ResultCode.APPOINTMENT_TIME_INVALID.getCode(), "预约日期不能早于今天");
            }
            Integer maxAdvanceDays = systemSettingManager.getInteger(
                    SystemSettingKeys.APPOINTMENT_ADVANCE_DAYS,
                    SystemConstants.ADVANCE_APPOINTMENT_DAYS
            );
            if (maxAdvanceDays != null && maxAdvanceDays > 0) {
                LocalDate latestAllowedDate = today.plusDays(maxAdvanceDays);
                if (appointmentDate.isAfter(latestAllowedDate)) {
                    return Result.error(
                            ResultCode.APPOINTMENT_TIME_INVALID.getCode(),
                            String.format("最多可提前%d天预约，请重新选择日期", maxAdvanceDays)
                    );
                }
            }

            // 1. 查询患者信息并填充
            User patient = userMapper.selectById(appointment.getPatientId());
            if (patient == null) {
                return Result.error(ResultCode.USER_NOT_FOUND);
            }
            appointment.setPatientName(patient.getRealName());
            appointment.setPatientPhone(patient.getPhone());
            appointment.setPatientIdCard(patient.getIdCard());
            
            // 2. 校验排班并设置 scheduleId / deptId，同时生成排队号
            // 根据医生、日期、时段查找排班
            com.hospital.entity.Schedule schedule = scheduleMapper.selectByDoctorDateSlot(
                appointment.getDoctorId(), 
                appointment.getAppointmentDate(), 
                appointment.getTimeSlot()
            );
            if (schedule == null) {
                return Result.error(ResultCode.SCHEDULE_NOT_FOUND.getCode(), "该医生在所选日期/时段无排班");
            }
            // 检查号源是否充足
            if (schedule.getRemainingQuota() == null || schedule.getRemainingQuota() <= 0) {
                return Result.error(ResultCode.SCHEDULE_FULL);
            }
            appointment.setScheduleId(schedule.getId());
            // 关联分类ID（必填字段）
            try {
                com.hospital.entity.Doctor doctor = doctorMapper.selectById(appointment.getDoctorId());
                if (doctor != null) {
                    // 设置分类ID（数据库必填字段）
                    if (doctor.getCategoryId() != null) {
                        appointment.setCategoryId(doctor.getCategoryId());
                    } else {
                        // 如果医生没有分类ID，返回错误
                        return Result.error(ResultCode.PARAM_ERROR.getCode(), "医生分类信息缺失");
                    }
                    // 设置科室ID（用于列表展示和统计，非数据库字段）
                    if (doctor.getDeptId() != null) {
                        appointment.setDeptId(doctor.getDeptId());
                    }
                }
            } catch (Exception e) {
                log.error("查询医生信息失败: doctorId={}, error={}", appointment.getDoctorId(), e.getMessage());
                return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "查询医生信息失败");
            }

            // 生成排队号（根据当天同医生同时段的预约数量）
            Integer queueNumber = generateQueueNumber(
                appointment.getDoctorId(), 
                appointment.getAppointmentDate(), 
                appointment.getTimeSlot()
            );
            appointment.setQueueNumber(queueNumber);
            
            // 3. 设置初始状态（已确认）
            appointment.setStatus(AppointmentStatus.CONFIRMED.getCode());

            // 4. 扣减号源（事务内）并保存预约
            scheduleService.decreaseQuota(schedule.getId());
            appointmentMapper.insert(appointment);

            log.info("创建预约成功: appointmentId={}, patientName={}, queueNumber={}", 
                    appointment.getId(), appointment.getPatientName(), queueNumber);

            // 5. 发送预约确认通知给患者
            try {
                Doctor doctor = doctorMapper.selectById(appointment.getDoctorId());
                String doctorName = doctor != null ? doctor.getDoctorName() : "医生";
                String title = "预约确认";
                String content = String.format("您已成功预约%s的%s时段，排队号：%d，请按时就诊。", 
                        doctorName, appointment.getTimeSlot(), queueNumber);
                notificationService.createAndSendNotification(
                        appointment.getPatientId(), 
                        title, 
                        content, 
                        "APPOINTMENT_CONFIRMED"
                );
            } catch (Exception e) {
                log.warn("发送预约确认通知失败: appointmentId={}, error={}", appointment.getId(), e.getMessage());
            }

            // 6. 发送通知给医生：预约成功
            try {
                com.hospital.entity.Doctor doctor = doctorMapper.selectById(appointment.getDoctorId());
                if (doctor != null && doctor.getUserId() != null) {
                    String patientName = appointment.getPatientName() != null ? appointment.getPatientName() : "患者";
                    String appointmentDateStr = appointment.getAppointmentDate() != null ? 
                            appointment.getAppointmentDate().toString() : "";
                    String timeSlotStr = appointment.getTimeSlot() != null ? 
                            (appointment.getTimeSlot().equals("MORNING") ? "上午" : 
                             appointment.getTimeSlot().equals("AFTERNOON") ? "下午" : 
                             appointment.getTimeSlot().equals("EVENING") ? "晚间" : appointment.getTimeSlot()) : "";
                    String content = String.format("患者%s已成功预约您的%s%s时段，排队号：%d号", 
                            patientName, appointmentDateStr, timeSlotStr, queueNumber);
                    notificationService.createAndSendNotification(
                            doctor.getUserId(),
                            "新预约通知",
                            content,
                            "APPOINTMENT_CREATED"
                    );
                }
            } catch (Exception e) {
                log.warn("发送预约成功通知失败: appointmentId={}, error={}", appointment.getId(), e.getMessage());
            }

            // 7. 仅当预约日期为今日时，失效医生当日患者列表与今日统计缓存（创建预约后需即时刷新）
            try {
                if (appointment.getAppointmentDate() != null && appointment.getAppointmentDate().equals(LocalDate.now())) {
                    Long doctorId = appointment.getDoctorId();
                    if (doctorId != null) {
                        redisUtil.delete("hospital:doctor:patient:list:pending:doctor:" + doctorId);
                        redisUtil.delete("hospital:doctor:patient:list:today:doctor:" + doctorId);
                        redisUtil.delete("hospital:doctor:patient:list:completed:doctor:" + doctorId);
                        String todayKey = "hospital:doctor:stats:today:doctor:" + doctorId + ":date:" + LocalDate.now();
                        redisUtil.delete(todayKey);
                    }
                }
                // 失效患者最近预约缓存（无论预约日期是否为今日，都应刷新）
                if (appointment.getPatientId() != null) {
                    redisUtil.delete("hospital:patient:stats:appointments:recent:patient:" + appointment.getPatientId());
                }
            } catch (Exception e1) {
                log.warn("创建预约后失效缓存失败: appointmentId={}, error={}", appointment.getId(), e1.getMessage());
            }
            return Result.success(appointment);
            
        } catch (Exception e) {
            log.error("创建预约失败", e);
            return Result.error(ResultCode.DB_INSERT_ERROR.getCode(), "创建预约失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成排队号
     * 根据当天同一医生同一时段的预约数量生成
     */
    private Integer generateQueueNumber(Long doctorId, LocalDate appointmentDate, String timeSlot) {
        // 查询当天同医生同时段的预约数量
        Integer count = appointmentMapper.countByDoctorDateTimeSlot(doctorId, appointmentDate, timeSlot);
        return count + 1;
    }

    /**
     * 根据预约时段推断该预约的具体时间点。
     * 缺省情况下认为上午9点。
     */
    private LocalDateTime resolveAppointmentDateTime(Appointment appointment) {
        if (appointment == null || appointment.getAppointmentDate() == null) {
            return null;
        }
        LocalTime baseTime = LocalTime.of(9, 0);
        String slot = appointment.getTimeSlot();
        if ("AFTERNOON".equalsIgnoreCase(slot)) {
            baseTime = LocalTime.of(14, 0);
        } else if ("EVENING".equalsIgnoreCase(slot)) {
            baseTime = LocalTime.of(19, 0);
        }
        return appointment.getAppointmentDate().atTime(baseTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancelAppointment(Long appointmentId, Long userId) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            return Result.error(ResultCode.APPOINTMENT_NOT_FOUND);
        }

        // 检查权限
        if (!appointment.getPatientId().equals(userId)) {
            return Result.error(ResultCode.FORBIDDEN);
        }

        Integer cancelHours = systemSettingManager.getInteger(
                SystemSettingKeys.APPOINTMENT_CANCEL_HOURS,
                SystemConstants.CANCEL_APPOINTMENT_HOURS
        );
        if (cancelHours != null && cancelHours > 0) {
            LocalDateTime appointmentDateTime = resolveAppointmentDateTime(appointment);
            if (appointmentDateTime != null) {
                LocalDateTime latestCancelTime = appointmentDateTime.minusHours(cancelHours);
                if (LocalDateTime.now().isAfter(latestCancelTime)) {
                    return Result.error(
                            ResultCode.APPOINTMENT_CANCEL_TIMEOUT.getCode(),
                            String.format("需提前%d小时取消预约，已超过可取消时间", cancelHours)
                    );
                }
            }
        }

        // 增加号源
        scheduleService.increaseQuota(appointment.getScheduleId());

        // 更新状态
        appointment.setStatus(AppointmentStatus.CANCELLED.getCode());
        appointmentMapper.updateById(appointment);

        // 发送预约取消通知给患者
        try {
            Doctor doctor = doctorMapper.selectById(appointment.getDoctorId());
            String doctorName = doctor != null ? doctor.getDoctorName() : "医生";
            String title = "预约已取消";
            String content = String.format("您已取消%s的%s时段预约，如有需要请重新预约。", 
                    doctorName, appointment.getTimeSlot());
            notificationService.createAndSendNotification(
                    appointment.getPatientId(), 
                    title, 
                    content, 
                    "APPOINTMENT_CANCELLED"
            );
        } catch (Exception e) {
            log.warn("发送预约取消通知失败: appointmentId={}, error={}", appointmentId, e.getMessage());
        }

        // 仅当预约日期为今日时，失效医生当日患者列表与今日统计缓存（取消预约后需即时刷新）
        try {
            if (appointment.getAppointmentDate() != null && appointment.getAppointmentDate().equals(LocalDate.now())) {
                Long doctorId = appointment.getDoctorId();
                if (doctorId != null) {
                    redisUtil.delete("hospital:doctor:patient:list:pending:doctor:" + doctorId);
                    redisUtil.delete("hospital:doctor:patient:list:today:doctor:" + doctorId);
                    redisUtil.delete("hospital:doctor:patient:list:completed:doctor:" + doctorId);
                    String todayKey = "hospital:doctor:stats:today:doctor:" + doctorId + ":date:" + LocalDate.now();
                    redisUtil.delete(todayKey);
                }
            }
            // 失效患者最近预约缓存（取消也需要刷新）
            if (appointment.getPatientId() != null) {
                redisUtil.delete("hospital:patient:stats:appointments:recent:patient:" + appointment.getPatientId());
            }
        } catch (Exception e1) {
            log.warn("取消预约后失效缓存失败: appointmentId={}, error={}", appointmentId, e1.getMessage());
        }

        return Result.success("取消成功");
    }

    @Override
    public Result<List<Appointment>> getPatientAppointments(Long patientId) {
        List<Appointment> appointments = appointmentMapper.selectByPatientId(patientId);
        return Result.success(appointments);
    }

    @Override
    public Result<List<Appointment>> getDoctorAppointments(Long doctorId) {
        List<Appointment> appointments = appointmentMapper.selectByDoctorId(doctorId);
        return Result.success(appointments);
    }

    @Override
    public Result<Appointment> getAppointmentById(Long id) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            return Result.error(ResultCode.APPOINTMENT_NOT_FOUND);
        }
        // 丰富预约信息
        enrichAppointmentInfo(appointment);
        return Result.success(appointment);
    }

    @Override
    public byte[] exportPatientAppointments(Long patientId, Map<String, Object> params) {
        log.info("导出患者预约记录，patientId={}，params={}", patientId, params);
        QueryWrapper<Appointment> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", patientId);

        String status = params != null && params.get("status") != null ? params.get("status").toString() : null;
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status.trim());
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (params != null) {
            String startDate = params.get("startDate") != null ? params.get("startDate").toString() : null;
            String endDate = params.get("endDate") != null ? params.get("endDate").toString() : null;
            if (StringUtils.hasText(startDate)) {
                try {
                    LocalDate begin = LocalDate.parse(startDate.trim(), dateFormatter);
                    wrapper.ge("appointment_date", begin);
                } catch (DateTimeParseException e) {
                    log.warn("导出预约记录-开始日期解析失败：{}", startDate);
                }
            }
            if (StringUtils.hasText(endDate)) {
                try {
                    LocalDate finish = LocalDate.parse(endDate.trim(), dateFormatter);
                    wrapper.le("appointment_date", finish);
                } catch (DateTimeParseException e) {
                    log.warn("导出预约记录-结束日期解析失败：{}", endDate);
                }
            }
        }
        wrapper.orderByDesc("appointment_date");

        List<Appointment> appointments = list(wrapper);
        enrichAppointmentList(appointments);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<AppointmentExportDTO> exportData = appointments.stream()
                .map(appointment -> {
                    AppointmentExportDTO dto = new AppointmentExportDTO();
                    dto.setAppointmentNo(appointment.getId() != null ? appointment.getId().toString() : "-");
                    dto.setDepartmentName(StringUtils.hasText(appointment.getDeptName())
                            ? appointment.getDeptName()
                            : (StringUtils.hasText(appointment.getCategoryName()) ? appointment.getCategoryName() : ""));
                    dto.setDoctorName(StringUtils.hasText(appointment.getDoctorName()) ? appointment.getDoctorName() : "");
                    dto.setAppointmentDate(appointment.getAppointmentDate() != null
                            ? appointment.getAppointmentDate().format(dateFormatter)
                            : "");
                    dto.setTimeSlot(resolveTimeSlotText(appointment.getTimeSlot()));
                    dto.setQueueNumber(appointment.getQueueNumber() != null ? appointment.getQueueNumber().toString() : "");
                    dto.setStatus(resolveStatusText(appointment.getStatus()));
                    dto.setConsultationFee(appointment.getConsultationFee() != null
                            ? appointment.getConsultationFee().stripTrailingZeros().toPlainString()
                            : "");
                    dto.setCreatedAt(appointment.getCreateTime() != null
                            ? appointment.getCreateTime().format(dateTimeFormatter)
                            : "");
                    dto.setUpdatedAt(appointment.getUpdateTime() != null
                            ? appointment.getUpdateTime().format(dateTimeFormatter)
                            : "");
                    return dto;
                })
                .collect(Collectors.toList());

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            EasyExcel.write(outputStream, AppointmentExportDTO.class)
                    .sheet("预约记录")
                    .doWrite(exportData);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("导出患者预约记录失败，patientId={}", patientId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "导出预约记录失败：" + e.getMessage());
        }
    }

    /**
     * 丰富预约信息（关联查询医生、科室、患者信息）
     * 
     * @param appointment 预约对象
     */
    public void enrichAppointmentInfo(Appointment appointment) {
        if (appointment == null) {
            return;
        }

        // 关联查询医生信息
        if (appointment.getDoctorId() != null) {
            try {
                Doctor doctor = doctorMapper.selectById(appointment.getDoctorId());
                if (doctor != null) {
                    appointment.setDoctorName(doctor.getDoctorName());
                    appointment.setDoctorTitle(doctor.getTitle());
                    appointment.setConsultationFee(doctor.getConsultationFee());
                }
            } catch (Exception e) {
                log.warn("获取医生信息失败: doctorId={}", appointment.getDoctorId(), e);
            }
        }

        // 关联查询中医分类信息（直接从预约表的categoryId查询）
        if (appointment.getCategoryId() != null) {
            try {
                Department department = departmentMapper.selectById(appointment.getCategoryId());
                if (department != null) {
                    appointment.setDeptName(department.getCategoryName());
                }
            } catch (Exception e) {
                log.warn("获取科室信息失败: categoryId={}", appointment.getCategoryId(), e);
            }
        }

        // 关联查询患者信息
        if (appointment.getPatientId() != null) {
            try {
                User user = userMapper.selectById(appointment.getPatientId());
                if (user != null) {
                    appointment.setPatientName(user.getRealName());
                    appointment.setPatientPhone(user.getPhone());
                }
            } catch (Exception e) {
                log.warn("获取患者信息失败: patientId={}", appointment.getPatientId(), e);
            }
        }
    }

    /**
     * 丰富预约列表信息（批量处理）
     * 
     * @param appointments 预约列表
     */
    public void enrichAppointmentList(List<Appointment> appointments) {
        if (appointments == null || appointments.isEmpty()) {
            return;
        }
        for (Appointment appointment : appointments) {
            enrichAppointmentInfo(appointment);
        }
    }

    private String resolveTimeSlotText(String timeSlot) {
        if (!StringUtils.hasText(timeSlot)) {
            return "";
        }
        switch (timeSlot) {
            case "MORNING":
                return "上午 08:00-12:00";
            case "AFTERNOON":
                return "下午 14:00-17:00";
            case "EVENING":
                return "晚间 18:00-21:00";
            default:
                return timeSlot;
        }
    }

    private String resolveStatusText(String status) {
        if (!StringUtils.hasText(status)) {
            return "";
        }
        AppointmentStatus statusEnum = AppointmentStatus.getByCode(status);
        return statusEnum != null ? statusEnum.getDesc() : status;
    }

    /**
     * 丰富预约分页信息
     * 
     * @param appointmentPage 预约分页对象
     */
    public void enrichAppointmentPage(IPage<Appointment> appointmentPage) {
        if (appointmentPage == null || appointmentPage.getRecords() == null) {
            return;
        }
        enrichAppointmentList(appointmentPage.getRecords());
    }
}

