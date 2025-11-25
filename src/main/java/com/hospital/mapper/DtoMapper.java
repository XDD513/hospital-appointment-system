package com.hospital.mapper;

import com.hospital.dto.StatisticsDTO;
import com.hospital.dto.response.LoginResponse;
import com.hospital.dto.response.UserInfoResponse;
import com.hospital.entity.Appointment;
import com.hospital.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.AfterMapping;
import org.mapstruct.factory.Mappers;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * DTO映射器接口
 * 使用MapStruct进行对象映射，替代手写BeanUtils.copyProperties
 *
 * @author Hospital Team
 * @since 2025-11-25
 */
@Mapper(componentModel = "spring")
public interface DtoMapper {

    DtoMapper INSTANCE = Mappers.getMapper(DtoMapper.class);

    /**
     * User实体转UserInfoResponse
     * password字段在User实体中存在，但UserInfoResponse中不存在，MapStruct会自动忽略
     */
    @Mapping(target = "doctorId", ignore = true) // doctorId需要单独设置
    UserInfoResponse toUserInfoResponse(User user);

    /**
     * User实体转LoginResponse
     * password字段在User实体中存在，但LoginResponse中不存在，MapStruct会自动忽略
     */
    @Mapping(target = "token", ignore = true) // token需要单独设置
    @Mapping(target = "doctorId", ignore = true) // doctorId需要单独设置
    LoginResponse toLoginResponse(User user);

    /**
     * User列表转UserInfoResponse列表
     */
    List<UserInfoResponse> toUserInfoResponseList(List<User> users);

    /**
     * Appointment实体转StatisticsDTO.RecentAppointment
     * 注意：deptName 优先使用 Appointment.deptName，如果为空则使用 categoryName（兼容字段）
     */
    @Mapping(source = "patientName", target = "patientName")
    @Mapping(source = "doctorName", target = "doctorName")
    @Mapping(source = "appointmentDate", target = "date", dateFormat = "yyyy-MM-dd")
    @Mapping(source = "status", target = "status")
    @Mapping(target = "deptName", ignore = true) // 在 @AfterMapping 中处理
    StatisticsDTO.RecentAppointment toRecentAppointment(Appointment appointment);

    /**
     * 处理 deptName 的兼容映射：优先使用 deptName，如果为空则使用 categoryName
     */
    @AfterMapping
    default void mapDeptName(@MappingTarget StatisticsDTO.RecentAppointment target, Appointment source) {
        if (StringUtils.hasText(source.getDeptName())) {
            target.setDeptName(source.getDeptName());
        } else if (StringUtils.hasText(source.getCategoryName())) {
            target.setDeptName(source.getCategoryName());
        }
    }

    /**
     * Appointment列表转RecentAppointment列表
     */
    List<StatisticsDTO.RecentAppointment> toRecentAppointmentList(List<Appointment> appointments);
}

