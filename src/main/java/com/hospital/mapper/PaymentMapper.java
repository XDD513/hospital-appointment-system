package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 支付记录Mapper接口
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

    /**
     * 根据预约ID查询支付记录
     *
     * @param appointmentId 预约ID
     * @return 支付记录列表
     */
    @Select("SELECT * FROM payment WHERE appointment_id = #{appointmentId}")
    List<Payment> selectByAppointmentId(Long appointmentId);

    /**
     * 根据支付单号查询支付记录
     *
     * @param paymentNo 支付单号
     * @return 支付记录
     */
    @Select("SELECT * FROM payment WHERE id = #{paymentNo}")
    Payment selectByPaymentNo(String paymentNo);

    /**
     * 根据用户ID查询支付记录
     *
     * @param userId 用户ID
     * @return 支付记录列表
     */
    @Select("SELECT * FROM payment WHERE patient_id = #{userId} ORDER BY created_at DESC")
    List<Payment> selectByUserId(Long userId);
}
