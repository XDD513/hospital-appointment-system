package com.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.common.result.Result;
import com.hospital.entity.ArticleComment;

/**
 * 文章评论服务接口
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
public interface ArticleCommentService {

    /**
     * 分页查询文章评论
     *
     * @param articleId 文章ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 评论列表
     */
    Result<IPage<ArticleComment>> getCommentList(Long articleId, Integer pageNum, Integer pageSize);

    /**
     * 发表评论
     *
     * @param comment 评论信息
     * @return 发表结果
     */
    Result<ArticleComment> publishComment(ArticleComment comment);

    /**
     * 删除评论
     *
     * @param id 评论ID
     * @param userId 用户ID
     * @return 删除结果
     */
    Result<Void> deleteComment(Long id, Long userId);

    /**
     * 点赞评论
     *
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 点赞结果
     */
    Result<Void> likeComment(Long commentId, Long userId);

    /**
     * 取消点赞评论
     *
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 取消点赞结果
     */
    Result<Void> unlikeComment(Long commentId, Long userId);

    /**
     * 检查用户是否已点赞评论
     *
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 是否已点赞
     */
    Result<Boolean> checkCommentLikeStatus(Long commentId, Long userId);
}

