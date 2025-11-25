package com.hospital.service;

import com.hospital.common.result.Result;
import com.hospital.entity.Doctor;

import java.util.List;

/**
 * 医生服务接口
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
public interface DoctorService {

    /**
     * 查询所有医生列表
     *
     * @return 医生列表
     */
    Result<List<Doctor>> getDoctorList();

    /**
     * 查询在职医生列表
     *
     * @return 医生列表
     */
    Result<List<Doctor>> getEnabledDoctorList();

    /**
     * 根据科室ID查询医生列表
     *
     * @param deptId 科室ID
     * @return 医生列表
     */
    Result<List<Doctor>> getDoctorListByDeptId(Long deptId);

    /**
     * 根据ID查询医生详情
     *
     * @param id 医生ID
     * @return 医生详情
     */
    Result<Doctor> getDoctorById(Long id);

    /**
     * 添加医生
     *
     * @param doctor 医生信息
     * @return 添加结果
     */
    Result<Void> addDoctor(Doctor doctor);

    /**
     * 更新医生信息
     *
     * @param doctor 医生信息
     * @return 更新结果
     */
    Result<Void> updateDoctor(Doctor doctor);

    /**
     * 删除医生
     *
     * @param id 医生ID
     * @return 删除结果
     */
    Result<Void> deleteDoctor(Long id);

    /**
     * 根据用户ID获取医生个人信息
     *
     * @param userId 用户ID
     * @return 医生信息
     */
    Result<Doctor> getDoctorProfileByUserId(Long userId);

    /**
     * 更新医生个人信息
     *
     * @param doctor 医生信息
     * @return 更新结果
     */
    Result<Void> updateDoctorProfile(Doctor doctor);

    /**
     * 更新医生状态
     *
     * @param id 医生ID
     * @param status 状态（0-离职，1-在职）
     * @return 更新结果
     */
    Result<Void> updateDoctorStatus(Long id, Integer status);

    /**
     * 刷新所有医生缓存（管理员端）
     */
    void refreshAllDoctorCaches();
}

