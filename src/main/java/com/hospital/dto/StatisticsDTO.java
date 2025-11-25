package com.hospital.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 统计数据DTO
 */
@Data
public class StatisticsDTO {
    
    /**
     * 管理员统计数据
     */
    @Data
    public static class AdminStats {
        private Integer departments;      // 科室数量
        private Integer doctors;          // 医生数量
        private Integer users;            // 用户数量
        private Integer todayAppointments;// 今日预约数量
    }
    
    /**
     * 患者统计数据
     */
    @Data
    public static class PatientStats {
        private Integer departments;      // 可用科室数
        private Integer doctors;          // 可用医生数
        private Integer myAppointments;   // 我的预约数
        private Integer todayAppointments;// 今日预约数
        private Integer testCount;        // 体质测试次数
        private Integer recipeCount;      // 收藏药膳数量
        private Integer articleCount;     // 发布文章数量
        private Integer checkinDays;      // 连续打卡天数
    }
    
    /**
     * 医生今日统计
     */
    @Data
    public static class DoctorTodayStats {
        private Integer appointments;     // 今日预约数
        private Integer completed;        // 已完成数
        private Integer pending;          // 待接诊数
        private Integer total;            // 累计接诊数
    }
    
    /**
     * 医生评价统计
     */
    @Data
    public static class DoctorReviewStats {
        private BigDecimal avgRating;     // 平均评分
        private Integer totalReviews;     // 总评价数
        private Integer monthlyReviews;   // 本月评价数
        private BigDecimal goodRate;      // 好评率
    }
    
    /**
     * 月度统计
     */
    @Data
    public static class MonthlyStats {
        private Integer totalAppointments;     // 总预约数
        private Integer completedAppointments; // 完成预约数
        private Integer cancelledAppointments; // 取消预约数
        private Integer noShowAppointments;    // 爽约数
    }
    
    /**
     * 科室统计
     */
    @Data
    public static class DepartmentStats {
        private Integer rank;             // 排名
        private String deptName;          // 科室名称
        private Integer appointmentCount; // 预约数量
        private BigDecimal percentage;    // 占比
    }
    
    /**
     * 医生统计
     */
    @Data
    public static class DoctorStats {
        private Integer rank;             // 排名
        private String doctorName;        // 医生姓名
        private String deptName;          // 科室名称
        private Integer appointmentCount; // 预约数量
    }
    
    /**
     * 趋势数据
     */
    @Data
    public static class TrendData {
        private String date;              // 日期
        private Integer totalCount;       // 总数
        private Integer completedCount;   // 完成数
        private Integer cancelledCount;   // 取消数
        private BigDecimal completionRate;// 完成率
    }
    
    /**
     * 最近预约
     */
    @Data
    public static class RecentAppointment {
        private String patientName;       // 患者姓名
        private String doctorName;        // 医生姓名
        private String deptName;          // 科室名称
        private String date;              // 日期
        private String status;            // 状态
    }
}
