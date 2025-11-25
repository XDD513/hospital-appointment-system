package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.entity.Doctor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 中医师Mapper接口
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Mapper
public interface DoctorMapper extends BaseMapper<Doctor> {

    /**
     * 根据用户ID查询中医师信息
     *
     * @param userId 用户ID
     * @return 中医师对象
     */
    @Select("SELECT * FROM tcm_doctor WHERE user_id = #{userId}")
    Doctor selectByUserId(Long userId);

    /**
     * 根据分类ID查询中医师列表
     *
     * @param categoryId 分类ID
     * @return 中医师列表
     */
    @Select("SELECT * FROM tcm_doctor WHERE category_id = #{categoryId} AND status = 1")
    List<Doctor> selectByCategoryId(Long categoryId);

    /**
     * 根据科室ID查询医生列表（兼容旧接口）
     *
     * @param deptId 科室ID
     * @return 医生列表
     */
    @Select("SELECT * FROM tcm_doctor WHERE category_id = #{deptId} AND status = 1")
    List<Doctor> selectByDeptId(Long deptId);

    /**
     * 查询在职中医师列表
     *
     * @return 中医师列表
     */
    @Select("SELECT * FROM tcm_doctor WHERE status = 1")
    List<Doctor> selectEnabledList();
}

