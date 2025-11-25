package com.hospital.dto.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 患者预约统计响应DTO
 *
 * @author Hospital Team
 */
@Data
public class PatientAppointmentStatsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 累计预约数
     */
    private Integer totalAppointments = 0;

    /**
     * 待就诊数（CONFIRMED状态）
     */
    private Integer pendingAppointments = 0;
}

