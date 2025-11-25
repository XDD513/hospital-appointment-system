package com.hospital.controller;

import com.hospital.annotation.OperationLog;
import com.hospital.common.result.Result;
import com.hospital.entity.Doctor;
import com.hospital.service.DoctorService;
import com.hospital.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 医生管理控制器
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Slf4j
@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 查询所有医生列表
     */
    @GetMapping("/list")
    public Result<List<Doctor>> getDoctorList() {
        log.info("查询医生列表");
        return doctorService.getDoctorList();
    }

    /**
     * 查询在职医生列表（给患者端使用）
     */
    @GetMapping("/list/enabled")
    public Result<List<Doctor>> getEnabledDoctorList() {
        log.info("查询在职医生列表");
        return doctorService.getEnabledDoctorList();
    }

    /**
     * 根据科室ID查询医生列表
     */
    @GetMapping("/list/dept/{deptId}")
    public Result<List<Doctor>> getDoctorListByDeptId(@PathVariable Long deptId) {
        log.info("查询科室医生列表: deptId={}", deptId);
        return doctorService.getDoctorListByDeptId(deptId);
    }

    /**
     * 根据ID查询医生详情
     */
    @GetMapping("/{id}")
    public Result<Doctor> getDoctorById(@PathVariable Long id) {
        log.info("查询医生详情: id={}", id);
        return doctorService.getDoctorById(id);
    }

    /**
     * 添加医生（管理员权限）
     */
    @OperationLog(module = "DOCTOR", type = "INSERT", description = "添加医生")
    @PostMapping("/add")
    public Result<Void> addDoctor(@Validated @RequestBody Doctor doctor) {
        log.info("添加医生: userId={}, deptId={}",
                doctor.getUserId(), doctor.getDeptId());
        return doctorService.addDoctor(doctor);
    }

    /**
     * 更新医生信息
     */
    @OperationLog(module = "DOCTOR", type = "UPDATE", description = "更新医生信息")
    @PutMapping("/update")
    public Result<Void> updateDoctor(@Validated @RequestBody Doctor doctor) {
        log.info("更新医生: id={}", doctor.getId());
        return doctorService.updateDoctor(doctor);
    }

    /**
     * 删除医生（管理员权限）
     */
    @OperationLog(module = "DOCTOR", type = "DELETE", description = "删除医生")
    @DeleteMapping("/{id}")
    public Result<Void> deleteDoctor(@PathVariable Long id) {
        log.info("删除医生: id={}", id);
        return doctorService.deleteDoctor(id);
    }

    /**
     * 获取当前登录医生的个人信息
     */
    @GetMapping("/profile")
    public Result<Doctor> getDoctorProfile(HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        log.info("获取医生个人信息: userId={}", userId);
        return doctorService.getDoctorProfileByUserId(userId);
    }

    /**
     * 更新当前登录医生的个人信息
     */
    @OperationLog(module = "DOCTOR", type = "UPDATE", description = "更新医生个人信息")
    @PutMapping("/profile")
    public Result<Void> updateDoctorProfile(@Validated @RequestBody Doctor doctor, HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        log.info("更新医生个人信息: userId={}", userId);
        doctor.setUserId(userId);
        return doctorService.updateDoctorProfile(doctor);
    }

    /**
     * 更新医生状态（管理员权限）
     */
    @OperationLog(module = "DOCTOR", type = "UPDATE", description = "更新医生状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateDoctorStatus(@PathVariable Long id, @RequestBody java.util.Map<String, Integer> request) {
        Integer status = request.get("status");
        log.info("更新医生状态: id={}, status={}", id, status);
        return doctorService.updateDoctorStatus(id, status);
    }

}

