package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.entity.UserArticleFavorite;
import org.apache.ibatis.annotations.*;

/**
 * 用户收藏文章Mapper接口
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Mapper
public interface UserArticleFavoriteMapper extends BaseMapper<UserArticleFavorite> {

    /**
     * 查询用户是否已收藏
     *
     * @param userId 用户ID
     * @param articleId 文章ID
     * @return 收藏记录
     */
    @Select("SELECT * FROM user_article_favorite WHERE user_id = #{userId} AND article_id = #{articleId}")
    UserArticleFavorite selectByUserAndArticle(@Param("userId") Long userId, @Param("articleId") Long articleId);

    /**
     * 分页查询用户收藏的文章
     *
     * @param page 分页对象
     * @param userId 用户ID
     * @return 收藏列表
     */
    @Select("SELECT uaf.*, ha.title, ha.summary, ha.cover_image, ha.category, " +
            "COALESCE(u.real_name, u.username) AS author_name, " +
            "ha.view_count, ha.like_count " +
            "FROM user_article_favorite uaf " +
            "LEFT JOIN health_article ha ON uaf.article_id = ha.id " +
            "LEFT JOIN user u ON ha.author_id = u.id " +
            "WHERE uaf.user_id = #{userId} AND ha.status = 1 " +
            "ORDER BY uaf.created_at DESC")
    IPage<UserArticleFavorite> selectFavoritesByUserId(Page<UserArticleFavorite> page, @Param("userId") Long userId);

    /**
     * 删除收藏记录
     *
     * @param userId 用户ID
     * @param articleId 文章ID
     */
    @Delete("DELETE FROM user_article_favorite WHERE user_id = #{userId} AND article_id = #{articleId}")
    void deleteByUserAndArticle(@Param("userId") Long userId, @Param("articleId") Long articleId);
}

