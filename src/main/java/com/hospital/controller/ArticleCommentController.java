package com.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.common.result.Result;
import com.hospital.entity.ArticleComment;
import com.hospital.service.ArticleCommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 文章评论控制器
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Slf4j
@RestController
@RequestMapping("/api/comment")
public class ArticleCommentController {

    @Autowired
    private ArticleCommentService commentService;

    /**
     * 分页查询文章评论
     *
     * @param articleId 文章ID
     * @param pageNum 页码（默认1）
     * @param pageSize 每页数量（默认10）
     * @return 评论列表
     */
    @GetMapping("/list")
    public Result<IPage<ArticleComment>> getCommentList(
            @RequestParam Long articleId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        log.info("查询文章评论：文章ID={}，页码={}，每页={}条", articleId, pageNum, pageSize);
        return commentService.getCommentList(articleId, pageNum, pageSize);
    }

    /**
     * 发表评论
     *
     * @param comment 评论信息
     * @return 发表结果
     */
    @PostMapping("/publish")
    public Result<ArticleComment> publishComment(@RequestBody ArticleComment comment) {
        log.info("发表评论：文章ID={}，用户ID={}", comment.getArticleId(), comment.getUserId());
        return commentService.publishComment(comment);
    }

    /**
     * 删除评论
     *
     * @param id 评论ID
     * @param userId 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id, @RequestParam Long userId) {
        log.info("删除评论：评论ID={}，用户ID={}", id, userId);
        return commentService.deleteComment(id, userId);
    }

    /**
     * 点赞评论
     *
     * @param id 评论ID
     * @param userId 用户ID
     * @return 点赞结果
     */
    @PostMapping("/like/{id}")
    public Result<Void> likeComment(@PathVariable Long id, @RequestParam Long userId) {
        log.info("点赞评论：评论ID={}，用户ID={}", id, userId);
        return commentService.likeComment(id, userId);
    }

    /**
     * 取消点赞评论
     *
     * @param id 评论ID
     * @param userId 用户ID
     * @return 取消点赞结果
     */
    @DeleteMapping("/like/{id}")
    public Result<Void> unlikeComment(@PathVariable Long id, @RequestParam Long userId) {
        log.info("取消点赞评论：评论ID={}，用户ID={}", id, userId);
        return commentService.unlikeComment(id, userId);
    }

    /**
     * 检查用户是否已点赞评论
     *
     * @param id 评论ID
     * @param userId 用户ID
     * @return 是否已点赞
     */
    @GetMapping("/like/status/{id}")
    public Result<Boolean> checkCommentLikeStatus(@PathVariable Long id, @RequestParam Long userId) {
        log.info("检查评论点赞状态：评论ID={}，用户ID={}", id, userId);
        return commentService.checkCommentLikeStatus(id, userId);
    }
}

