package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Map;

/**
 * 评价Mapper接口
 */
@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
    
    /**
     * 分页查询医生评价
     * @param page 分页对象
     * @param params 查询参数
     */
    IPage<Review> selectDoctorReviews(Page<Review> page, @Param("params") Map<String, Object> params);
    
    /**
     * 分页查询所有评价（管理员）
     * @param page 分页对象
     * @param params 查询参数
     */
    IPage<Review> selectAllReviews(Page<Review> page, @Param("params") Map<String, Object> params);
    
    /**
     * 更新医生评分统计
     * @param doctorId 医生ID
     */
    void updateDoctorRating(@Param("doctorId") Long doctorId);
}
