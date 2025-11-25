package com.hospital.service;

import com.hospital.common.result.Result;
import com.hospital.entity.Department;

import java.util.List;

/**
 * 科室服务接口
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
public interface DepartmentService {

    /**
     * 查询所有科室列表
     *
     * @return 科室列表
     */
    Result<List<Department>> getDepartmentList();

    /**
     * 查询启用状态的科室列表
     *
     * @return 科室列表
     */
    Result<List<Department>> getEnabledDepartmentList();

    /**
     * 查询启用状态的科室列表（按分类）
     *
     * @param categoryId 分类ID
     * @return 科室列表
     */
    Result<List<Department>> getEnabledDepartmentListByCategory(Integer categoryId);

    /**
     * 根据ID查询科室详情
     *
     * @param id 科室ID
     * @return 科室详情
     */
    Result<Department> getDepartmentById(Long id);

    /**
     * 添加科室
     *
     * @param department 科室信息
     * @return 添加结果
     */
    Result<Void> addDepartment(Department department);

    /**
     * 更新科室信息
     *
     * @param department 科室信息
     * @return 更新结果
     */
    Result<Void> updateDepartment(Department department);

    /**
     * 删除科室
     *
     * @param id 科室ID
     * @return 删除结果
     */
    Result<Void> deleteDepartment(Long id);

    /**
     * 更新科室状态
     *
     * @param id 科室ID
     * @param status 状态（0-禁用，1-启用）
     * @return 更新结果
     */
    Result<Void> updateDepartmentStatus(Long id, Integer status);

    /**
     * 刷新所有分类缓存（管理员端）
     */
    void refreshAllDepartmentCaches();
}

