package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.entity.HerbalRecipe;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 药膳食谱Mapper接口
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
@Mapper
public interface HerbalRecipeMapper extends BaseMapper<HerbalRecipe> {

    /**
     * 根据体质类型推荐药膳（分页）
     *
     * @param page 分页对象
     * @param constitutionType 体质类型
     * @param season 季节（可选）
     * @return 药膳列表
     */
    IPage<HerbalRecipe> selectRecommendedRecipes(Page<HerbalRecipe> page,
                                                   @Param("constitutionType") String constitutionType,
                                                   @Param("season") String season);

    /**
     * 获取全部药膳列表（分页，不包含AI推荐）
     *
     * @param page 分页对象
     * @return 药膳列表
     */
    IPage<HerbalRecipe> selectAllRecipes(Page<HerbalRecipe> page);

    /**
     * 搜索药膳（按名称或功效）
     *
     * @param page 分页对象
     * @param keyword 关键词
     * @return 药膳列表
     */
    IPage<HerbalRecipe> searchRecipes(Page<HerbalRecipe> page,
                                      @Param("keyword") String keyword,
                                      @Param("season") String season,
                                      @Param("constitutionType") String constitutionType,
                                      @Param("effect") String effect);

    /**
     * 获取热门药膳
     *
     * @param limit 数量限制
     * @return 药膳列表
     */
    List<HerbalRecipe> selectPopularRecipes(@Param("limit") Integer limit);

    /**
     * 获取时令药膳
     *
     * @param season 季节
     * @param limit 数量限制
     * @return 药膳列表
     */
    List<HerbalRecipe> selectSeasonalRecipes(@Param("season") String season, @Param("limit") Integer limit);

    /**
     * 增加浏览次数
     *
     * @param id 食谱ID
     * @return 影响行数
     */
    int incrementViewCount(@Param("id") Long id);

    /**
     * 增加收藏次数
     *
     * @param id 食谱ID
     * @return 影响行数
     */
    int incrementFavoriteCount(@Param("id") Long id);

    /**
     * 减少收藏次数
     *
     * @param id 食谱ID
     * @return 影响行数
     */
    int decrementFavoriteCount(@Param("id") Long id);

    /**
     * 获取可用于推荐的活跃药膳列表
     *
     * @param limit 数量限制
     * @return 药膳列表
     */
    List<HerbalRecipe> selectActiveRecipesForRecommendation(@Param("limit") Integer limit);

    /**
     * 根据ID集合查询有效药膳
     *
     * @param ids 食谱ID集合
     * @return 药膳列表
     */
    List<HerbalRecipe> selectActiveRecipesByIds(@Param("ids") List<Long> ids);
}

