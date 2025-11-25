package com.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.entity.Acupoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 穴位信息Mapper接口
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Mapper
public interface AcupointMapper extends BaseMapper<Acupoint> {

    /**
     * 分页查询穴位列表
     *
     * @param page 分页对象
     * @param meridian 经络（可选）
     * @param constitutionType 体质类型（可选）
     * @return 穴位列表
     */
    IPage<Acupoint> selectAcupointPage(Page<Acupoint> page,
                                        @Param("meridian") String meridian,
                                        @Param("constitutionType") String constitutionType);

    /**
     * 搜索穴位（按名称、拼音、主治）
     *
     * @param page 分页对象
     * @param keyword 关键词
     * @return 穴位列表
     */
    IPage<Acupoint> searchAcupoints(Page<Acupoint> page, @Param("keyword") String keyword);

    /**
     * 根据经络查询穴位
     *
     * @param meridian 经络名称
     * @return 穴位列表
     */
    List<Acupoint> selectByMeridian(@Param("meridian") String meridian);

    /**
     * 根据体质推荐穴位
     *
     * @param constitutionType 体质类型
     * @param limit 数量限制
     * @return 穴位列表
     */
    List<Acupoint> selectRecommendedAcupoints(@Param("constitutionType") String constitutionType,
                                               @Param("limit") Integer limit);

    /**
     * 根据ID列表批量查询穴位
     *
     * @param ids 穴位ID列表
     * @return 穴位列表
     */
    List<Acupoint> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 获取所有经络列表
     *
     * @return 经络列表
     */
    List<String> selectAllMeridians();

    /**
     * 增加浏览次数
     *
     * @param id 穴位ID
     */
    void incrementViewCount(@Param("id") Long id);

    /**
     * 获取热门穴位
     *
     * @param limit 数量限制
     * @return 穴位列表
     */
    List<Acupoint> selectPopularAcupoints(@Param("limit") Integer limit);
}

