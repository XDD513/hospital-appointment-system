package com.hospital.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 接诊记录导出DTO
 */
@Data
@HeadRowHeight(20)
@ContentRowHeight(18)
public class ConsultationRecordExportDTO {

    @ExcelProperty(value = "患者姓名", index = 0)
    @ColumnWidth(12)
    private String patientName;

    @ExcelProperty(value = "患者性别", index = 1)
    @ColumnWidth(12)
    private String gender;

    @ExcelProperty(value = "患者年龄", index = 2)
    @ColumnWidth(12)
    private String age;

    @ExcelProperty(value = "医生姓名", index = 3)
    @ColumnWidth(12)
    private String doctorName;

    @ExcelProperty(value = "科室", index = 4)
    @ColumnWidth(12)
    private String categoryName;

    @ExcelProperty(value = "主诉", index = 5)
    @ColumnWidth(30)
    private String chiefComplaint;

    @ExcelProperty(value = "现病史", index = 6)
    @ColumnWidth(40)
    private String presentIllness;

    @ExcelProperty(value = "既往史", index = 7)
    @ColumnWidth(30)
    private String pastHistory;

    @ExcelProperty(value = "体格检查", index = 8)
    @ColumnWidth(30)
    private String physicalExamination;

    @ExcelProperty(value = "辅助检查", index = 9)
    @ColumnWidth(30)
    private String auxiliaryExamination;

    @ExcelProperty(value = "诊断", index = 10)
    @ColumnWidth(30)
    private String diagnosis;

    @ExcelProperty(value = "治疗方案", index = 11)
    @ColumnWidth(40)
    private String treatmentPlan;

    @ExcelProperty(value = "随访建议", index = 12)
    @ColumnWidth(30)
    private String followUpAdvice;

    @ExcelProperty(value = "诊疗费", index = 13)
    @ColumnWidth(10)
    private String consultationFee;

    @ExcelProperty(value = "接诊时长(分钟)", index = 14)
    @ColumnWidth(25)
    private String durationMinutes;

    @ExcelProperty(value = "状态", index = 15)
    @ColumnWidth(10)
    private String status;

    @ExcelProperty(value = "接诊时间", index = 16)
    @ColumnWidth(20)
    private String consultationDateTime;

    @ExcelProperty(value = "完成就诊时间", index = 17)
    @ColumnWidth(20)
    private String completedTime;
}

