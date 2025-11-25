package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.entity.UserNotification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户通知 Mapper。
 */
@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {
}

