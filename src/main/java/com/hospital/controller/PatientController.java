package com.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.common.result.Result;
import com.hospital.entity.User;
import com.hospital.service.PatientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 患者管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/patient")
public class PatientController {

    @Autowired
    private PatientService patientService;

    /**
     * 获取今日患者列表（医生端使用）
     */
    @GetMapping("/doctor/{doctorId}/today")
    public Result<List<Map<String, Object>>> getTodayPatients(@PathVariable Long doctorId) {
        List<Map<String, Object>> patients = patientService.getTodayPatients(doctorId);
        return Result.success(patients);
    }

    /**
     * 获取历史患者列表（医生端使用）
     */
    @GetMapping("/doctor/{doctorId}/history")
    public Result<IPage<Map<String, Object>>> getHistoryPatients(@PathVariable Long doctorId,
                                                                @RequestParam Map<String, Object> params) {
        IPage<Map<String, Object>> patients = patientService.getHistoryPatients(doctorId, params);
        return Result.success(patients);
    }

    /**
     * 获取待接诊患者列表
     */
    @GetMapping("/doctor/{doctorId}/pending")
    public Result<List<Map<String, Object>>> getTodayPendingPatients(@PathVariable Long doctorId) {
        List<Map<String, Object>> patients = patientService.getTodayPendingPatients(doctorId);
        return Result.success(patients);
    }

    /**
     * 获取已接诊患者列表
     */
    @GetMapping("/doctor/{doctorId}/completed")
    public Result<List<Map<String, Object>>> getTodayCompletedPatients(@PathVariable Long doctorId) {
        List<Map<String, Object>> patients = patientService.getTodayCompletedPatients(doctorId);
        return Result.success(patients);
    }

    /**
     * 获取所有患者列表（管理员端使用）
     */
    @GetMapping("/list")
    public Result<IPage<User>> getPatientList(@RequestParam Map<String, Object> params) {
        IPage<User> patients = patientService.getPatientList(params);
        return Result.success(patients);
    }

    /**
     * 获取患者详情
     */
    @GetMapping("/{id}")
    public Result<User> getPatientById(@PathVariable Long id) {
        User patient = patientService.getById(id);
        return Result.success(patient);
    }

    /**
     * 添加患者
     */
    @PostMapping("/add")
    public Result<Boolean> addPatient(@RequestBody User patient) {
        boolean result = patientService.addPatient(patient);
        return Result.success(result);
    }

    /**
     * 更新患者信息
     */
    @PutMapping("/update")
    public Result<Boolean> updatePatient(@RequestBody User patient) {
        boolean result = patientService.updatePatient(patient);
        return Result.success(result);
    }

    /**
     * 删除患者
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deletePatient(@PathVariable Long id) {
        boolean result = patientService.deletePatient(id);
        return Result.success(result);
    }
}
