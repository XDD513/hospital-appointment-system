package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班Mapper接口
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {

    /**
     * 根据医生ID和日期查询排班
     *
     * @param doctorId 医生ID
     * @param scheduleDate 排班日期
     * @return 排班列表
     */
    List<Schedule> selectByDoctorIdAndDate(@Param("doctorId") Long doctorId, @Param("scheduleDate") LocalDate scheduleDate);

    /**
     * 查询医生某个日期某个时段的排班
     *
     * @param doctorId 医生ID
     * @param scheduleDate 日期
     * @param timeSlot 时段
     * @return 排班对象
     */
    Schedule selectByDoctorDateSlot(@Param("doctorId") Long doctorId, @Param("scheduleDate") LocalDate scheduleDate, @Param("timeSlot") String timeSlot);

    /**
     * 扣减号源（乐观锁）
     *
     * @param id 排班ID
     * @return 影响行数
     */
    int decreaseQuota(@Param("id") Long id);

    /**
     * 增加号源（取消预约时）
     *
     * @param id 排班ID
     * @return 影响行数
     */
    int increaseQuota(@Param("id") Long id);

    /**
     * 查询医生某个月的排班
     *
     * @param doctorId 医生ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 排班列表
     */
    List<Schedule> selectByDoctorIdAndDateRange(@Param("doctorId") Long doctorId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}

