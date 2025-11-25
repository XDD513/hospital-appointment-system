package com.hospital.dto.request;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 批量创建排班请求
 */
@Data
public class BatchCreateScheduleRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 多个医生ID */
    @NotEmpty(message = "医生ID列表不能为空")
    private List<Long> doctorIds;

    /** 开始日期 */
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    /** 结束日期 */
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    /** 排班时段（MORNING/AFTERNOON/EVENING） */
    @NotEmpty(message = "至少选择一个时段")
    private List<String> timeSlots;

    /** 每个排班的总号源数 */
    @NotNull(message = "号源数量不能为空")
    private Integer totalQuota;

    /** 排班状态（可选，默认AVAILABLE） */
    private String status;

    /** 备注（可选） */
    private String note;
}