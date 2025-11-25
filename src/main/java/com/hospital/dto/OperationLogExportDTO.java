package com.hospital.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

/**
 * 操作日志导出DTO
 */
@Data
@HeadRowHeight(20)
@ContentRowHeight(18)
public class OperationLogExportDTO {

    @ExcelProperty(value = "操作用户", index = 0)
    @ColumnWidth(15)
    private String username;

    @ExcelProperty(value = "操作模块", index = 1)
    @ColumnWidth(15)
    private String operationModule;

    @ExcelProperty(value = "操作类型", index = 2)
    @ColumnWidth(12)
    private String operationType;

    @ExcelProperty(value = "操作描述", index = 3)
    @ColumnWidth(30)
    private String operationDesc;

    @ExcelProperty(value = "请求方法", index = 4)
    @ColumnWidth(12)
    private String requestMethod;

    @ExcelProperty(value = "请求URL", index = 5)
    @ColumnWidth(40)
    private String requestUrl;

    @ExcelProperty(value = "IP地址", index = 6)
    @ColumnWidth(18)
    private String ipAddress;

    @ExcelProperty(value = "耗时(ms)", index = 7)
    @ColumnWidth(20)
    private Integer executionTime;

    @ExcelProperty(value = "状态", index = 8)
    @ColumnWidth(10)
    private String status;

    @ExcelProperty(value = "操作时间", index = 9)
    @ColumnWidth(20)
    private String createdAt;
}

