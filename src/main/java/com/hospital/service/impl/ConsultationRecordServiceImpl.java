package com.hospital.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.common.constant.AppointmentStatus;
import com.hospital.common.constant.SystemConstants;
import com.hospital.dto.ConsultationRecordExportDTO;
import com.hospital.entity.Appointment;
import com.hospital.entity.ConsultationRecord;
import com.hospital.entity.Department;
import com.hospital.entity.Doctor;
import com.hospital.mapper.AppointmentMapper;
import com.hospital.mapper.ConsultationRecordMapper;
import com.hospital.mapper.DepartmentMapper;
import com.hospital.mapper.DoctorMapper;
import com.hospital.messaging.ConsultationReminderPublisher;
import com.hospital.dto.ConsultationReminderMessageDTO;
import com.hospital.service.ConsultationRecordService;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 接诊记录服务实现类
 */
@Slf4j
@Service
public class ConsultationRecordServiceImpl extends ServiceImpl<ConsultationRecordMapper, ConsultationRecord>
        implements ConsultationRecordService {

    @Autowired
    private ConsultationRecordMapper consultationRecordMapper;

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private ConsultationReminderPublisher consultationReminderPublisher;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private com.hospital.service.NotificationService notificationService;

    @Override
    public IPage<ConsultationRecord> getDoctorRecords(Map<String, Object> params) {
        log.info("分页查询医生接诊记录，参数：{}", params);

        // 安全地解析分页参数
        Integer page = 1;
        Integer pageSize = SystemConstants.DEFAULT_PAGE_SIZE;

        try {
            if (params.get("page") != null) {
                page = Integer.parseInt(params.get("page").toString());
            }
        } catch (NumberFormatException e) {
            log.warn("无效的页码参数：{}", params.get("page"));
        }

        try {
            if (params.get("pageSize") != null) {
                pageSize = Integer.parseInt(params.get("pageSize").toString());
            }
        } catch (NumberFormatException e) {
            log.warn("无效的页面大小参数：{}", params.get("pageSize"));
        }

        // 只缓存前2页（30秒），使用参数哈希简化层级
        Map<String, Object> filterParams = new java.util.HashMap<>(params);
        filterParams.remove("page");
        filterParams.remove("pageSize");
        filterParams.remove("doctorId"); // doctorId已经在前缀中
        String cacheKey = redisUtil.buildCacheKey("hospital:doctor:consultation:list", page, pageSize, filterParams);

        if (page <= 2) {
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof IPage) {
                try {
                    @SuppressWarnings("unchecked")
                    IPage<ConsultationRecord> cachedPage = (IPage<ConsultationRecord>) cached;
                    log.info("从缓存获取接诊记录");
                    return cachedPage;
                } catch (ClassCastException ignored) {}
            }
        }

        Page<ConsultationRecord> pageObject = new Page<>(page, pageSize);
        IPage<ConsultationRecord> result = consultationRecordMapper.selectDoctorRecords(pageObject, params);

        // 缓存前2页（5分钟）
        if (page <= 2) {
            redisUtil.set(cacheKey, result, 5, java.util.concurrent.TimeUnit.MINUTES);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createConsultationRecord(ConsultationRecord consultationRecord) {
        log.info("创建接诊记录，预约ID：{}", consultationRecord.getAppointmentId());
        // 先从预约表读取权威字段，避免前端传入的id不一致
        Appointment appointmentSource = appointmentMapper.selectById(consultationRecord.getAppointmentId());
        if (appointmentSource == null) {
            log.warn("创建接诊记录失败：预约不存在，appointmentId={}", consultationRecord.getAppointmentId());
            return false;
        }
        // 幂等处理：按预约ID查已有接诊记录，存在则复用并更新为进行中
        QueryWrapper<ConsultationRecord> existWrapper = new QueryWrapper<>();
        existWrapper.eq("appointment_id", consultationRecord.getAppointmentId());
        ConsultationRecord existing = getOne(existWrapper, false);

        if (existing != null) {
            // 已存在记录：若未完成，则更新为进行中并刷新时间；已完成则直接返回成功
            if (!AppointmentStatus.COMPLETED.getCode().equals(existing.getStatus())) {
                existing.setStatus(AppointmentStatus.IN_PROGRESS.getCode());
                existing.setConsultationDate(LocalDate.now());
                existing.setConsultationTime(LocalTime.now());
                // 校正外键字段与预约表一致
                existing.setPatientId(appointmentSource.getPatientId());
                existing.setDoctorId(appointmentSource.getDoctorId());
                existing.setCategoryId(appointmentSource.getCategoryId());
                // 更新接诊详情字段
                existing.setChiefComplaint(consultationRecord.getChiefComplaint());
                existing.setPresentIllness(consultationRecord.getPresentIllness());
                existing.setPastHistory(consultationRecord.getPastHistory());
                existing.setPhysicalExamination(consultationRecord.getPhysicalExamination());
                existing.setAuxiliaryExamination(consultationRecord.getAuxiliaryExamination());
                existing.setDiagnosis(consultationRecord.getDiagnosis());
                existing.setTreatmentPlan(consultationRecord.getTreatmentPlan());
                existing.setPrescription(consultationRecord.getPrescription());
                existing.setFollowUpAdvice(consultationRecord.getFollowUpAdvice());
                existing.setConsultationFee(consultationRecord.getConsultationFee());
                existing.setDurationMinutes(consultationRecord.getDurationMinutes());
                updateById(existing);
            }
            // 同步预约状态为进行中（显式SQL，确保更新成功）
            int affected = appointmentMapper.updateStatusById(consultationRecord.getAppointmentId(), AppointmentStatus.IN_PROGRESS.getCode());
            if (affected == 0) {
                log.warn("预约状态更新为{}失败，appointmentId={}", AppointmentStatus.IN_PROGRESS.getCode(), consultationRecord.getAppointmentId());
            } else {
                log.info("预约状态已更新为{}，appointmentId={}", AppointmentStatus.IN_PROGRESS.getCode(), consultationRecord.getAppointmentId());
            }

            // 失效医生当日患者列表与统计缓存（开始接诊后需要即时刷新）
            try {
                Long doctorId = appointmentSource.getDoctorId();
                if (doctorId != null) {
                    redisUtil.delete("hospital:doctor:patient:list:pending:doctor:" + doctorId);
                    redisUtil.delete("hospital:doctor:patient:list:today:doctor:" + doctorId);
                    redisUtil.delete("hospital:doctor:patient:list:completed:doctor:" + doctorId);
                    String todayKey = "hospital:doctor:stats:today:doctor:" + doctorId + ":date:" + java.time.LocalDate.now();
                    redisUtil.delete(todayKey);
                }
            } catch (Exception e) {
                log.warn("开始接诊后失效缓存失败: appointmentId={}, error={}", consultationRecord.getAppointmentId(), e.getMessage());
            }
            return true;
        }

        // 设置接诊日期和时间
        consultationRecord.setConsultationDate(LocalDate.now());
        consultationRecord.setConsultationTime(LocalTime.now());
        consultationRecord.setStatus(AppointmentStatus.IN_PROGRESS.getCode());
        // 使用预约表字段填充外键，确保联表查询与统计一致
        consultationRecord.setPatientId(appointmentSource.getPatientId());
        consultationRecord.setDoctorId(appointmentSource.getDoctorId());
        consultationRecord.setCategoryId(appointmentSource.getCategoryId());

        // 保存接诊记录
        boolean result = save(consultationRecord);

        if (result) {
            // 更新预约状态为接诊中（显式SQL，确保更新成功）
            int affected = appointmentMapper.updateStatusById(consultationRecord.getAppointmentId(), AppointmentStatus.IN_PROGRESS.getCode());
            if (affected == 0) {
                log.warn("预约状态更新为{}失败，appointmentId={}", AppointmentStatus.IN_PROGRESS.getCode(), consultationRecord.getAppointmentId());
            } else {
                log.info("预约状态已更新为{}，appointmentId={}", AppointmentStatus.IN_PROGRESS.getCode(), consultationRecord.getAppointmentId());
            }

            // 失效医生当日患者列表与统计缓存
            try {
                Long doctorId = appointmentSource.getDoctorId();
                if (doctorId != null) {
                    redisUtil.delete("hospital:doctor:patient:list:pending:doctor:" + doctorId);
                    redisUtil.delete("hospital:doctor:patient:list:today:doctor:" + doctorId);
                    redisUtil.delete("hospital:doctor:patient:list:completed:doctor:" + doctorId);
                    String todayKey = "hospital:doctor:stats:today:doctor:" + doctorId + ":date:" + java.time.LocalDate.now();
                    redisUtil.delete(todayKey);
                }
            } catch (Exception e) {
                log.warn("开始接诊后失效缓存失败: appointmentId={}, error={}", consultationRecord.getAppointmentId(), e.getMessage());
            }
        }

        return result;
    }

    @Override
    public boolean updateConsultationRecord(ConsultationRecord consultationRecord) {
        log.info("更新接诊记录，记录ID：{}", consultationRecord.getId());
        return updateById(consultationRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startConsultation(Long appointmentId) {
        log.info("开始接诊，预约ID：{}", appointmentId);

        // 1. 查询预约信息
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            log.warn("开始接诊失败：预约不存在，appointmentId={}", appointmentId);
            return false;
        }

        // 2. 更新预约状态为接诊中
        int affected = appointmentMapper.updateStatusById(appointmentId, AppointmentStatus.IN_PROGRESS.getCode());
        if (affected == 0) {
            log.warn("预约状态更新为{}失败，appointmentId={}", AppointmentStatus.IN_PROGRESS.getCode(), appointmentId);
            return false;
        }
        log.info("预约状态已更新为{}，appointmentId={}", AppointmentStatus.IN_PROGRESS.getCode(), appointmentId);

        // 3. 发送体质测试提醒给患者
        Long patientId = appointment.getPatientId();
        try {
            if (patientId != null) {
                sendConstitutionReminder(appointmentId, appointment, appointment.getDoctorId(),
                        appointment.getPatientId(), appointment.getCategoryId());
            }
        } catch (Exception e) {
            log.error("开始接诊后发送体质测试提醒失败，appointmentId={}, error={}", appointmentId, e.getMessage(), e);
        }

        // 4. 失效医生当日患者列表与统计缓存
        try {
            Long doctorId = appointment.getDoctorId();
            if (doctorId != null) {
                redisUtil.delete("hospital:doctor:patient:list:pending:doctor:" + doctorId);
                redisUtil.delete("hospital:doctor:patient:list:today:doctor:" + doctorId);
                redisUtil.delete("hospital:doctor:patient:list:completed:doctor:" + doctorId);
                String todayKey = "hospital:doctor:stats:today:doctor:" + doctorId + ":date:" + LocalDate.now();
                redisUtil.delete(todayKey);
            }
        } catch (Exception e) {
            log.warn("开始接诊后失效缓存失败: appointmentId={}, error={}", appointmentId, e.getMessage());
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeConsultation(Long appointmentId, ConsultationRecord consultationRecord) {
        log.info("完成接诊，预约ID：{}", appointmentId);

        // 从预约表读取权威字段
        Appointment appointmentSource = appointmentMapper.selectById(appointmentId);
        if (appointmentSource == null) {
            log.warn("完成接诊失败：预约不存在，appointmentId={}", appointmentId);
            return false;
        }

        Long doctorId = appointmentSource.getDoctorId();
        Long patientId = appointmentSource.getPatientId();
        Long categoryId = appointmentSource.getCategoryId();

        // 查找预约对应的接诊记录
        QueryWrapper<ConsultationRecord> existWrapper = new QueryWrapper<>();
        existWrapper.eq("appointment_id", appointmentId);
        ConsultationRecord existing = getOne(existWrapper, false);

        boolean recordResult;
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        if (existing == null) {
            // 不存在记录则创建已完成记录，保证幂等
            ConsultationRecord newRecord = new ConsultationRecord();
            newRecord.setAppointmentId(appointmentId);
            newRecord.setDoctorId(doctorId);
            newRecord.setPatientId(patientId);
            newRecord.setCategoryId(categoryId);
            newRecord.setConsultationDate(today);
            newRecord.setConsultationTime(now);
            newRecord.setStatus(AppointmentStatus.COMPLETED.getCode());
            // 复制接诊详情字段
            newRecord.setChiefComplaint(consultationRecord.getChiefComplaint());
            newRecord.setPresentIllness(consultationRecord.getPresentIllness());
            newRecord.setPastHistory(consultationRecord.getPastHistory());
            newRecord.setPhysicalExamination(consultationRecord.getPhysicalExamination());
            newRecord.setAuxiliaryExamination(consultationRecord.getAuxiliaryExamination());
            newRecord.setDiagnosis(consultationRecord.getDiagnosis());
            newRecord.setTreatmentPlan(consultationRecord.getTreatmentPlan());
            newRecord.setPrescription(consultationRecord.getPrescription());
            newRecord.setFollowUpAdvice(consultationRecord.getFollowUpAdvice());
            newRecord.setConsultationFee(consultationRecord.getConsultationFee());
            newRecord.setDurationMinutes(consultationRecord.getDurationMinutes());
            recordResult = save(newRecord);
        } else {
            // 已存在记录则更新为完成，并刷新时间
            existing.setStatus(AppointmentStatus.COMPLETED.getCode());
            existing.setConsultationDate(today);
            existing.setConsultationTime(now);
            // 纠正外键字段
            existing.setPatientId(patientId);
            existing.setDoctorId(doctorId);
            existing.setCategoryId(categoryId);
            // 更新接诊详情字段
            existing.setChiefComplaint(consultationRecord.getChiefComplaint());
            existing.setPresentIllness(consultationRecord.getPresentIllness());
            existing.setPastHistory(consultationRecord.getPastHistory());
            existing.setPhysicalExamination(consultationRecord.getPhysicalExamination());
            existing.setAuxiliaryExamination(consultationRecord.getAuxiliaryExamination());
            existing.setDiagnosis(consultationRecord.getDiagnosis());
            existing.setTreatmentPlan(consultationRecord.getTreatmentPlan());
            existing.setPrescription(consultationRecord.getPrescription());
            existing.setFollowUpAdvice(consultationRecord.getFollowUpAdvice());
            existing.setConsultationFee(consultationRecord.getConsultationFee());
            existing.setDurationMinutes(consultationRecord.getDurationMinutes());
            recordResult = updateById(existing);
        }

        // 更新预约状态为已完成（显式SQL，确保更新成功）
        int affected = appointmentMapper.updateStatusById(appointmentId, AppointmentStatus.COMPLETED.getCode());
        if (affected == 0) {
            log.warn("预约状态更新为{}失败，appointmentId={}", AppointmentStatus.COMPLETED.getCode(), appointmentId);
        } else {
            log.info("预约状态已更新为{}，appointmentId={}", AppointmentStatus.COMPLETED.getCode(), appointmentId);
        }

        // 更新医生接诊统计
        updateDoctorConsultationStats(doctorId);

        // 发送通知给患者：完成接诊
        try {
            if (patientId != null) {
                String doctorName = appointmentSource.getDoctorName();
                if (doctorName == null && doctorId != null) {
                    Doctor doctor = doctorMapper.selectById(doctorId);
                    if (doctor != null) {
                        doctorName = doctor.getDoctorName();
                    }
                }
                String resolvedDoctorName = doctorName != null ? doctorName : "医生";
                String content = String.format("您在%s的接诊已完成，请查看接诊记录详情", resolvedDoctorName);
                notificationService.createAndSendNotification(
                        patientId,
                        "预约完成通知",
                        content,
                        "APPOINTMENT_COMPLETED"
                );
            }
        } catch (Exception e) {
            log.warn("发送接诊完成通知失败: appointmentId={}, error={}", appointmentId, e.getMessage());
        }

        // 失效医生当日患者列表与统计缓存（完成接诊后需要即时刷新）
        try {
            if (doctorId != null) {
                redisUtil.delete("hospital:doctor:patient:list:pending:doctor:" + doctorId);
                redisUtil.delete("hospital:doctor:patient:list:today:doctor:" + doctorId);
                redisUtil.delete("hospital:doctor:patient:list:completed:doctor:" + doctorId);
                String todayKey = "hospital:doctor:stats:today:doctor:" + doctorId + ":date:" + java.time.LocalDate.now();
                redisUtil.delete(todayKey);
            }
            // 失效患者最近预约缓存（无论预约日期是否为今日，都应刷新）
            if (patientId != null) {
                redisUtil.delete("hospital:patient:stats:appointments:recent:patient:" + patientId);
            }
        } catch (Exception e) {
            log.warn("完成接诊后失效缓存失败: appointmentId={}, error={}", appointmentId, e.getMessage());
        }

        return recordResult && affected > 0;
    }

    private void sendConstitutionReminder(Long appointmentId, Appointment appointmentSource, Long doctorId,
                                          Long patientId, Long categoryId) {
        if (patientId == null) {
            return;
        }

        String doctorName = appointmentSource.getDoctorName();
        if (doctorName == null && doctorId != null) {
            Doctor doctor = doctorMapper.selectById(doctorId);
            if (doctor != null) {
                doctorName = doctor.getDoctorName();
                if (categoryId == null) {
                    categoryId = doctor.getCategoryId();
                }
            }
        }

        String categoryName = appointmentSource.getCategoryName();
        if (categoryName == null && categoryId != null) {
            Department department = departmentMapper.selectById(categoryId);
            if (department != null) {
                categoryName = department.getCategoryName();
            }
        }

        String resolvedDoctorName = doctorName != null ? doctorName : "医生";
        String resolvedCategoryName = categoryName != null ? categoryName : "相关科室";
        String messageContent = String.format("您在%s的%s就诊已完成，建议尽快完成体质测试，以便获得更精准的养生方案。",
                resolvedCategoryName, resolvedDoctorName);

        ConsultationReminderMessageDTO reminderMessage = ConsultationReminderMessageDTO.builder()
                .appointmentId(appointmentId)
                .patientId(patientId)
                .doctorName(resolvedDoctorName)
                .categoryName(resolvedCategoryName)
                .message(messageContent)
                .createdAt(LocalDateTime.now())
                .build();

        consultationReminderPublisher.publish(reminderMessage);
    }

    @Override
    public byte[] exportConsultationRecords(Long doctorId, Map<String, Object> params) {
        log.info("导出接诊记录，医生ID：{}，参数：{}", doctorId, params);

        try {
            // 查询所有符合条件的接诊记录（不分页，限制最多10000条）
            params.put("doctorId", doctorId);
            Page<ConsultationRecord> page = new Page<>(1, 10000);
            IPage<ConsultationRecord> result = consultationRecordMapper.selectDoctorRecords(page, params);
            List<ConsultationRecord> records = result.getRecords();

            if (records.isEmpty()) {
                log.warn("没有找到需要导出的接诊记录，医生ID：{}", doctorId);
                // 返回一个空的Excel文件
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                EasyExcel.write(outputStream, ConsultationRecordExportDTO.class)
                    .sheet("接诊记录")
                    .doWrite(List.of());
                return outputStream.toByteArray();
            }

            // 转换为导出DTO
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            List<ConsultationRecordExportDTO> exportData = records.stream()
                .map(record -> {
                    ConsultationRecordExportDTO dto = new ConsultationRecordExportDTO();

                    // 合并接诊日期和时间
                    String consultationDateTime = "";
                    if (record.getConsultationDate() != null && record.getConsultationTime() != null) {
                        LocalDateTime dateTime = LocalDateTime.of(record.getConsultationDate(), record.getConsultationTime());
                        consultationDateTime = dateTime.format(dateTimeFormatter);
                    } else if (record.getConsultationDate() != null) {
                        consultationDateTime = record.getConsultationDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    }
                    dto.setConsultationDateTime(consultationDateTime);

                    // 完成就诊时间（当状态为已完成时，使用updated_at）
                    String completedTime = "";
                    if (AppointmentStatus.COMPLETED.getCode().equals(record.getStatus()) && record.getUpdatedAt() != null) {
                        completedTime = record.getUpdatedAt().format(dateTimeFormatter);
                    }
                    dto.setCompletedTime(completedTime);

                    // 患者信息
                    dto.setPatientName(StringUtils.hasText(record.getPatientName())
                        ? record.getPatientName() : "未知患者");
                    // 性别已经在Mapper中转换为"男"/"女"，直接使用
                    dto.setGender(StringUtils.hasText(record.getGender())
                        ? record.getGender() : "");
                    dto.setAge(record.getAge() != null ? String.valueOf(record.getAge()) : "");

                    // 医生和科室信息
                    dto.setDoctorName(StringUtils.hasText(record.getDoctorName())
                        ? record.getDoctorName() : "未知医生");
                    dto.setCategoryName(StringUtils.hasText(record.getCategoryName())
                        ? record.getCategoryName() : "");

                    // 接诊信息
                    dto.setChiefComplaint(StringUtils.hasText(record.getChiefComplaint())
                        ? record.getChiefComplaint() : "");
                    dto.setPresentIllness(StringUtils.hasText(record.getPresentIllness())
                        ? record.getPresentIllness() : "");
                    dto.setPastHistory(StringUtils.hasText(record.getPastHistory())
                        ? record.getPastHistory() : "");
                    dto.setPhysicalExamination(StringUtils.hasText(record.getPhysicalExamination())
                        ? record.getPhysicalExamination() : "");
                    dto.setAuxiliaryExamination(StringUtils.hasText(record.getAuxiliaryExamination())
                        ? record.getAuxiliaryExamination() : "");
                    dto.setDiagnosis(StringUtils.hasText(record.getDiagnosis())
                        ? record.getDiagnosis() : "");
                    dto.setTreatmentPlan(StringUtils.hasText(record.getTreatmentPlan())
                        ? record.getTreatmentPlan() : "");
                    dto.setFollowUpAdvice(StringUtils.hasText(record.getFollowUpAdvice())
                        ? record.getFollowUpAdvice() : "");

                    // 费用和时长
                    dto.setConsultationFee(record.getConsultationFee() != null
                        ? record.getConsultationFee().toString() : "");
                    dto.setDurationMinutes(record.getDurationMinutes() != null
                        ? String.valueOf(record.getDurationMinutes()) : "");

                    // 状态
                    if (StringUtils.hasText(record.getStatus())) {
                        if (AppointmentStatus.COMPLETED.getCode().equals(record.getStatus())) {
                            dto.setStatus("已完成");
                        } else if (AppointmentStatus.IN_PROGRESS.getCode().equals(record.getStatus())) {
                            dto.setStatus("进行中");
                        } else {
                            dto.setStatus(record.getStatus());
                        }
                    } else {
                        dto.setStatus("");
                    }

                    return dto;
                })
                .collect(Collectors.toList());

            // 使用EasyExcel导出
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            EasyExcel.write(outputStream, ConsultationRecordExportDTO.class)
                .sheet("接诊记录")
                .doWrite(exportData);

            log.info("导出接诊记录成功，医生ID：{}，记录数：{}", doctorId, exportData.size());
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("导出接诊记录失败，医生ID：{}", doctorId, e);
            throw new RuntimeException("导出接诊记录失败: " + e.getMessage());
        }
    }

    /**
     * 更新医生接诊统计
     */
    private void updateDoctorConsultationStats(Long doctorId) {
        if (doctorId == null) {
            log.warn("更新医生接诊统计失败：医生ID为空");
            return;
        }

        try {
            // 统计医生总接诊数（已完成状态）
            QueryWrapper<ConsultationRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("doctor_id", doctorId)
                   .eq("status", AppointmentStatus.COMPLETED.getCode());
            long consultationCount = count(wrapper);

            // 更新医生表中的接诊数统计
            Doctor doctor = doctorMapper.selectById(doctorId);
            if (doctor != null) {
                doctor.setConsultationCount((int) consultationCount);
                doctorMapper.updateById(doctor);
                log.info("更新医生接诊统计成功：医生ID={}，总接诊数={}", doctorId, consultationCount);
            } else {
                log.warn("更新医生接诊统计失败：医生不存在，医生ID={}", doctorId);
            }
        } catch (Exception e) {
            log.error("更新医生接诊统计失败：医生ID={}", doctorId, e);
            // 不抛出异常，避免影响主流程
        }
    }

    @Override
    public ConsultationRecord getConsultationRecordDetail(Long id) {
        log.info("获取接诊记录详情，记录ID：{}", id);
        return consultationRecordMapper.selectRecordDetailById(id);
    }
}
