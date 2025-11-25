package com.hospital.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import lombok.Data;

/**
 * 患者预约记录导出 DTO
 */
@Data
@HeadRowHeight(20)
@ContentRowHeight(18)
public class AppointmentExportDTO {

    @ExcelProperty(value = "预约编号", index = 0)
    @ColumnWidth(20)
    private String appointmentNo;

    @ExcelProperty(value = "科室/分类", index = 1)
    @ColumnWidth(20)
    private String departmentName;

    @ExcelProperty(value = "医生姓名", index = 2)
    @ColumnWidth(16)
    private String doctorName;

    @ExcelProperty(value = "就诊日期", index = 3)
    @ColumnWidth(18)
    private String appointmentDate;

    @ExcelProperty(value = "就诊时段", index = 4)
    @ColumnWidth(18)
    private String timeSlot;

    @ExcelProperty(value = "排队号", index = 5)
    @ColumnWidth(12)
    private String queueNumber;

    @ExcelProperty(value = "预约状态", index = 6)
    @ColumnWidth(14)
    private String status;

    @ExcelProperty(value = "挂号费(元)", index = 7)
    @ColumnWidth(14)
    private String consultationFee;

    @ExcelProperty(value = "创建时间", index = 8)
    @ColumnWidth(22)
    private String createdAt;

    @ExcelProperty(value = "更新时间", index = 9)
    @ColumnWidth(22)
    private String updatedAt;
}


