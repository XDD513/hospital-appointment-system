package com.hospital.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hospital.common.result.Result;
import com.hospital.entity.Appointment;

import java.util.List;
import java.util.Map;

public interface AppointmentService extends IService<Appointment> {

    /**
     * 创建预约
     */
    Result<Appointment> createAppointment(Appointment appointment);

    /**
     * 取消预约
     */
    Result<Void> cancelAppointment(Long appointmentId, Long userId);

    /**
     * 查询患者预约列表
     */
    Result<List<Appointment>> getPatientAppointments(Long patientId);

    /**
     * 查询医生预约列表
     */
    Result<List<Appointment>> getDoctorAppointments(Long doctorId);

    /**
     * 查询预约详情
     */
    Result<Appointment> getAppointmentById(Long id);

    /**
     * 导出患者预约记录
     *
     * @param patientId 患者ID
     * @param params    查询参数：状态、起止日期等
     * @return Excel 二进制字节
     */
    byte[] exportPatientAppointments(Long patientId, Map<String, Object> params);
}

