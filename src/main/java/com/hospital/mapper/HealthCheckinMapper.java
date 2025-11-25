package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.entity.HealthCheckin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 健康打卡Mapper接口
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Mapper
public interface HealthCheckinMapper extends BaseMapper<HealthCheckin> {

    /**
     * 分页查询用户的打卡记录
     *
     * @param page 分页对象
     * @param userId 用户ID
     * @param checkinType 打卡类型（可选）
     * @return 打卡记录列表
     */
    IPage<HealthCheckin> selectByUserId(Page<HealthCheckin> page,
                                         @Param("userId") Long userId,
                                         @Param("checkinType") String checkinType);

    /**
     * 查询用户指定日期的打卡记录
     *
     * @param userId 用户ID
     * @param checkinDate 打卡日期
     * @return 打卡记录列表
     */
    List<HealthCheckin> selectByUserIdAndDate(@Param("userId") Long userId, @Param("checkinDate") LocalDate checkinDate);

    /**
     * 查询用户指定日期范围的打卡记录
     *
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 打卡记录列表
     */
    List<HealthCheckin> selectByDateRange(@Param("userId") Long userId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    /**
     * 统计用户连续打卡天数
     *
     * @param userId 用户ID
     * @return 连续打卡天数
     */
    Integer countContinuousDays(@Param("userId") Long userId);
}

