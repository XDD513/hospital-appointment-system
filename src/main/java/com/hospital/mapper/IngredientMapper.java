package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospital.entity.Ingredient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 食材库Mapper接口
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
@Mapper
public interface IngredientMapper extends BaseMapper<Ingredient> {

    /**
     * 根据体质类型查询适用食材
     *
     * @param constitutionType 体质类型
     * @return 食材列表
     */
    @Select("SELECT * FROM ingredient WHERE status = 1 " +
            "AND suitable_constitution LIKE CONCAT('%', #{constitutionType}, '%') " +
            "ORDER BY category, name")
    List<Ingredient> selectByConstitutionType(@Param("constitutionType") String constitutionType);

    /**
     * 根据分类查询食材
     *
     * @param category 食材分类
     * @return 食材列表
     */
    @Select("SELECT * FROM ingredient WHERE status = 1 AND category = #{category} ORDER BY name")
    List<Ingredient> selectByCategory(@Param("category") String category);

    /**
     * 搜索食材
     *
     * @param keyword 关键词
     * @return 食材列表
     */
    @Select("SELECT * FROM ingredient WHERE status = 1 " +
            "AND (name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR efficacy LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY name")
    List<Ingredient> searchIngredients(@Param("keyword") String keyword);
}

