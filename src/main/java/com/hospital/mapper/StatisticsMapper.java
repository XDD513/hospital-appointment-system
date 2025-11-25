package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.dto.StatisticsDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 统计数据Mapper接口
 */
@Mapper
public interface StatisticsMapper extends BaseMapper<Object> {
    
    /**
     * 获取管理员统计数据
     */
    StatisticsDTO.AdminStats getAdminStats();
    
    /**
     * 获取患者统计数据
     * @param patientId 患者ID
     */
    StatisticsDTO.PatientStats getPatientStats(@Param("patientId") Long patientId);
    
    /**
     * 获取医生今日统计
     * @param doctorId 医生ID
     * @param date 日期
     */
    StatisticsDTO.DoctorTodayStats getDoctorTodayStats(@Param("doctorId") Long doctorId, @Param("date") LocalDate date);
    
    /**
     * 获取医生评价统计
     * @param doctorId 医生ID
     */
    StatisticsDTO.DoctorReviewStats getDoctorReviewStats(@Param("doctorId") Long doctorId);
    
    /**
     * 获取月度统计
     * @param year 年份
     * @param month 月份
     */
    StatisticsDTO.MonthlyStats getMonthlyStats(@Param("year") Integer year, @Param("month") Integer month);
    
    /**
     * 获取科室统计排行
     * @param params 查询参数
     */
    List<StatisticsDTO.DepartmentStats> getDepartmentStats(@Param("params") Map<String, Object> params);
    
    /**
     * 获取医生统计排行
     * @param params 查询参数
     */
    List<StatisticsDTO.DoctorStats> getDoctorStats(@Param("params") Map<String, Object> params);
    
    /**
     * 获取预约趋势数据
     * @param startDate 开始日期
     * @param endDate 结束日期
     */
    List<StatisticsDTO.TrendData> getAppointmentTrend(@Param("startDate") LocalDate startDate, 
                                                      @Param("endDate") LocalDate endDate);
    
    /**
     * 获取最近预约列表
     * @param limit 限制条数
     */
    List<StatisticsDTO.RecentAppointment> getRecentAppointments(@Param("limit") Integer limit);
    
    /**
     * 获取患者最近预约
     * @param patientId 患者ID
     * @param limit 限制条数
     */
    List<StatisticsDTO.RecentAppointment> getPatientRecentAppointments(@Param("patientId") Long patientId, 
                                                                       @Param("limit") Integer limit);
}
