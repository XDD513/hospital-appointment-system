package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.entity.Department;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 科室Mapper接口（映射到中医分类表）
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {

    /**
     * 查询启用状态的科室列表
     * SQL 定义在 DepartmentMapper.xml 中
     *
     * @return 科室列表
     */
    List<Department> selectEnabledList();

    /**
     * 查询启用状态的科室列表（按ID查询）
     * SQL 定义在 DepartmentMapper.xml 中
     */
    List<Department> selectEnabledListByCategoryId(Integer categoryId);

    /**
     * 查询所有科室列表
     * SQL 定义在 DepartmentMapper.xml 中
     */
    List<Department> selectAllWithCategory();
}

