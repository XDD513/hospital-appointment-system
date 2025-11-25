package com.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.common.result.Result;
import com.hospital.entity.ConsultationRecord;
import com.hospital.service.ConsultationRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 接诊记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/consultation")
public class ConsultationController {

    @Autowired
    private ConsultationRecordService consultationRecordService;

    /**
     * 获取医生接诊记录列表
     */
    @GetMapping("/doctor/{doctorId}/records")
    public Result<IPage<ConsultationRecord>> getDoctorRecords(@PathVariable Long doctorId,
                                                             @RequestParam Map<String, Object> params) {
        params.put("doctorId", doctorId);
        IPage<ConsultationRecord> records = consultationRecordService.getDoctorRecords(params);
        return Result.success(records);
    }

    /**
     * 获取接诊记录详情
     */
    @GetMapping("/record/{id}")
    public Result<ConsultationRecord> getConsultationRecordById(@PathVariable Long id) {
        ConsultationRecord record = consultationRecordService.getConsultationRecordDetail(id);
        return Result.success(record);
    }

    /**
     * 创建接诊记录
     */
    @PostMapping("/record/create")
    public Result<Boolean> createConsultationRecord(@RequestBody ConsultationRecord consultationRecord) {
        boolean result = consultationRecordService.createConsultationRecord(consultationRecord);
        return Result.success(result);
    }

    /**
     * 更新接诊记录
     */
    @PutMapping("/record/update")
    public Result<Boolean> updateConsultationRecord(@RequestBody ConsultationRecord consultationRecord) {
        boolean result = consultationRecordService.updateConsultationRecord(consultationRecord);
        return Result.success(result);
    }

    /**
     * 开始接诊
     */
    @PostMapping("/start/{appointmentId}")
    public Result<Boolean> startConsultation(@PathVariable Long appointmentId) {
        log.info("开始接诊，预约ID：{}", appointmentId);
        boolean result = consultationRecordService.startConsultation(appointmentId);
        return Result.success(result);
    }

    /**
     * 完成接诊
     */
    @PostMapping("/complete/{appointmentId}")
    public Result<Boolean> completeConsultation(@PathVariable Long appointmentId,
                                               @RequestBody ConsultationRecord consultationRecord) {
        boolean result = consultationRecordService.completeConsultation(appointmentId, consultationRecord);
        return Result.success(result);
    }

    /**
     * 导出接诊记录
     */
    @GetMapping("/doctor/{doctorId}/export")
    public void exportConsultationRecords(@PathVariable Long doctorId,
                                         @RequestParam Map<String, Object> params,
                                         HttpServletResponse response) throws IOException {
        byte[] data = consultationRecordService.exportConsultationRecords(doctorId, params);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=consultation_records.xlsx");
        response.getOutputStream().write(data);
    }
}
