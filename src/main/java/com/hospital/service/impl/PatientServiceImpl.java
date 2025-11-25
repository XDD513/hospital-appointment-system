package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.common.constant.SystemConstants;
import com.hospital.common.constant.AppointmentStatus;
import com.hospital.entity.Appointment;
import com.hospital.entity.User;
import com.hospital.mapper.AppointmentMapper;
import com.hospital.mapper.PatientMapper;
import com.hospital.service.PatientService;
import com.hospital.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;

/**
 * 患者管理服务实现类
 */
@Slf4j
@Service
public class PatientServiceImpl extends ServiceImpl<PatientMapper, User> implements PatientService {

    @Autowired
    private PatientMapper patientMapper;

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<Map<String, Object>> getTodayPatients(Long doctorId) {
        log.info("获取今日患者列表，医生ID：{}", doctorId);
        String cacheKey = "hospital:doctor:patient:list:today:doctor:" + doctorId;
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = (List<Map<String, Object>>) cached;
                return list;
            } catch (ClassCastException ignored) {}
        }
        List<Map<String, Object>> data = patientMapper.selectTodayPatients(doctorId);
        // 今日患者列表缓存 2 分钟
        redisUtil.set(cacheKey, data, 2, TimeUnit.MINUTES);
        return data;
    }

    @Override
    public IPage<Map<String, Object>> getHistoryPatients(Long doctorId, Map<String, Object> params) {
        log.info("获取历史患者列表，医生ID：{}，参数：{}", doctorId, params);

        Integer page = (Integer) params.get("page");
        Integer pageSize = (Integer) params.get("pageSize");

        Page<Map<String, Object>> pageObject = new Page<>(page != null ? page : 1, pageSize != null ? pageSize : SystemConstants.DEFAULT_PAGE_SIZE);
        params.put("doctorId", doctorId);

        return patientMapper.selectHistoryPatients(pageObject, params);
    }

    @Override
    public List<Map<String, Object>> getTodayPendingPatients(Long doctorId) {
        log.info("获取待接诊患者列表，医生ID：{}", doctorId);
        String cacheKey = "hospital:doctor:patient:list:pending:doctor:" + doctorId;
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = (List<Map<String, Object>>) cached;
                return list;
            } catch (ClassCastException ignored) {}
        }
        List<Map<String, Object>> data = patientMapper.selectTodayPendingPatients(doctorId);
        // 待接诊患者列表缓存 2 分钟
        redisUtil.set(cacheKey, data, 2, TimeUnit.MINUTES);
        return data;
    }

    @Override
    public List<Map<String, Object>> getTodayCompletedPatients(Long doctorId) {
        log.info("获取已接诊患者列表，医生ID：{}", doctorId);
        String cacheKey = "hospital:doctor:patient:list:completed:doctor:" + doctorId;
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = (List<Map<String, Object>>) cached;
                return list;
            } catch (ClassCastException ignored) {}
        }
        List<Map<String, Object>> data = patientMapper.selectTodayCompletedPatients(doctorId);
        // 已接诊患者列表缓存 2 分钟
        redisUtil.set(cacheKey, data, 2, TimeUnit.MINUTES);
        return data;
    }

    @Override
    public IPage<User> getPatientList(Map<String, Object> params) {
        log.info("获取患者列表，参数：{}", params);

        Integer page = (Integer) params.get("page");
        Integer pageSize = (Integer) params.get("pageSize");

        Page<User> pageObject = new Page<>(page != null ? page : 1, pageSize != null ? pageSize : SystemConstants.DEFAULT_PAGE_SIZE);
        return patientMapper.selectPatientList(pageObject, params);
    }

    @Override
    public boolean addPatient(User patient) {
        log.info("添加患者，用户名：{}", patient.getUsername());

        // 设置角色为患者
        patient.setRoleType(1);
        patient.setStatus(1);

        return save(patient);
    }

    @Override
    public boolean updatePatient(User patient) {
        log.info("更新患者信息，患者ID：{}", patient.getId());
        return updateById(patient);
    }

    @Override
    public boolean deletePatient(Long id) {
        log.info("删除患者，患者ID：{}", id);

        // 检查是否有未完成的预约
        QueryWrapper<Appointment> appointmentWrapper = new QueryWrapper<>();
        appointmentWrapper.eq("user_id", id)
                .in("status", 
                    AppointmentStatus.PENDING_PAYMENT.getCode(),
                    AppointmentStatus.PENDING_VISIT.getCode(),
                    AppointmentStatus.CONFIRMED.getCode(),
                    AppointmentStatus.IN_PROGRESS.getCode());
        long unfinishedAppointmentCount = appointmentMapper.selectCount(appointmentWrapper);
        if (unfinishedAppointmentCount > 0) {
            log.warn("删除患者失败：存在未完成的预约，患者ID={}，未完成预约数={}", id, unfinishedAppointmentCount);
            throw new RuntimeException("该患者存在" + unfinishedAppointmentCount + "个未完成的预约，无法删除");
        }

        return removeById(id);
    }
}
