package com.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.common.result.Result;
import com.hospital.entity.HealthArticle;

import java.util.List;

/**
 * 养生文章服务接口
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
public interface HealthArticleService {

    /**
     * 分页查询文章列表
     *
     * @param category 分类（可选）
     * @param constitutionType 体质类型（可选）
     * @param tags 标签（可选，支持多个标签，逗号分隔）
     * @param isFeatured 是否精选（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 文章列表
     */
    Result<IPage<HealthArticle>> getArticleList(String category, String constitutionType, String tags, Integer isFeatured, Integer pageNum, Integer pageSize);

    /**
     * 获取文章详情
     *
     * @param id 文章ID
     * @return 文章详情
     */
    Result<HealthArticle> getArticleDetail(Long id);

    /**
     * 搜索文章
     *
     * @param keyword 关键词
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 文章列表
     */
    Result<IPage<HealthArticle>> searchArticles(String keyword, Integer pageNum, Integer pageSize);

    /**
     * 发布文章
     *
     * @param article 文章信息
     * @return 发布结果
     */
    Result<HealthArticle> publishArticle(HealthArticle article);

    /**
     * 更新文章
     *
     * @param article 文章信息
     * @return 更新结果
     */
    Result<HealthArticle> updateArticle(HealthArticle article);

    /**
     * 删除文章
     *
     * @param id 文章ID
     * @param userId 用户ID
     * @return 删除结果
     */
    Result<Void> deleteArticle(Long id, Long userId);

    /**
     * 点赞文章
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 点赞结果
     */
    Result<Void> likeArticle(Long articleId, Long userId);

    /**
     * 取消点赞文章
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 取消点赞结果
     */
    Result<Void> unlikeArticle(Long articleId, Long userId);

    /**
     * 收藏文章
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     * @param remark 备注
     * @return 收藏结果
     */
    Result<Void> favoriteArticle(Long articleId, Long userId, String remark);

    /**
     * 取消收藏文章
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 取消收藏结果
     */
    Result<Void> unfavoriteArticle(Long articleId, Long userId);

    /**
     * 获取用户收藏的文章
     *
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 收藏列表
     */
    Result<IPage<HealthArticle>> getFavoriteArticles(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 获取用户发布的文章
     *
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 文章列表
     */
    Result<IPage<HealthArticle>> getMyArticles(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 获取推荐文章
     *
     * @param limit 数量限制
     * @return 文章列表
     */
    Result<List<HealthArticle>> getRecommendedArticles(Integer limit);

    /**
     * 获取热门文章
     *
     * @param limit 数量限制
     * @return 文章列表
     */
    Result<List<HealthArticle>> getPopularArticles(Integer limit);

    /**
     * 检查用户是否已点赞文章
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 是否已点赞
     */
    Result<Boolean> checkLikeStatus(Long articleId, Long userId);

    /**
     * 检查用户是否已收藏文章
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 是否已收藏
     */
    Result<Boolean> checkFavoriteStatus(Long articleId, Long userId);

    /**
     * 获取所有标签列表（用于筛选）
     *
     * @return 标签列表
     */
    Result<List<String>> getAllTags();
}

