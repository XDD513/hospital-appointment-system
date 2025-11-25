package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.common.result.Result;
import com.hospital.common.result.ResultCode;
import com.hospital.entity.ArticleComment;
import com.hospital.entity.UserLike;
import com.hospital.mapper.ArticleCommentMapper;
import com.hospital.mapper.HealthArticleMapper;
import com.hospital.mapper.UserLikeMapper;
import com.hospital.service.ArticleCommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文章评论服务实现类
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Slf4j
@Service
public class ArticleCommentServiceImpl implements ArticleCommentService {

    @Autowired
    private ArticleCommentMapper commentMapper;

    @Autowired
    private HealthArticleMapper articleMapper;

    @Autowired
    private UserLikeMapper userLikeMapper;

    /**
     * 分页查询文章评论
     */
    @Override
    public Result<IPage<ArticleComment>> getCommentList(Long articleId, Integer pageNum, Integer pageSize) {
        try {
            Page<ArticleComment> page = new Page<>(pageNum, pageSize);
            // 查询顶级评论（parentId = 0）
            IPage<ArticleComment> result = commentMapper.selectCommentPage(page, articleId, 0L);
            log.info("查询文章评论：文章ID={}，共{}条", articleId, result.getTotal());
            return Result.success(result);

        } catch (Exception e) {
            log.error("查询文章评论失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 发表评论
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<ArticleComment> publishComment(ArticleComment comment) {
        try {
            // 设置初始值
            comment.setLikeCount(0);
            comment.setStatus(1); // 已发布

            commentMapper.insert(comment);

            // 增加文章评论数
            articleMapper.incrementCommentCount(comment.getArticleId());

            log.info("发表评论成功：文章ID={}，用户ID={}", comment.getArticleId(), comment.getUserId());
            return Result.success(comment);

        } catch (Exception e) {
            log.error("发表评论失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 删除评论
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteComment(Long id, Long userId) {
        try {
            ArticleComment comment = commentMapper.selectById(id);
            if (comment == null) {
                return Result.error(ResultCode.DATA_NOT_FOUND.getCode(), "评论不存在");
            }

            // 只允许评论作者删除自己的评论
            if (!comment.getUserId().equals(userId)) {
                return Result.error(ResultCode.FORBIDDEN.getCode(), "无权限删除此评论");
            }

            // 软删除：更新状态为已删除
            comment.setStatus(2);
            commentMapper.updateById(comment);

            // 减少文章评论数
            articleMapper.decrementCommentCount(comment.getArticleId());

            log.info("删除评论成功：评论ID={}，用户ID={}", id, userId);
            return Result.success();

        } catch (Exception e) {
            log.error("删除评论失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 点赞评论
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> likeComment(Long commentId, Long userId) {
        try {
            // 检查是否已点赞
            UserLike existingLike = userLikeMapper.selectByUserAndTarget(userId, commentId, "COMMENT");
            if (existingLike != null) {
                return Result.error(ResultCode.OPERATION_FAILED.getCode(), "已点赞过该评论");
            }

            // 创建点赞记录
            UserLike userLike = new UserLike();
            userLike.setUserId(userId);
            userLike.setTargetId(commentId);
            userLike.setTargetType("COMMENT");
            userLikeMapper.insert(userLike);

            // 增加评论点赞数
            commentMapper.incrementLikeCount(commentId);

            log.info("点赞评论成功：评论ID={}，用户ID={}", commentId, userId);
            return Result.success();

        } catch (Exception e) {
            log.error("点赞评论失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 取消点赞评论
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> unlikeComment(Long commentId, Long userId) {
        try {
            // 检查是否已点赞
            UserLike existingLike = userLikeMapper.selectByUserAndTarget(userId, commentId, "COMMENT");
            if (existingLike == null) {
                return Result.error(ResultCode.OPERATION_FAILED.getCode(), "未点赞过该评论");
            }

            // 删除点赞记录
            userLikeMapper.deleteByUserAndTarget(userId, commentId, "COMMENT");

            // 减少评论点赞数
            commentMapper.decrementLikeCount(commentId);

            log.info("取消点赞评论成功：评论ID={}，用户ID={}", commentId, userId);
            return Result.success();

        } catch (Exception e) {
            log.error("取消点赞评论失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 检查用户是否已点赞评论
     */
    @Override
    public Result<Boolean> checkCommentLikeStatus(Long commentId, Long userId) {
        try {
            UserLike existingLike = userLikeMapper.selectByUserAndTarget(userId, commentId, "COMMENT");
            boolean isLiked = existingLike != null;
            log.info("检查评论点赞状态：评论ID={}，用户ID={}，已点赞={}", commentId, userId, isLiked);
            return Result.success(isLiked);

        } catch (Exception e) {
            log.error("检查评论点赞状态失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }
}

