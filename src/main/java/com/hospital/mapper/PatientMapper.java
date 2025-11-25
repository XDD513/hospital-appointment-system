package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

/**
 * 患者Mapper接口
 */
@Mapper
public interface PatientMapper extends BaseMapper<User> {
    
    /**
     * 获取今日患者列表（医生端使用）
     * @param doctorId 医生ID
     */
    List<Map<String, Object>> selectTodayPatients(@Param("doctorId") Long doctorId);
    
    /**
     * 获取历史患者列表（医生端使用）
     * @param page 分页对象
     * @param params 查询参数
     */
    IPage<Map<String, Object>> selectHistoryPatients(Page<Map<String, Object>> page, @Param("params") Map<String, Object> params);
    
    /**
     * 获取待接诊患者列表
     * @param doctorId 医生ID
     */
    List<Map<String, Object>> selectTodayPendingPatients(@Param("doctorId") Long doctorId);
    
    /**
     * 获取已接诊患者列表
     * @param doctorId 医生ID
     */
    List<Map<String, Object>> selectTodayCompletedPatients(@Param("doctorId") Long doctorId);
    
    /**
     * 获取所有患者列表（管理员端使用）
     * @param page 分页对象
     * @param params 查询参数
     */
    IPage<User> selectPatientList(Page<User> page, @Param("params") Map<String, Object> params);
}
