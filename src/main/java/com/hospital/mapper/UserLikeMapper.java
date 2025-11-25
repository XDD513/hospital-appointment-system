package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.entity.UserLike;
import org.apache.ibatis.annotations.*;

/**
 * 用户点赞Mapper接口
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Mapper
public interface UserLikeMapper extends BaseMapper<UserLike> {

    /**
     * 查询用户是否已点赞
     *
     * @param userId 用户ID
     * @param targetId 目标ID
     * @param targetType 目标类型
     * @return 点赞记录
     */
    @Select("SELECT * FROM user_like WHERE user_id = #{userId} " +
            "AND target_id = #{targetId} AND target_type = #{targetType}")
    UserLike selectByUserAndTarget(@Param("userId") Long userId,
                                    @Param("targetId") Long targetId,
                                    @Param("targetType") String targetType);

    /**
     * 删除点赞记录
     *
     * @param userId 用户ID
     * @param targetId 目标ID
     * @param targetType 目标类型
     */
    @Delete("DELETE FROM user_like WHERE user_id = #{userId} " +
            "AND target_id = #{targetId} AND target_type = #{targetType}")
    void deleteByUserAndTarget(@Param("userId") Long userId,
                                @Param("targetId") Long targetId,
                                @Param("targetType") String targetType);
}

