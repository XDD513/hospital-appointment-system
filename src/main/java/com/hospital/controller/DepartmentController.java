package com.hospital.controller;

import com.hospital.common.result.Result;
import com.hospital.entity.Department;
import com.hospital.mapper.DepartmentMapper;
import com.hospital.service.DepartmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 科室管理控制器
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Slf4j
@RestController
@RequestMapping("/api/department")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private DepartmentMapper departmentMapper;

    /**
     * 查询所有科室列表
     */
    @GetMapping("/list")
    public Result<List<Department>> getDepartmentList() {
        log.info("查询科室列表");
        return departmentService.getDepartmentList();
    }

    /**
     * 查询启用状态的科室列表（给患者端使用）
     */
    @GetMapping("/list/enabled")
    public Result<List<Department>> getEnabledDepartmentList() {
        log.info("查询启用状态的科室列表");
        return departmentService.getEnabledDepartmentList();
    }

    /**
     * 查询启用状态的科室列表（按分类，患者端）
     */
    @GetMapping("/list/by-category/{categoryId}")
    public Result<List<Department>> getEnabledDepartmentListByCategory(@PathVariable Integer categoryId) {
        log.info("查询分类下启用科室列表: categoryId={}", categoryId);
        return departmentService.getEnabledDepartmentListByCategory(categoryId);
    }

    /**
     * 根据ID查询科室详情
     */
    @GetMapping("/{id}")
    public Result<Department> getDepartmentById(@PathVariable Long id) {
        log.info("查询科室详情: id={}", id);
        return departmentService.getDepartmentById(id);
    }

    /**
     * 添加科室（管理员权限）
     */
    @PostMapping("/add")
    public Result<Void> addDepartment(@Validated @RequestBody Department department) {
        log.info("添加科室: deptName={}", department.getDeptName());
        return departmentService.addDepartment(department);
    }

    /**
     * 更新科室信息（管理员权限）
     */
    @PutMapping("/update")
    public Result<Void> updateDepartment(@Validated @RequestBody Department department) {
        log.info("更新科室: id={}, deptName={}", 
                department.getId(), department.getDeptName());
        return departmentService.updateDepartment(department);
    }

    /**
     * 删除科室（管理员权限）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteDepartment(@PathVariable Long id) {
        log.info("删除科室: id={}", id);
        return departmentService.deleteDepartment(id);
    }

    /**
     * 更新科室状态（管理员权限）
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateDepartmentStatus(@PathVariable Long id, @RequestBody java.util.Map<String, Integer> request) {
        Integer status = request.get("status");
        log.info("更新科室状态: id={}, status={}", id, status);
        return departmentService.updateDepartmentStatus(id, status);
    }

    /**
     * 🔍 测试接口：直接查询数据库，绕过缓存
     */
    @GetMapping("/test/direct/{id}")
    public Result<Department> testGetDepartmentDirect(@PathVariable Long id) {
        log.info("🔍 测试 - 直接查询数据库: id={}", id);
        Department dept = departmentMapper.selectById(id);
        if (dept == null) {
            return Result.error("科室不存在");
        }
        log.info("🔍 测试 - 查询结果: id={}, categoryName={}, createTime={}, updateTime={}",
            dept.getId(), dept.getCategoryName(), dept.getCreateTime(), dept.getUpdateTime());
        return Result.success(dept);
    }


}

