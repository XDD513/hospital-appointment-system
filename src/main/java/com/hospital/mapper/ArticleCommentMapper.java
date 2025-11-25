package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.entity.ArticleComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文章评论Mapper接口
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Mapper
public interface ArticleCommentMapper extends BaseMapper<ArticleComment> {

    /**
     * 分页查询文章评论
     *
     * @param page 分页对象
     * @param articleId 文章ID
     * @param parentId 父评论ID（0表示顶级评论）
     * @return 评论列表
     */
    IPage<ArticleComment> selectCommentPage(Page<ArticleComment> page,
                                             @Param("articleId") Long articleId,
                                             @Param("parentId") Long parentId);

    /**
     * 查询文章的所有评论（包括子评论）
     *
     * @param articleId 文章ID
     * @return 评论列表
     */
    List<ArticleComment> selectAllByArticleId(@Param("articleId") Long articleId);

    /**
     * 查询用户的评论
     *
     * @param page 分页对象
     * @param userId 用户ID
     * @return 评论列表
     */
    IPage<ArticleComment> selectByUserId(Page<ArticleComment> page, @Param("userId") Long userId);

    /**
     * 查询评论的回复数量
     *
     * @param parentId 父评论ID
     * @return 回复数量
     */
    Integer countReplies(@Param("parentId") Long parentId);

    /**
     * 增加点赞次数
     *
     * @param id 评论ID
     */
    void incrementLikeCount(@Param("id") Long id);

    /**
     * 减少点赞次数
     *
     * @param id 评论ID
     */
    void decrementLikeCount(@Param("id") Long id);
}

