package com.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.common.result.Result;
import com.hospital.entity.Review;
import com.hospital.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 评价管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    /**
     * 获取医生评价列表
     */
    @GetMapping("/doctor/{doctorId}")
    public Result<IPage<Review>> getDoctorReviews(@PathVariable Long doctorId, @RequestParam Map<String, Object> params) {
        IPage<Review> reviews = reviewService.getDoctorReviews(doctorId, params);
        return Result.success(reviews);
    }

    /**
     * 获取评价详情
     */
    @GetMapping("/{id}")
    public Result<Review> getReviewById(@PathVariable Long id) {
        Review review = reviewService.getReviewById(id);
        return Result.success(review);
    }

    /**
     * 创建评价
     */
    @PostMapping("/create")
    public Result<Boolean> createReview(@RequestBody Review review) {
        boolean result = reviewService.createReview(review);
        return Result.success(result);
    }

    /**
     * 回复评价
     */
    @PostMapping("/{reviewId}/reply")
    public Result<Boolean> replyReview(@PathVariable Long reviewId, @RequestBody Map<String, String> request) {
        String reply = request.get("reply");
        boolean result = reviewService.replyReview(reviewId, reply);
        return Result.success(result);
    }

    /**
     * 删除评价
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteReview(@PathVariable Long id) {
        boolean result = reviewService.deleteReview(id);
        return Result.success(result);
    }

    /**
     * 获取所有评价列表（管理员端）
     */
    @GetMapping("/list")
    public Result<IPage<Review>> getAllReviews(@RequestParam Map<String, Object> params) {
        IPage<Review> reviews = reviewService.getAllReviews(params);
        return Result.success(reviews);
    }
}
