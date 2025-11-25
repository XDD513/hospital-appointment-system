package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.entity.HealthPlanRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 健康计划记录Mapper接口
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Mapper
public interface HealthPlanRecordMapper extends BaseMapper<HealthPlanRecord> {

    /**
     * 分页查询用户的健康计划
     *
     * @param page 分页对象
     * @param userId 用户ID
     * @param status 状态（可选）
     * @return 健康计划列表
     */
    @Select("<script>" +
            "SELECT * FROM health_plan_record WHERE user_id = #{userId} " +
            "<if test='status != null'>" +
            "AND status = #{status} " +
            "</if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    IPage<HealthPlanRecord> selectByUserId(Page<HealthPlanRecord> page,
                                            @Param("userId") Long userId,
                                            @Param("status") Integer status);

    /**
     * 查询用户进行中的计划
     *
     * @param userId 用户ID
     * @return 健康计划列表
     */
    @Select("SELECT * FROM health_plan_record WHERE user_id = #{userId} AND status = 1 " +
            "ORDER BY created_at DESC")
    List<HealthPlanRecord> selectActivePlans(@Param("userId") Long userId);

    /**
     * 更新计划完成次数
     *
     * @param id 计划ID
     */
    @Update("UPDATE health_plan_record SET completed_count = completed_count + 1, " +
            "completion_rate = (completed_count + 1) * 100.0 / target_count " +
            "WHERE id = #{id}")
    void incrementCompletedCount(@Param("id") Long id);
}

