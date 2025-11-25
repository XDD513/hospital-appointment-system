package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {

    @Select("SELECT * FROM appointment WHERE user_id = #{patientId} ORDER BY created_at DESC")
    List<Appointment> selectByPatientId(Long patientId);

    @Select("SELECT * FROM appointment WHERE doctor_id = #{doctorId} ORDER BY created_at DESC")
    List<Appointment> selectByDoctorId(Long doctorId);
    
    /**
     * 统计指定医生、日期、时段的预约数量（用于生成排队号）
     * 注意：SQL中的状态值对应 AppointmentStatus.CONFIRMED, IN_PROGRESS, COMPLETED
     */
    @Select("SELECT COUNT(*) FROM appointment WHERE doctor_id = #{doctorId} " +
            "AND appointment_date = #{appointmentDate} AND time_slot = #{timeSlot} " +
            "AND status IN ('CONFIRMED', 'IN_PROGRESS', 'COMPLETED')")
    Integer countByDoctorDateTimeSlot(@Param("doctorId") Long doctorId, 
                                      @Param("appointmentDate") LocalDate appointmentDate, 
                                      @Param("timeSlot") String timeSlot);

    /**
     * 显式更新预约状态（同时刷新更新时间）
     */
    @Update("UPDATE appointment SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatusById(@Param("id") Long id, @Param("status") String status);

    /**
     * 统计患者预约数量
     */
    @Select("SELECT COUNT(*) FROM appointment WHERE user_id = #{patientId}")
    Integer countByPatientId(@Param("patientId") Long patientId);

    /**
     * 统计患者待就诊数量（CONFIRMED状态）
     */
    @Select("SELECT COUNT(*) FROM appointment WHERE user_id = #{patientId} AND status = 'CONFIRMED'")
    Integer countPendingByPatientId(@Param("patientId") Long patientId);
}

