package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.common.result.Result;
import com.hospital.common.result.ResultCode;
import com.hospital.entity.Acupoint;
import com.hospital.entity.AcupointCombination;
import com.hospital.entity.UserConstitutionTest;
import com.hospital.mapper.AcupointCombinationMapper;
import com.hospital.mapper.AcupointMapper;
import com.hospital.mapper.UserConstitutionTestMapper;
import com.hospital.service.AcupointService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 穴位服务实现类
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Slf4j
@Service
public class AcupointServiceImpl implements AcupointService {

    @Autowired
    private AcupointMapper acupointMapper;

    @Autowired
    private AcupointCombinationMapper acupointCombinationMapper;

    @Autowired
    private UserConstitutionTestMapper constitutionTestMapper;

    @Autowired
    private com.hospital.util.RedisUtil redisUtil;

    /**
     * 分页查询穴位列表
     */
    @Override
    public Result<IPage<Acupoint>> getAcupointList(String meridian, String constitutionType, Integer pageNum, Integer pageSize) {
        try {
            // 只缓存前3页，使用参数哈希简化层级
            Map<String, Object> filterParams = new java.util.HashMap<>();
            if (meridian != null && !"all".equals(meridian)) {
                filterParams.put("meridian", meridian);
            }
            if (constitutionType != null && !"all".equals(constitutionType)) {
                filterParams.put("type", constitutionType);
            }
            String cacheKey = redisUtil.buildCacheKey("hospital:common:acupoint:list", pageNum, pageSize, filterParams);

            if (pageNum <= 3) {
                Object cached = redisUtil.get(cacheKey);
                if (cached instanceof IPage) {
                    try {
                        @SuppressWarnings("unchecked")
                        IPage<Acupoint> cachedPage = (IPage<Acupoint>) cached;
                        log.info("从缓存获取穴位列表");
                        return Result.success(cachedPage);
                    } catch (ClassCastException ignored) {}
                }
            }

            Page<Acupoint> page = new Page<>(pageNum, pageSize);
            IPage<Acupoint> result = acupointMapper.selectAcupointPage(page, meridian, constitutionType);

            // 缓存前3页（30分钟）
            if (pageNum <= 3) {
                redisUtil.set(cacheKey, result, 30, java.util.concurrent.TimeUnit.MINUTES);
            }

            log.info("查询穴位列表：经络={}，体质={}，共{}条", meridian, constitutionType, result.getTotal());
            return Result.success(result);

        } catch (Exception e) {
            log.error("查询穴位列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 获取穴位详情
     */
    @Override
    public Result<Acupoint> getAcupointDetail(Long id) {
        try {
            String cacheKey = "hospital:common:acupoint:detail:id:" + id;

            // 尝试从缓存获取
            Object cached = redisUtil.get(cacheKey);
            Acupoint acupoint = null;
            if (cached instanceof Acupoint) {
                acupoint = (Acupoint) cached;
                log.info("从缓存获取穴位详情: {}", id);
            } else {
                acupoint = acupointMapper.selectById(id);
                if (acupoint == null) {
                    return Result.error(ResultCode.DATA_NOT_FOUND.getCode(), "穴位不存在");
                }
                // 存入缓存（30分钟）
                redisUtil.set(cacheKey, acupoint, 30, java.util.concurrent.TimeUnit.MINUTES);
            }

            // 增加浏览次数（无论是否从缓存获取）
            acupointMapper.incrementViewCount(id);

            log.info("查询穴位详情：id={}，名称={}", id, acupoint.getAcupointName());
            return Result.success(acupoint);

        } catch (Exception e) {
            log.error("查询穴位详情失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 搜索穴位
     */
    @Override
    public Result<IPage<Acupoint>> searchAcupoints(String keyword, Integer pageNum, Integer pageSize) {
        try {
            // 只缓存前3页，关键词作为特殊参数处理（保留可读性）
            Map<String, Object> filterParams = new java.util.HashMap<>();
            if (keyword != null && !keyword.isEmpty()) {
                filterParams.put("keyword", keyword);
            }
            String cacheKey = redisUtil.buildCacheKey("hospital:common:acupoint:search", pageNum, pageSize, filterParams);

            if (pageNum <= 3) {
                Object cached = redisUtil.get(cacheKey);
                if (cached instanceof IPage) {
                    try {
                        @SuppressWarnings("unchecked")
                        IPage<Acupoint> cachedPage = (IPage<Acupoint>) cached;
                        log.info("从缓存获取穴位搜索结果");
                        return Result.success(cachedPage);
                    } catch (ClassCastException ignored) {}
                }
            }

            Page<Acupoint> page = new Page<>(pageNum, pageSize);
            IPage<Acupoint> result = acupointMapper.searchAcupoints(page, keyword);

            // 缓存前3页（30分钟）
            if (pageNum <= 3) {
                redisUtil.set(cacheKey, result, 30, java.util.concurrent.TimeUnit.MINUTES);
            }

            log.info("搜索穴位：关键词={}，共{}条", keyword, result.getTotal());
            return Result.success(result);

        } catch (Exception e) {
            log.error("搜索穴位失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 根据经络查询穴位
     */
    @Override
    public Result<List<Acupoint>> getAcupointsByMeridian(String meridian) {
        try {
            String cacheKey = "hospital:common:acupoint:list:meridian:" + meridian;

            // 尝试从缓存获取
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Acupoint> list = (List<Acupoint>) cached;
                    log.info("从缓存获取经络穴位");
                    return Result.success(list);
                } catch (ClassCastException ignored) {}
            }

            List<Acupoint> acupoints = acupointMapper.selectByMeridian(meridian);

            // 存入缓存（30分钟）
            redisUtil.set(cacheKey, acupoints, 30, java.util.concurrent.TimeUnit.MINUTES);

            log.info("查询经络穴位：经络={}，共{}个", meridian, acupoints.size());
            return Result.success(acupoints);

        } catch (Exception e) {
            log.error("查询经络穴位失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 获取所有经络列表
     */
    @Override
    public Result<List<String>> getAllMeridians() {
        try {
            String cacheKey = "acupoint:meridians:all";

            // 尝试从缓存获取
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<String> list = (List<String>) cached;
                    log.info("从缓存获取经络列表");
                    return Result.success(list);
                } catch (ClassCastException ignored) {}
            }

            List<String> meridians = acupointMapper.selectAllMeridians();

            // 存入缓存（永久）
            redisUtil.set(cacheKey, meridians);

            log.info("查询所有经络：共{}条", meridians.size());
            return Result.success(meridians);

        } catch (Exception e) {
            log.error("查询所有经络失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 获取热门穴位
     */
    @Override
    public Result<List<Acupoint>> getPopularAcupoints(Integer limit) {
        try {
            String cacheKey = "acupoint:popular:limit:" + limit;

            // 尝试从缓存获取
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Acupoint> list = (List<Acupoint>) cached;
                    log.info("从缓存获取热门穴位");
                    return Result.success(list);
                } catch (ClassCastException ignored) {}
            }

            List<Acupoint> acupoints = acupointMapper.selectPopularAcupoints(limit);

            // 存入缓存（30分钟）
            redisUtil.set(cacheKey, acupoints, 30, java.util.concurrent.TimeUnit.MINUTES);

            log.info("查询热门穴位：共{}个", acupoints.size());
            return Result.success(acupoints);

        } catch (Exception e) {
            log.error("查询热门穴位失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 分页查询穴位组合列表
     */
    @Override
    public Result<IPage<AcupointCombination>> getCombinationList(String constitutionType, String symptom, Integer pageNum, Integer pageSize) {
        try {
            // 只缓存前3页，使用参数哈希简化层级
            Map<String, Object> filterParams = new java.util.HashMap<>();
            if (constitutionType != null && !"all".equals(constitutionType)) {
                filterParams.put("type", constitutionType);
            }
            if (symptom != null && !"all".equals(symptom)) {
                filterParams.put("symptom", symptom);
            }
            String cacheKey = redisUtil.buildCacheKey("hospital:common:acupoint:combo:list", pageNum, pageSize, filterParams);

            if (pageNum <= 3) {
                Object cached = redisUtil.get(cacheKey);
                if (cached instanceof IPage) {
                    try {
                        @SuppressWarnings("unchecked")
                        IPage<AcupointCombination> cachedPage = (IPage<AcupointCombination>) cached;
                        log.info("从缓存获取穴位组合列表");
                        return Result.success(cachedPage);
                    } catch (ClassCastException ignored) {}
                }
            }

            Page<AcupointCombination> page = new Page<>(pageNum, pageSize);
            IPage<AcupointCombination> result = acupointCombinationMapper.selectCombinationPage(page, constitutionType, symptom);

            // 缓存前3页（30分钟）
            if (pageNum <= 3) {
                redisUtil.set(cacheKey, result, 30, java.util.concurrent.TimeUnit.MINUTES);
            }

            log.info("查询穴位组合列表：体质={}，症状={}，共{}条", constitutionType, symptom, result.getTotal());
            return Result.success(result);

        } catch (Exception e) {
            log.error("查询穴位组合列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 获取穴位组合详情
     */
    @Override
    public Result<AcupointCombination> getCombinationDetail(Long id) {
        try {
            AcupointCombination combination = acupointCombinationMapper.selectById(id);
            if (combination == null) {
                return Result.error(ResultCode.DATA_NOT_FOUND.getCode(), "穴位组合不存在");
            }

            log.info("查询穴位组合详情：id={}，名称={}", id, combination.getCombinationName());
            return Result.success(combination);

        } catch (Exception e) {
            log.error("查询穴位组合详情失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 根据用户体质推荐穴位组合
     */
    @Override
    public Result<List<AcupointCombination>> getRecommendedCombinations(Long userId, Integer limit) {
        try {
            // 1. 获取用户最新的体质测试结果
            UserConstitutionTest latestTest = constitutionTestMapper.selectLatestByUserId(userId);
            if (latestTest == null) {
                return Result.error(ResultCode.DATA_NOT_FOUND.getCode(), "请先完成体质测试");
            }

            String constitutionType = latestTest.getPrimaryConstitution();

            // 2. 根据体质推荐穴位组合
            List<AcupointCombination> combinations = acupointCombinationMapper.selectRecommendedCombinations(constitutionType, limit);

            log.info("推荐穴位组合：用户ID={}，体质={}，共{}个", userId, constitutionType, combinations.size());
            return Result.success(combinations);

        } catch (Exception e) {
            log.error("推荐穴位组合失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 搜索穴位组合
     */
    @Override
    public Result<IPage<AcupointCombination>> searchCombinations(String keyword, Integer pageNum, Integer pageSize) {
        try {
            Page<AcupointCombination> page = new Page<>(pageNum, pageSize);
            IPage<AcupointCombination> result = acupointCombinationMapper.searchCombinations(page, keyword);
            log.info("搜索穴位组合：关键词={}，共{}条", keyword, result.getTotal());
            return Result.success(result);

        } catch (Exception e) {
            log.error("搜索穴位组合失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 根据症状查询穴位组合
     */
    @Override
    public Result<List<AcupointCombination>> getCombinationsBySymptom(String symptom) {
        try {
            List<AcupointCombination> combinations = acupointCombinationMapper.selectBySymptom(symptom);
            log.info("查询症状穴位组合：症状={}，共{}个", symptom, combinations.size());
            return Result.success(combinations);

        } catch (Exception e) {
            log.error("查询症状穴位组合失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 获取热门穴位组合
     */
    @Override
    public Result<List<AcupointCombination>> getPopularCombinations(Integer limit) {
        try {
            List<AcupointCombination> combinations = acupointCombinationMapper.selectPopularCombinations(limit);
            log.info("查询热门穴位组合：共{}个", combinations.size());
            return Result.success(combinations);

        } catch (Exception e) {
            log.error("查询热门穴位组合失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }
}

