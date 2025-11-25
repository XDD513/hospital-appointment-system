package com.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hospital.entity.ConsultationRecord;
import java.util.Map;

/**
 * 接诊记录服务接口
 */
public interface ConsultationRecordService extends IService<ConsultationRecord> {
    
    /**
     * 分页查询医生接诊记录
     * @param params 查询参数
     * @return 分页结果
     */
    IPage<ConsultationRecord> getDoctorRecords(Map<String, Object> params);
    
    /**
     * 创建接诊记录
     * @param consultationRecord 接诊记录
     * @return 是否成功
     */
    boolean createConsultationRecord(ConsultationRecord consultationRecord);
    
    /**
     * 更新接诊记录
     * @param consultationRecord 接诊记录
     * @return 是否成功
     */
    boolean updateConsultationRecord(ConsultationRecord consultationRecord);

    /**
     * 开始接诊
     * @param appointmentId 预约ID
     * @return 是否成功
     */
    boolean startConsultation(Long appointmentId);

    /**
     * 完成接诊
     * @param appointmentId 预约ID
     * @param consultationRecord 接诊记录
     * @return 是否成功
     */
    boolean completeConsultation(Long appointmentId, ConsultationRecord consultationRecord);
    
    /**
     * 导出接诊记录
     * @param doctorId 医生ID
     * @param params 查询参数
     * @return 导出数据
     */
    byte[] exportConsultationRecords(Long doctorId, Map<String, Object> params);

    /**
     * 获取接诊记录详情（包含关联信息）
     * @param id 记录ID
     * @return 接诊记录详情
     */
    ConsultationRecord getConsultationRecordDetail(Long id);
}
