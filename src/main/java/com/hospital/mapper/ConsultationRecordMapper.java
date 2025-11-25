package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.entity.ConsultationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Map;

/**
 * 接诊记录Mapper接口
 */
@Mapper
public interface ConsultationRecordMapper extends BaseMapper<ConsultationRecord> {
    
    /**
     * 分页查询医生接诊记录
     * @param page 分页对象
     * @param params 查询参数
     */
    IPage<ConsultationRecord> selectDoctorRecords(Page<ConsultationRecord> page, @Param("params") Map<String, Object> params);
    
    /**
     * 查询医生接诊记录详情
     * @param id 记录ID
     * @param doctorId 医生ID
     */
    ConsultationRecord selectDoctorRecordById(@Param("id") Long id, @Param("doctorId") Long doctorId);
    
    /**
     * 统计医生接诊记录数量
     * @param doctorId 医生ID
     * @param params 查询参数
     */
    Long countDoctorRecords(@Param("doctorId") Long doctorId, @Param("params") Map<String, Object> params);

    /**
     * 查询接诊记录详情（包含关联信息）
     * @param id 记录ID
     * @return 接诊记录详情
     */
    ConsultationRecord selectRecordDetailById(@Param("id") Long id);
}
