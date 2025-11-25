package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.entity.UserRecipeFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户药膳收藏Mapper接口
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
@Mapper
public interface UserRecipeFavoriteMapper extends BaseMapper<UserRecipeFavorite> {

    /**
     * 查询用户是否已收藏某个食谱
     *
     * @param userId 用户ID
     * @param recipeId 食谱ID
     * @return 收藏记录
     */
    @Select("SELECT * FROM user_recipe_favorite WHERE user_id = #{userId} AND recipe_id = #{recipeId}")
    UserRecipeFavorite selectByUserIdAndRecipeId(@Param("userId") Long userId, @Param("recipeId") Long recipeId);

    /**
     * 查询用户收藏的食谱列表（分页，关联查询食谱详情）
     *
     * @param page 分页对象
     * @param userId 用户ID
     * @return 收藏列表
     */
    @Select("SELECT f.*, r.recipe_name, r.category, r.difficulty, r.cooking_time, " +
            "r.efficacy, r.image, r.view_count, r.favorite_count " +
            "FROM user_recipe_favorite f " +
            "LEFT JOIN herbal_recipe r ON f.recipe_id = r.id " +
            "WHERE f.user_id = #{userId} AND r.status = 1 " +
            "ORDER BY f.created_at DESC")
    IPage<UserRecipeFavorite> selectFavoritesByUserId(Page<UserRecipeFavorite> page, @Param("userId") Long userId);

    /**
     * 统计用户收藏数量
     *
     * @param userId 用户ID
     * @return 收藏数量
     */
    @Select("SELECT COUNT(*) FROM user_recipe_favorite WHERE user_id = #{userId}")
    Integer countByUserId(@Param("userId") Long userId);

    /**
     * 查询用户收藏的全部食谱ID
     *
     * @param userId 用户ID
     * @return 食谱ID列表
     */
    @Select("SELECT recipe_id FROM user_recipe_favorite WHERE user_id = #{userId}")
    List<Long> selectRecipeIdsByUserId(@Param("userId") Long userId);
}

