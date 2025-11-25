package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hospital.common.exception.BusinessException;
import com.hospital.common.result.Result;
import com.hospital.common.result.ResultCode;
import com.hospital.entity.Department;
import com.hospital.entity.Doctor;
import com.hospital.mapper.DepartmentMapper;
import com.hospital.mapper.DoctorMapper;
import com.hospital.util.RedisUtil;
import com.hospital.service.DepartmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;
import java.util.List;

/**
 * 科室服务实现类
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Slf4j
@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 查询所有科室列表
     */
    @Override
    public Result<List<Department>> getDepartmentList() {
        try {
            String cacheKey = "hospital:common:dept:list";
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Department> list = (List<Department>) cached;
                    return Result.success(list);
                } catch (ClassCastException ignored) {}
            }

            // 查询中医分类列表
            List<Department> departments = departmentMapper.selectAllWithCategory();

            // 🔍 调试日志：检查数据库查询结果
            if (!departments.isEmpty()) {
                Department first = departments.get(0);
                log.info("🔍 调试 - 第一条记录: id={}, categoryName={}, createTime={}, updateTime={}",
                    first.getId(), first.getCategoryName(), first.getCreateTime(), first.getUpdateTime());
            }

            // 设置兼容字段
            departments.forEach(this::setCompatibilityFields);
            // 科室列表缓存（永久）
            redisUtil.set(cacheKey, departments);
            return Result.success(departments);
        } catch (Exception e) {
            log.error("查询科室列表失败", e);
            return Result.error("查询科室列表失败");
        }
    }

    /**
     * 查询启用状态的科室列表
     */
    @Override
    public Result<List<Department>> getEnabledDepartmentList() {
        try {
            String cacheKey = "hospital:common:dept:list:enabled";
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Department> list = (List<Department>) cached;
                    return Result.success(list);
                } catch (ClassCastException ignored) {}
            }

            List<Department> departments = departmentMapper.selectEnabledList();
            // 设置兼容字段
            departments.forEach(this::setCompatibilityFields);
            // 启用科室列表缓存（永久）
            redisUtil.set(cacheKey, departments);
            return Result.success(departments);
        } catch (Exception e) {
            log.error("查询科室列表失败", e);
            return Result.error("查询科室列表失败");
        }
    }

    /**
     * 查询启用状态的科室列表（按分类）
     */
    @Override
    public Result<List<Department>> getEnabledDepartmentListByCategory(Integer categoryId) {
        try {
            String cacheKey = "hospital:common:dept:list:category:" + categoryId;
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Department> list = (List<Department>) cached;
                    return Result.success(list);
                } catch (ClassCastException ignored) {}
            }

            List<Department> departments = departmentMapper.selectEnabledListByCategoryId(categoryId);
            // 设置兼容字段
            departments.forEach(this::setCompatibilityFields);
            // 指定分类的启用科室列表缓存（永久）
            redisUtil.set(cacheKey, departments);
            return Result.success(departments);
        } catch (Exception e) {
            log.error("查询分类下科室列表失败: categoryId={}", categoryId, e);
            return Result.error("查询分类下科室列表失败");
        }
    }

    /**
     * 根据ID查询科室详情
     */
    @Override
    public Result<Department> getDepartmentById(Long id) {
        String cacheKey = "hospital:common:dept:detail:id:" + id;
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof Department) {
            return Result.success((Department) cached);
        }

        Department department = departmentMapper.selectById(id);
        if (department == null) {
            return Result.error(ResultCode.DEPARTMENT_NOT_FOUND);
        }
        // 设置兼容字段
        setCompatibilityFields(department);
        // 科室详情缓存（永久）
        redisUtil.set(cacheKey, department);
        return Result.success(department);
    }

    /**
     * 添加科室
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> addDepartment(Department department) {
        // 1. 同步兼容字段到实际字段
        syncFieldsBeforeSave(department);

        // 2. 设置默认值
        if (department.getStatus() == null) {
            department.setStatus(1); // 默认启用
        }
        if (department.getSortOrder() == null) {
            department.setSortOrder(0);
        }

        // 3. 保存到数据库
        int result = departmentMapper.insert(department);
        if (result > 0) {
            log.info("添加科室成功: categoryName={}", department.getCategoryName());
            // 主动更新缓存
            refreshAllDepartmentCaches();
            return Result.success("添加成功");
        } else {
            throw new BusinessException(ResultCode.DB_INSERT_ERROR);
        }
    }

    /**
     * 更新科室信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateDepartment(Department department) {
        // 1. 检查科室是否存在
        Department existDept = departmentMapper.selectById(department.getId());
        if (existDept == null) {
            return Result.error(ResultCode.DEPARTMENT_NOT_FOUND);
        }

        // 2. 同步兼容字段到实际字段
        syncFieldsBeforeSave(department);

        // 3. 更新数据库
        int result = departmentMapper.updateById(department);
        if (result > 0) {
            log.info("更新科室成功: id={}, categoryName={}",
                    department.getId(), department.getCategoryName());
            // 主动更新缓存
            refreshAllDepartmentCaches();
            return Result.success("更新成功");
        } else {
            throw new BusinessException(ResultCode.DB_UPDATE_ERROR);
        }
    }

    /**
     * 删除科室
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteDepartment(Long id) {
        // 1. 检查科室是否存在
        Department department = departmentMapper.selectById(id);
        if (department == null) {
            return Result.error(ResultCode.DEPARTMENT_NOT_FOUND);
        }

        // 2. 检查是否有医生关联该科室
        QueryWrapper<Doctor> doctorWrapper = new QueryWrapper<>();
        doctorWrapper.eq("category_id", id);
        long doctorCount = doctorMapper.selectCount(doctorWrapper);
        if (doctorCount > 0) {
            log.warn("删除科室失败：存在关联的医生，科室ID={}，关联医生数={}", id, doctorCount);
            return Result.error(ResultCode.DEPARTMENT_HAS_DOCTORS);
        }

        // 3. 删除科室
        int result = departmentMapper.deleteById(id);
        if (result > 0) {
            log.info("删除科室成功: id={}, categoryName={}", id, department.getCategoryName());
            // 主动更新缓存
            refreshAllDepartmentCaches();
            return Result.success("删除成功");
        } else {
            throw new BusinessException(ResultCode.DB_DELETE_ERROR);
        }
    }

    /**
     * 更新科室状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateDepartmentStatus(Long id, Integer status) {
        log.info("更新科室状态: id={}, status={}", id, status);

        // 1. 检查科室是否存在
        Department existDept = departmentMapper.selectById(id);
        if (existDept == null) {
            return Result.error(ResultCode.DEPARTMENT_NOT_FOUND);
        }

        // 2. 验证状态值
        if (status == null || (status != 0 && status != 1)) {
            return Result.error(400, "状态参数无效");
        }

        // 3. 更新状态
        Department department = new Department();
        department.setId(id);
        department.setStatus(status);

        int result = departmentMapper.updateById(department);
        if (result > 0) {
            log.info("更新科室状态成功: id={}, categoryName={}, status={}",
                    id, existDept.getCategoryName(), status == 1 ? "启用" : "禁用");
            // 主动更新缓存
            refreshAllDepartmentCaches();
            return Result.success(status == 1 ? "启用成功" : "禁用成功");
        } else {
            throw new BusinessException(ResultCode.DB_UPDATE_ERROR);
        }
    }

    /**
     * 设置兼容字段（从数据库字段到兼容字段）
     */
    private void setCompatibilityFields(Department department) {
        if (department == null) {
            return;
        }
        // categoryName -> deptName
        department.setDeptName(department.getCategoryName());
        // categoryDesc -> deptDesc
        department.setDeptDesc(department.getCategoryDesc());
        // categoryDescription -> categoryDesc
        department.setCategoryDescription(department.getCategoryDesc());
    }

    /**
     * 同步兼容字段到实际字段（保存前调用）
     */
    private void syncFieldsBeforeSave(Department department) {
        if (department == null) {
            return;
        }
        // 如果使用了兼容字段，同步到实际字段
        if (department.getDeptName() != null && department.getCategoryName() == null) {
            department.setCategoryName(department.getDeptName());
        }
        if (department.getDeptDesc() != null && department.getCategoryDesc() == null) {
            department.setCategoryDesc(department.getDeptDesc());
        }
    }

    /**
     * 刷新所有科室缓存
     */
    @Override
    public void refreshAllDepartmentCaches() {
        try {
            log.info("开始刷新科室缓存...");

            // 1. 刷新所有科室列表缓存
            List<Department> allDepartments = departmentMapper.selectAllWithCategory();
            allDepartments.forEach(this::setCompatibilityFields);
            redisUtil.set("hospital:common:dept:list", allDepartments);
            log.info("已刷新缓存: hospital:common:dept:list, 共{}条记录", allDepartments.size());

            // 2. 刷新启用科室列表缓存
            List<Department> enabledDepartments = departmentMapper.selectEnabledList();
            enabledDepartments.forEach(this::setCompatibilityFields);
            redisUtil.set("hospital:common:dept:list:enabled", enabledDepartments);
            log.info("已刷新缓存: hospital:common:dept:list:enabled, 共{}条记录", enabledDepartments.size());

            // 3. 刷新按分类的启用科室列表缓存
            redisUtil.deleteByPattern("hospital:common:dept:list:category:*");
            log.info("已删除缓存: hospital:common:dept:list:category:*");

            // 4. 刷新所有科室详情缓存
            for (Department dept : allDepartments) {
                redisUtil.set("hospital:common:dept:detail:id:" + dept.getId(), dept);
            }
            log.info("已刷新{}个科室详情缓存", allDepartments.size());

            log.info("科室缓存刷新成功！");
        } catch (Exception e) {
            log.error("⚠️ 刷新科室缓存失败！这可能导致前端数据不同步", e);
            // 重新抛出异常，让调用方知道缓存刷新失败
            throw new RuntimeException("Redis缓存刷新失败", e);
        }
    }
}

