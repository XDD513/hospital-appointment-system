package com.hospital.service;

import com.hospital.dto.StatisticsDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 统计数据服务接口
 */
public interface StatisticsService {
    
    /**
     * 获取管理员统计数据
     */
    StatisticsDTO.AdminStats getAdminStats();
    
    /**
     * 获取患者统计数据
     * @param patientId 患者ID
     */
    StatisticsDTO.PatientStats getPatientStats(Long patientId);
    
    /**
     * 获取医生今日统计
     * @param doctorId 医生ID
     */
    StatisticsDTO.DoctorTodayStats getDoctorTodayStats(Long doctorId);
    
    /**
     * 获取医生评价统计
     * @param doctorId 医生ID
     */
    StatisticsDTO.DoctorReviewStats getDoctorReviewStats(Long doctorId);
    
    /**
     * 获取月度统计
     */
    StatisticsDTO.MonthlyStats getMonthlyStats();
    
    /**
     * 获取科室统计排行
     * @param params 查询参数
     */
    List<StatisticsDTO.DepartmentStats> getDepartmentStats(Map<String, Object> params);
    
    /**
     * 获取医生统计排行
     * @param params 查询参数
     */
    List<StatisticsDTO.DoctorStats> getDoctorStats(Map<String, Object> params);
    
    /**
     * 获取预约趋势数据
     * @param params 查询参数
     */
    List<StatisticsDTO.TrendData> getAppointmentTrend(Map<String, Object> params);
    
    /**
     * 获取最近预约列表
     */
    List<StatisticsDTO.RecentAppointment> getRecentAppointments();
    
    /**
     * 获取患者最近预约
     * @param patientId 患者ID
     */
    List<StatisticsDTO.RecentAppointment> getPatientRecentAppointments(Long patientId);
}
