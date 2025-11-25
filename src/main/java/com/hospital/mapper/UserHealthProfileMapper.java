package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.entity.UserHealthProfile;
import org.apache.ibatis.annotations.*;

/**
 * 用户健康档案Mapper接口
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Mapper
public interface UserHealthProfileMapper extends BaseMapper<UserHealthProfile> {

    /**
     * 根据用户ID查询健康档案
     *
     * @param userId 用户ID
     * @return 健康档案
     */
    @Select("SELECT * FROM user_health_profile WHERE user_id = #{userId}")
    UserHealthProfile selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID删除健康档案
     *
     * @param userId 用户ID
     */
    @Delete("DELETE FROM user_health_profile WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);
}

