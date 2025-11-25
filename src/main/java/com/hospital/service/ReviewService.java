package com.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hospital.entity.Review;
import java.util.Map;

/**
 * 评价管理服务接口
 */
public interface ReviewService extends IService<Review> {
    
    /**
     * 获取医生评价列表
     * @param doctorId 医生ID
     * @param params 查询参数
     * @return 分页结果
     */
    IPage<Review> getDoctorReviews(Long doctorId, Map<String, Object> params);
    
    /**
     * 获取评价详情
     * @param id 评价ID
     * @return 评价详情
     */
    Review getReviewById(Long id);
    
    /**
     * 创建评价
     * @param review 评价信息
     * @return 是否成功
     */
    boolean createReview(Review review);
    
    /**
     * 回复评价
     * @param reviewId 评价ID
     * @param reply 回复内容
     * @return 是否成功
     */
    boolean replyReview(Long reviewId, String reply);
    
    /**
     * 删除评价
     * @param id 评价ID
     * @return 是否成功
     */
    boolean deleteReview(Long id);
    
    /**
     * 获取所有评价列表（管理员端）
     * @param params 查询参数
     * @return 分页结果
     */
    IPage<Review> getAllReviews(Map<String, Object> params);
}
