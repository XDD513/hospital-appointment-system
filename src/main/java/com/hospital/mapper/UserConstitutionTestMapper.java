package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.entity.UserConstitutionTest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户体质测试记录Mapper接口
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
@Mapper
public interface UserConstitutionTestMapper extends BaseMapper<UserConstitutionTest> {

    /**
     * 查询用户最新的测试记录
     *
     * @param userId 用户ID
     * @return 测试记录
     */
    UserConstitutionTest selectLatestByUserId(@Param("userId") Long userId);

    /**
     * 查询用户的测试历史记录（按时间倒序）
     *
     * @param userId 用户ID
     * @return 测试记录列表
     */
    List<UserConstitutionTest> selectHistoryByUserId(@Param("userId") Long userId);

    /**
     * 统计用户测试次数
     *
     * @param userId 用户ID
     * @return 测试次数
     */
    Integer countByUserId(@Param("userId") Long userId);

    /**
     * 根据预约ID查询体质测试记录
     *
     * @param appointmentId 预约ID
     * @return 测试记录
     */
    UserConstitutionTest selectByAppointmentId(@Param("appointmentId") Long appointmentId);
}

