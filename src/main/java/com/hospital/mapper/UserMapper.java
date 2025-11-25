package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
