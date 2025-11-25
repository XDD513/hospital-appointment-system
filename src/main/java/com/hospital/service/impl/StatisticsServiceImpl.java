package com.hospital.service.impl;

import com.hospital.dto.StatisticsDTO;
import com.hospital.mapper.StatisticsMapper;
import com.hospital.service.StatisticsService;
import com.hospital.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 统计数据服务实现类
 */
@Slf4j
@Service
public class StatisticsServiceImpl implements StatisticsService {
    
    @Autowired
    private StatisticsMapper statisticsMapper;
    
    @Autowired
    private RedisUtil redisUtil;
    
    @Override
    public StatisticsDTO.AdminStats getAdminStats() {
        log.info("获取管理员统计数据");
        String cacheKey = "hospital:admin:stats:overview";
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof StatisticsDTO.AdminStats) {
            return (StatisticsDTO.AdminStats) cached;
        }
        StatisticsDTO.AdminStats data = statisticsMapper.getAdminStats();
        // 管理员统计数据，缓存 10 分钟
        redisUtil.set(cacheKey, data, 10, TimeUnit.MINUTES);
        return data;
    }
    
    @Override
    public StatisticsDTO.PatientStats getPatientStats(Long patientId) {
        log.info("获取患者统计数据，患者ID：{}", patientId);
        String cacheKey = "hospital:patient:stats:overview:patient:" + patientId;
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof StatisticsDTO.PatientStats) {
            return (StatisticsDTO.PatientStats) cached;
        }
        StatisticsDTO.PatientStats data = statisticsMapper.getPatientStats(patientId);
        // 患者统计数据，缓存 10 分钟
        redisUtil.set(cacheKey, data, 10, TimeUnit.MINUTES);
        return data;
    }
    
    @Override
    public StatisticsDTO.DoctorTodayStats getDoctorTodayStats(Long doctorId) {
        log.info("获取医生今日统计，医生ID：{}", doctorId);
        LocalDate today = LocalDate.now();
        String cacheKey = "hospital:doctor:stats:today:doctor:" + doctorId + ":date:" + today;
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof StatisticsDTO.DoctorTodayStats) {
            return (StatisticsDTO.DoctorTodayStats) cached;
        }
        StatisticsDTO.DoctorTodayStats data = statisticsMapper.getDoctorTodayStats(doctorId, today);
        // 医生今日统计，缓存 5 分钟
        redisUtil.set(cacheKey, data, 5, TimeUnit.MINUTES);
        return data;
    }
    
    @Override
    public StatisticsDTO.DoctorReviewStats getDoctorReviewStats(Long doctorId) {
        log.info("获取医生评价统计，医生ID：{}", doctorId);
        String cacheKey = "stats:doctor:" + doctorId + ":review";
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof StatisticsDTO.DoctorReviewStats) {
            return (StatisticsDTO.DoctorReviewStats) cached;
        }
        StatisticsDTO.DoctorReviewStats data = statisticsMapper.getDoctorReviewStats(doctorId);
        if (data == null) {
            data = new StatisticsDTO.DoctorReviewStats();
            data.setAvgRating(BigDecimal.ZERO);
            data.setGoodRate(BigDecimal.ZERO);
            data.setTotalReviews(0);
            data.setMonthlyReviews(0);
        }
        // 医生评价统计，缓存 15 分钟
        redisUtil.set(cacheKey, data, 15, TimeUnit.MINUTES);
        return data;
    }
    
    @Override
    public StatisticsDTO.MonthlyStats getMonthlyStats() {
        log.info("获取月度统计");
        LocalDate now = LocalDate.now();
        String cacheKey = "stats:monthly:" + now.getYear() + ":" + now.getMonthValue();
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof StatisticsDTO.MonthlyStats) {
            return (StatisticsDTO.MonthlyStats) cached;
        }
        StatisticsDTO.MonthlyStats data = statisticsMapper.getMonthlyStats(now.getYear(), now.getMonthValue());
        // 月度统计，缓存 30 分钟
        redisUtil.set(cacheKey, data, 30, TimeUnit.MINUTES);
        return data;
    }
    
    @Override
    public List<StatisticsDTO.DepartmentStats> getDepartmentStats(Map<String, Object> params) {
        log.info("获取科室统计排行，参数：{}", params);

        // 生成缓存键（基于参数）
        String startDate = params.getOrDefault("startDate", "").toString();
        String endDate = params.getOrDefault("endDate", "").toString();
        String cacheKey = "stats:dept:range:" + startDate + ":" + endDate;

        // 尝试从缓存获取
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<StatisticsDTO.DepartmentStats> list = (List<StatisticsDTO.DepartmentStats>) cached;
                log.info("从缓存获取科室统计数据");
                return list;
            } catch (ClassCastException ignored) {}
        }

        // 从数据库查询
        List<StatisticsDTO.DepartmentStats> data = statisticsMapper.getDepartmentStats(params);

        // 存入缓存（30分钟）
        redisUtil.set(cacheKey, data, 30, TimeUnit.MINUTES);

        return data;
    }
    
    @Override
    public List<StatisticsDTO.DoctorStats> getDoctorStats(Map<String, Object> params) {
        log.info("获取医生统计排行，参数：{}", params);

        // 生成缓存键（基于参数）
        String startDate = params.getOrDefault("startDate", "").toString();
        String endDate = params.getOrDefault("endDate", "").toString();
        String cacheKey = "stats:doctor:range:" + startDate + ":" + endDate;

        // 尝试从缓存获取
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<StatisticsDTO.DoctorStats> list = (List<StatisticsDTO.DoctorStats>) cached;
                log.info("从缓存获取医生统计数据");
                return list;
            } catch (ClassCastException ignored) {}
        }

        // 从数据库查询
        List<StatisticsDTO.DoctorStats> data = statisticsMapper.getDoctorStats(params);

        // 存入缓存（30分钟）
        redisUtil.set(cacheKey, data, 30, TimeUnit.MINUTES);

        return data;
    }
    
    @Override
    public List<StatisticsDTO.TrendData> getAppointmentTrend(Map<String, Object> params) {
        log.info("获取预约趋势数据，参数：{}", params);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30); // 默认获取30天数据

        // 如果参数中有日期范围，使用参数中的
        if (params.containsKey("startDate")) {
            try {
                startDate = LocalDate.parse(params.get("startDate").toString(), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                log.warn("解析开始日期失败，使用默认值：{}", e.getMessage());
            }
        }

        if (params.containsKey("endDate")) {
            try {
                endDate = LocalDate.parse(params.get("endDate").toString(), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                log.warn("解析结束日期失败，使用默认值：{}", e.getMessage());
            }
        }

        // 生成缓存键（基于日期范围）
        String cacheKey = "stats:trend:" + startDate + ":" + endDate;

        // 尝试从缓存获取
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<StatisticsDTO.TrendData> list = (List<StatisticsDTO.TrendData>) cached;
                log.info("从缓存获取预约趋势数据");
                return list;
            } catch (ClassCastException ignored) {}
        }

        // 从数据库查询
        List<StatisticsDTO.TrendData> data = statisticsMapper.getAppointmentTrend(startDate, endDate);

        // 存入缓存（30分钟）
        redisUtil.set(cacheKey, data, 30, TimeUnit.MINUTES);

        return data;
    }
    
    @Override
    public List<StatisticsDTO.RecentAppointment> getRecentAppointments() {
        log.info("获取最近预约列表");
        String cacheKey = "stats:recentAppointments:10";
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<StatisticsDTO.RecentAppointment> list = (List<StatisticsDTO.RecentAppointment>) cached;
                return list;
            } catch (ClassCastException ignored) {}
        }
        List<StatisticsDTO.RecentAppointment> data = statisticsMapper.getRecentAppointments(10); // 默认获取10条
        // 最近预约列表，缓存 5 分钟
        redisUtil.set(cacheKey, data, 5, TimeUnit.MINUTES);
        return data;
    }
    
    @Override
    public List<StatisticsDTO.RecentAppointment> getPatientRecentAppointments(Long patientId) {
        log.info("获取患者最近预约，患者ID：{}", patientId);
        String cacheKey = "hospital:patient:stats:appointments:recent:patient:" + patientId;
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<StatisticsDTO.RecentAppointment> list = (List<StatisticsDTO.RecentAppointment>) cached;
                return list;
            } catch (ClassCastException ignored) {}
        }
        List<StatisticsDTO.RecentAppointment> data = statisticsMapper.getPatientRecentAppointments(patientId, 5); // 默认获取5条
        // 患者最近预约，缓存 5 分钟
        redisUtil.set(cacheKey, data, 5, TimeUnit.MINUTES);
        return data;
    }
}
