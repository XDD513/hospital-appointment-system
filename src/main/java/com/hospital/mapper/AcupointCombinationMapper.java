package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.entity.AcupointCombination;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 穴位组合方案Mapper接口
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Mapper
public interface AcupointCombinationMapper extends BaseMapper<AcupointCombination> {

    /**
     * 分页查询穴位组合列表
     *
     * @param page 分页对象
     * @param constitutionType 体质类型（可选）
     * @param symptom 症状（可选）
     * @return 穴位组合列表
     */
    IPage<AcupointCombination> selectCombinationPage(Page<AcupointCombination> page,
                                                      @Param("constitutionType") String constitutionType,
                                                      @Param("symptom") String symptom);

    /**
     * 根据体质推荐穴位组合
     *
     * @param constitutionType 体质类型
     * @param limit 数量限制
     * @return 穴位组合列表
     */
    List<AcupointCombination> selectRecommendedCombinations(@Param("constitutionType") String constitutionType,
                                                             @Param("limit") Integer limit);

    /**
     * 搜索穴位组合（按名称、症状）
     *
     * @param page 分页对象
     * @param keyword 关键词
     * @return 穴位组合列表
     */
    IPage<AcupointCombination> searchCombinations(Page<AcupointCombination> page, @Param("keyword") String keyword);

    /**
     * 根据症状查询穴位组合
     *
     * @param symptom 症状
     * @return 穴位组合列表
     */
    List<AcupointCombination> selectBySymptom(@Param("symptom") String symptom);

    /**
     * 获取热门穴位组合
     *
     * @param limit 数量限制
     * @return 穴位组合列表
     */
    List<AcupointCombination> selectPopularCombinations(@Param("limit") Integer limit);
}

