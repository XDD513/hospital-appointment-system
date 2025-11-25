package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.common.constant.CacheConstants;
import com.hospital.common.constant.SystemConstants;
import com.hospital.config.AvatarConfig;
import com.hospital.entity.Review;
import com.hospital.mapper.ReviewMapper;
import com.hospital.service.OssService;
import com.hospital.service.ReviewService;
import com.hospital.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 评价管理服务实现类
 */
@Slf4j
@Service
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {
    
    @Autowired
    private ReviewMapper reviewMapper;
    
    @Autowired
    private RedisUtil redisUtil;
    
    @Autowired
    private OssService ossService;
    
    @Autowired
    private AvatarConfig avatarConfig;
    
    @Override
    public IPage<Review> getDoctorReviews(Long doctorId, Map<String, Object> params) {
        log.info("获取医生评价列表，医生ID：{}，参数：{}", doctorId, params);
        
        // 安全地解析分页参数
        Integer page = 1;
        Integer pageSize = SystemConstants.DEFAULT_PAGE_SIZE;
        
        try {
            if (params.get("page") != null) {
                page = Integer.parseInt(params.get("page").toString());
            }
        } catch (NumberFormatException e) {
            log.warn("无效的页码参数：{}", params.get("page"));
        }
        
        try {
            if (params.get("pageSize") != null) {
                pageSize = Integer.parseInt(params.get("pageSize").toString());
            }
        } catch (NumberFormatException e) {
            log.warn("无效的页面大小参数：{}", params.get("pageSize"));
        }
        
        Page<Review> pageObject = new Page<>(page, pageSize);
        params.put("doctorId", doctorId);
        
        // 热门区间：仅缓存前3页，使用参数哈希简化层级
        Map<String, Object> filterParams = new java.util.HashMap<>();
        if (params.containsKey("status") && !"ALL".equals(String.valueOf(params.get("status")))) {
            filterParams.put("status", params.get("status"));
        }
        if (params.containsKey("sort") && !"default".equals(String.valueOf(params.get("sort")))) {
            filterParams.put("sort", params.get("sort"));
        }
        if (params.containsKey("rating") && params.get("rating") != null && !"".equals(String.valueOf(params.get("rating")))) {
            filterParams.put("rating", params.get("rating"));
        }
        String cacheKey = redisUtil.buildCacheKey("hospital:common:review:v2:list:doctor:" + doctorId, page, pageSize, filterParams);

        if (page <= 3) {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                @SuppressWarnings("unchecked")
                IPage<Review> cachedPage = (IPage<Review>) cached;
                return cachedPage;
            }
        }

        IPage<Review> result = reviewMapper.selectDoctorReviews(pageObject, params);
        
        // 处理患者头像URL
        if (result != null && result.getRecords() != null) {
            for (Review review : result.getRecords()) {
                if (review != null && review.getPatientId() != null) {
                    review.setPatientAvatar(resolveAvatarUrl(review.getPatientAvatar(), review.getPatientId(), "patient"));
                }
            }
        }
        
        if (page <= 3) {
            redisUtil.set(cacheKey, result, CacheConstants.DOCTOR_REVIEWS_HOT_TTL_SECONDS, TimeUnit.SECONDS);
        }
        return result;
    }
    
    @Override
    public Review getReviewById(Long id) {
        log.info("获取评价详情，评价ID：{}", id);
        String cacheKey = String.format("hospital:common:review:v2:detail:id:%d", id);
        Object cached = redisUtil.get(cacheKey);
        if (cached != null) {
            return (Review) cached;
        }

        Review review = getById(id);
        if (review != null) {
            redisUtil.set(cacheKey, review, CacheConstants.REVIEW_DETAIL_TTL_SECONDS, TimeUnit.SECONDS);
        }
        return review;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createReview(Review review) {
        log.info("创建评价，预约ID：{}", review.getAppointmentId());
        
        // 设置评价状态为已发布（患者提交后直接发布）
        if (review.getStatus() == null || review.getStatus().isEmpty()) {
            review.setStatus("PUBLISHED");
        }
        
        boolean result = save(review);
        
        if (result) {
            // 更新医生评分统计
            reviewMapper.updateDoctorRating(review.getDoctorId());

            // 失效医生评价热门分页缓存（该医生）与管理员端列表缓存
            redisUtil.deleteByPattern(String.format("hospital:common:review:v2:list:doctor:%d:*", review.getDoctorId()));
            redisUtil.deleteByPattern("hospital:admin:review:list:*");
        }
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean replyReview(Long reviewId, String reply) {
        log.info("回复评价，评价ID：{}，回复内容：{}", reviewId, reply);
        
        // 先查询以获取医生ID，用于失效对应缓存
        Review existed = getById(reviewId);

        Review review = new Review();
        review.setId(reviewId);
        review.setDoctorReply(reply);
        review.setDoctorReplyTime(LocalDateTime.now());
        
        boolean updated = updateById(review);

        if (updated) {
            // 失效评价详情缓存
            redisUtil.delete(String.format("hospital:common:review:v2:detail:id:%d", reviewId));

            // 失效医生评价热门分页缓存（该医生）与管理员端列表缓存
            if (existed != null && existed.getDoctorId() != null) {
                redisUtil.deleteByPattern(String.format("hospital:common:review:v2:list:doctor:%d:*", existed.getDoctorId()));
            }
            redisUtil.deleteByPattern("hospital:admin:review:list:*");
        }
        
        return updated;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteReview(Long id) {
        log.info("删除评价，评价ID：{}", id);
        
        Review review = getById(id);
        boolean result = removeById(id);
        
        if (result && review != null) {
            // 更新医生评分统计
            reviewMapper.updateDoctorRating(review.getDoctorId());

            // 失效评价详情缓存
            redisUtil.delete(String.format("hospital:common:review:v2:detail:id:%d", id));

            // 失效医生评价热门分页缓存（该医生）与管理员端列表缓存
            redisUtil.deleteByPattern(String.format("hospital:common:review:v2:list:doctor:%d:*", review.getDoctorId()));
            redisUtil.deleteByPattern("hospital:admin:review:list:*");
        }
        
        return result;
    }
    
    @Override
    public IPage<Review> getAllReviews(Map<String, Object> params) {
        log.info("获取所有评价列表，参数：{}", params);
        
        Integer page = (Integer) params.get("page");
        Integer pageSize = (Integer) params.get("pageSize");

        int p = page != null ? page : 1;
        int ps = pageSize != null ? pageSize : SystemConstants.DEFAULT_PAGE_SIZE;

        Page<Review> pageObject = new Page<>(p, ps);

        // 管理员端列表：仅缓存前2页，使用参数哈希简化层级
        Map<String, Object> filterParams = new java.util.HashMap<>();
        if (params.containsKey("status") && !"ALL".equals(String.valueOf(params.get("status")))) {
            filterParams.put("status", params.get("status"));
        }
        if (params.containsKey("sort") && !"default".equals(String.valueOf(params.get("sort")))) {
            filterParams.put("sort", params.get("sort"));
        }
        String cacheKey = redisUtil.buildCacheKey("hospital:admin:review:list", p, ps, filterParams);

        if (p <= 2) {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                @SuppressWarnings("unchecked")
                IPage<Review> cachedPage = (IPage<Review>) cached;
                return cachedPage;
            }
        }

        IPage<Review> result = reviewMapper.selectAllReviews(pageObject, params);
        
        // 处理患者头像URL
        if (result != null && result.getRecords() != null) {
            for (Review review : result.getRecords()) {
                if (review != null && review.getPatientId() != null) {
                    review.setPatientAvatar(resolveAvatarUrl(review.getPatientAvatar(), review.getPatientId(), "patient"));
                }
            }
        }
        
        if (p <= 2) {
            redisUtil.set(cacheKey, result, CacheConstants.ADMIN_REVIEWS_TTL_SECONDS, TimeUnit.SECONDS);
        }
        return result;
    }
    
    /**
     * 清理头像URL（移除查询参数等）
     */
    private String sanitizeAvatarUrl(String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl)) {
            return null;
        }
        String sanitized = avatarUrl.trim();
        boolean containsSignatureParam = sanitized.contains("Signature=") || sanitized.contains("OSSAccessKeyId=")
                || sanitized.contains("Expires=");
        if (containsSignatureParam) {
            int questionIndex = sanitized.indexOf('?');
            if (questionIndex > 0) {
                sanitized = sanitized.substring(0, questionIndex);
            }
        }
        return sanitized;
    }
    
    /**
     * 生成可直接访问的头像URL
     */
    private String resolveAvatarUrl(String rawAvatar, Long entityId, String entityType) {
        String sanitizedAvatar = sanitizeAvatarUrl(rawAvatar);
        if (StringUtils.hasText(sanitizedAvatar)) {
            // 尝试从缓存获取签名URL
            String cacheKey = CacheConstants.CACHE_OSS_SIGNED_URL_PREFIX + sanitizedAvatar;
            Object cached = redisUtil.get(cacheKey);
            if (cached != null && cached instanceof String) {
                return (String) cached;
            }
            
            try {
                String signedUrl = ossService.generatePresignedUrl(sanitizedAvatar, avatarConfig.getTtlMinutes());
                if (StringUtils.hasText(signedUrl)) {
                    // 存入缓存
                    redisUtil.set(cacheKey, signedUrl, CacheConstants.CACHE_OSS_SIGNED_URL_TTL_SECONDS, TimeUnit.SECONDS);
                    return signedUrl;
                }
            } catch (Exception e) {
                log.warn("生成头像签名URL失败: avatar={}, error={}", sanitizedAvatar, e.getMessage());
                return sanitizedAvatar;
            }
        }
        // 使用默认头像
        if ("patient".equals(entityType) && entityId != null) {
            return avatarConfig.getDefaultPatient() + "&seed=" + entityId;
        }
        if ("doctor".equals(entityType) && entityId != null) {
            return avatarConfig.getDefaultDoctor() + "&seed=" + entityId;
        }
        if ("admin".equals(entityType) && entityId != null) {
            return avatarConfig.getDefaultAdmin() + "&seed=" + entityId;
        }
        return avatarConfig.getDefaultPatient();
    }
}
