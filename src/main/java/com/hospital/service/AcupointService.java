package com.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.common.result.Result;
import com.hospital.entity.Acupoint;
import com.hospital.entity.AcupointCombination;

import java.util.List;

/**
 * 穴位服务接口
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
public interface AcupointService {

    /**
     * 分页查询穴位列表
     *
     * @param meridian 经络（可选）
     * @param constitutionType 体质类型（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 穴位列表
     */
    Result<IPage<Acupoint>> getAcupointList(String meridian, String constitutionType, Integer pageNum, Integer pageSize);

    /**
     * 获取穴位详情
     *
     * @param id 穴位ID
     * @return 穴位详情
     */
    Result<Acupoint> getAcupointDetail(Long id);

    /**
     * 搜索穴位
     *
     * @param keyword 关键词
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 穴位列表
     */
    Result<IPage<Acupoint>> searchAcupoints(String keyword, Integer pageNum, Integer pageSize);

    /**
     * 根据经络查询穴位
     *
     * @param meridian 经络名称
     * @return 穴位列表
     */
    Result<List<Acupoint>> getAcupointsByMeridian(String meridian);

    /**
     * 获取所有经络列表
     *
     * @return 经络列表
     */
    Result<List<String>> getAllMeridians();

    /**
     * 获取热门穴位
     *
     * @param limit 数量限制
     * @return 穴位列表
     */
    Result<List<Acupoint>> getPopularAcupoints(Integer limit);

    /**
     * 分页查询穴位组合列表
     *
     * @param constitutionType 体质类型（可选）
     * @param symptom 症状（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 穴位组合列表
     */
    Result<IPage<AcupointCombination>> getCombinationList(String constitutionType, String symptom, Integer pageNum, Integer pageSize);

    /**
     * 获取穴位组合详情
     *
     * @param id 组合ID
     * @return 穴位组合详情（包含穴位详细信息）
     */
    Result<AcupointCombination> getCombinationDetail(Long id);

    /**
     * 根据用户体质推荐穴位组合
     *
     * @param userId 用户ID
     * @param limit 数量限制
     * @return 推荐的穴位组合列表
     */
    Result<List<AcupointCombination>> getRecommendedCombinations(Long userId, Integer limit);

    /**
     * 搜索穴位组合
     *
     * @param keyword 关键词
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 穴位组合列表
     */
    Result<IPage<AcupointCombination>> searchCombinations(String keyword, Integer pageNum, Integer pageSize);

    /**
     * 根据症状查询穴位组合
     *
     * @param symptom 症状
     * @return 穴位组合列表
     */
    Result<List<AcupointCombination>> getCombinationsBySymptom(String symptom);

    /**
     * 获取热门穴位组合
     *
     * @param limit 数量限制
     * @return 穴位组合列表
     */
    Result<List<AcupointCombination>> getPopularCombinations(Integer limit);
}

