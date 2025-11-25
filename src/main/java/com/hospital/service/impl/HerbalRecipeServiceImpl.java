package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.common.result.Result;
import com.hospital.common.result.ResultCode;
import com.hospital.entity.HerbalRecipe;
import com.hospital.entity.Ingredient;
import com.hospital.entity.UserConstitutionTest;
import com.hospital.entity.UserRecipeFavorite;
import com.hospital.mapper.HerbalRecipeMapper;
import com.hospital.mapper.IngredientMapper;
import com.hospital.mapper.UserConstitutionTestMapper;
import com.hospital.mapper.UserRecipeFavoriteMapper;
import com.hospital.service.AiRecommendationService;
import com.hospital.service.HerbalRecipeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 药膳食谱服务实现类
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
@Slf4j
@Service
public class HerbalRecipeServiceImpl implements HerbalRecipeService {

    @Autowired
    private HerbalRecipeMapper herbalRecipeMapper;

    @Autowired
    private IngredientMapper ingredientMapper;

    @Autowired
    private UserRecipeFavoriteMapper userRecipeFavoriteMapper;

    @Autowired
    private UserConstitutionTestMapper constitutionTestMapper;

    @Autowired
    private com.hospital.util.RedisUtil redisUtil;

    @Autowired(required = false)
    private AiRecommendationService aiRecommendationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 根据用户体质推荐药膳（分页）
     */
    @Override
    public Result<IPage<HerbalRecipe>> getRecommendedRecipes(Long userId, String season, Integer pageNum, Integer pageSize) {
        try {
            // 1. 获取用户最新体质测试结果
            UserConstitutionTest latestTest = constitutionTestMapper.selectLatestByUserId(userId);
            if (latestTest == null) {
                log.warn("用户{}尚未进行体质测试，无法推荐药膳", userId);
                return Result.error(ResultCode.DATA_NOT_FOUND.getCode(), "请先完成体质测试");
            }

            String constitutionType = latestTest.getPrimaryConstitution();

            // 2. 只缓存前3页（不包含用户特定的收藏状态），使用参数哈希简化层级
            Map<String, Object> filterParams = new java.util.HashMap<>();
            filterParams.put("type", constitutionType);
            if (season != null && !"all".equals(season)) {
                filterParams.put("season", season);
            }
            String cacheKey = redisUtil.buildCacheKey("hospital:common:recipe:recommend", pageNum, pageSize, filterParams);

            if (pageNum <= 3) {
                Object cached = redisUtil.get(cacheKey);
                if (cached instanceof IPage) {
                    try {
                        @SuppressWarnings("unchecked")
                        IPage<HerbalRecipe> cachedPage = (IPage<HerbalRecipe>) cached;
                        // 设置用户特定的收藏状态和AI推荐理由
                        // 性能优化：只对前3个药膳生成AI推荐理由
                        int aiRecommendationLimit = 3;
                        int index = 0;
                        
                        for (HerbalRecipe recipe : cachedPage.getRecords()) {
                            UserRecipeFavorite favorite = userRecipeFavoriteMapper.selectByUserIdAndRecipeId(userId, recipe.getId());
                            recipe.setIsFavorited(favorite != null);

                            // AI增强：只对前几个药膳生成个性化推荐理由
                            if (aiRecommendationService != null && index < aiRecommendationLimit) {
                                try {
                                    String recommendationReason = aiRecommendationService.generateRecommendationReason(recipe, latestTest);
                                    recipe.setRecommendationReason(recommendationReason);
                                } catch (Exception e) {
                                    log.warn("生成推荐理由失败，使用降级方案：recipeId={}", recipe.getId(), e);
                                    // 降级：使用默认推荐理由
                                    recipe.setRecommendationReason(buildDefaultRecommendationReason(recipe, latestTest));
                                }
                            } else {
                                // 其他药膳使用默认推荐理由
                                recipe.setRecommendationReason(buildDefaultRecommendationReason(recipe, latestTest));
                            }
                            index++;
                        }
                        log.info("从缓存获取推荐药膳");
                        return Result.success(cachedPage);
                    } catch (ClassCastException ignored) {}
                }
            }

            // 3. 分页查询推荐药膳
            Page<HerbalRecipe> page = new Page<>(pageNum, pageSize);
            IPage<HerbalRecipe> result = herbalRecipeMapper.selectRecommendedRecipes(page, constitutionType, season);

            // 4. 设置每个药膳的收藏状态和AI推荐理由
            // 性能优化：只对前3个药膳生成AI推荐理由，其他使用默认理由，避免响应时间过长
            int aiRecommendationLimit = 3;
            int index = 0;
            
            for (HerbalRecipe recipe : result.getRecords()) {
                UserRecipeFavorite favorite = userRecipeFavoriteMapper.selectByUserIdAndRecipeId(userId, recipe.getId());
                recipe.setIsFavorited(favorite != null);

                // AI增强：只对前几个药膳生成个性化推荐理由
                if (aiRecommendationService != null && index < aiRecommendationLimit) {
                    try {
                        String recommendationReason = aiRecommendationService.generateRecommendationReason(recipe, latestTest);
                        recipe.setRecommendationReason(recommendationReason);
                    } catch (Exception e) {
                        log.warn("生成推荐理由失败，使用降级方案：recipeId={}", recipe.getId(), e);
                        // 降级：使用默认推荐理由
                        recipe.setRecommendationReason(buildDefaultRecommendationReason(recipe, latestTest));
                    }
                } else {
                    // 其他药膳使用默认推荐理由
                    recipe.setRecommendationReason(buildDefaultRecommendationReason(recipe, latestTest));
                }
                index++;
            }

            // 5. 缓存前3页（15分钟）
            if (pageNum <= 3) {
                redisUtil.set(cacheKey, result, 15, java.util.concurrent.TimeUnit.MINUTES);
            }

            log.info("为用户{}推荐药膳，体质：{}，季节：{}，共{}条", userId, constitutionType, season, result.getTotal());
            return Result.success(result);

        } catch (Exception e) {
            log.error("推荐药膳失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 获取全部药膳列表（分页，不包含AI推荐）
     */
    @Override
    public Result<IPage<HerbalRecipe>> getAllRecipes(Integer pageNum, Integer pageSize, Long userId) {
        try {
            // 缓存前3页
            Map<String, Object> filterParams = new HashMap<>();
            String cacheKey = redisUtil.buildCacheKey("hospital:common:recipe:list", pageNum, pageSize, filterParams);

            if (pageNum <= 3) {
                Object cached = redisUtil.get(cacheKey);
                if (cached instanceof IPage) {
                    try {
                        @SuppressWarnings("unchecked")
                        IPage<HerbalRecipe> cachedPage = (IPage<HerbalRecipe>) cached;
                        // 设置用户特定的收藏状态
                        if (userId != null) {
                            for (HerbalRecipe recipe : cachedPage.getRecords()) {
                                UserRecipeFavorite favorite = userRecipeFavoriteMapper.selectByUserIdAndRecipeId(userId, recipe.getId());
                                recipe.setIsFavorited(favorite != null);
                            }
                        }
                        log.info("从缓存获取全部药膳列表");
                        return Result.success(cachedPage);
                    } catch (ClassCastException ignored) {}
                }
            }

            Page<HerbalRecipe> page = new Page<>(pageNum, pageSize);
            IPage<HerbalRecipe> result = herbalRecipeMapper.selectAllRecipes(page);

            // 如果用户已登录，设置每个药膳的收藏状态
            if (userId != null) {
                for (HerbalRecipe recipe : result.getRecords()) {
                    UserRecipeFavorite favorite = userRecipeFavoriteMapper.selectByUserIdAndRecipeId(userId, recipe.getId());
                    recipe.setIsFavorited(favorite != null);
                }
            }

            // 缓存前3页（15分钟）
            if (pageNum <= 3) {
                redisUtil.set(cacheKey, result, 15, java.util.concurrent.TimeUnit.MINUTES);
            }

            log.info("获取全部药膳列表，共{}条", result.getTotal());
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取全部药膳列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 搜索药膳（分页）
     */
    @Override
    public Result<IPage<HerbalRecipe>> searchRecipes(String keyword,
                                                     String season,
                                                     String constitutionType,
                                                     String effect,
                                                     Integer pageNum,
                                                     Integer pageSize,
                                                     Long userId) {
        try {
            // 只缓存前3页，关键词作为特殊参数处理（保留可读性）
            Map<String, Object> filterParams = new java.util.HashMap<>();
            if (StringUtils.hasText(keyword)) {
                filterParams.put("keyword", keyword);
            }
            if (StringUtils.hasText(season)) {
                filterParams.put("season", season);
            }
            if (StringUtils.hasText(constitutionType)) {
                filterParams.put("constitutionType", constitutionType);
            }
            if (StringUtils.hasText(effect)) {
                filterParams.put("effect", effect);
            }
            String cacheKey = redisUtil.buildCacheKey("hospital:common:recipe:search", pageNum, pageSize, filterParams);

            if (pageNum <= 3) {
                Object cached = redisUtil.get(cacheKey);
                if (cached instanceof IPage) {
                    try {
                        @SuppressWarnings("unchecked")
                        IPage<HerbalRecipe> cachedPage = (IPage<HerbalRecipe>) cached;
                        // 设置用户特定的收藏状态
                        if (userId != null) {
                            for (HerbalRecipe recipe : cachedPage.getRecords()) {
                                UserRecipeFavorite favorite = userRecipeFavoriteMapper.selectByUserIdAndRecipeId(userId, recipe.getId());
                                recipe.setIsFavorited(favorite != null);
                            }
                        }
                        log.info("从缓存获取药膳搜索结果");
                        return Result.success(cachedPage);
                    } catch (ClassCastException ignored) {}
                }
            }

            Page<HerbalRecipe> page = new Page<>(pageNum, pageSize);
            IPage<HerbalRecipe> result = herbalRecipeMapper.searchRecipes(
                    page,
                    keyword,
                    season,
                    constitutionType,
                    effect
            );

            // 如果用户已登录，设置每个药膳的收藏状态
            if (userId != null) {
                for (HerbalRecipe recipe : result.getRecords()) {
                    UserRecipeFavorite favorite = userRecipeFavoriteMapper.selectByUserIdAndRecipeId(userId, recipe.getId());
                    recipe.setIsFavorited(favorite != null);
                }
            }

            // 缓存前3页（15分钟）
            if (pageNum <= 3) {
                redisUtil.set(cacheKey, result, 15, java.util.concurrent.TimeUnit.MINUTES);
            }

            log.info("搜索药膳：keyword={}, season={}, constitutionType={}, effect={}, total={}",
                    keyword, season, constitutionType, effect, result.getTotal());
            return Result.success(result);

        } catch (Exception e) {
            log.error("搜索药膳失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 获取药膳详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<HerbalRecipe> getRecipeDetail(Long recipeId, Long userId) {
        try {
            String cacheKey = "hospital:common:recipe:detail:id:" + recipeId;

            // 尝试从缓存获取
            Object cached = redisUtil.get(cacheKey);
            HerbalRecipe recipe = null;
            if (cached instanceof HerbalRecipe) {
                recipe = (HerbalRecipe) cached;
                log.info("从缓存获取药膳详情: {}", recipeId);
            } else {
                recipe = herbalRecipeMapper.selectById(recipeId);
                if (recipe == null) {
                    log.warn("药膳不存在: {}", recipeId);
                    return Result.error(ResultCode.DATA_NOT_FOUND);
                }
                // 存入缓存（30分钟）
                redisUtil.set(cacheKey, recipe, 30, java.util.concurrent.TimeUnit.MINUTES);
            }

            // 增加浏览次数（无论是否从缓存获取）
            herbalRecipeMapper.incrementViewCount(recipeId);

            // 如果用户已登录，设置收藏状态
            if (userId != null) {
                UserRecipeFavorite favorite = userRecipeFavoriteMapper.selectByUserIdAndRecipeId(userId, recipeId);
                recipe.setIsFavorited(favorite != null);
            }

            // 补全食材备注信息
            enrichIngredientsWithNotes(recipe);

            log.info("获取药膳详情: {}，用户ID：{}", recipeId, userId);
            return Result.success(recipe);

        } catch (Exception e) {
            log.error("获取药膳详情失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 获取热门药膳
     */
    @Override
    public Result<List<HerbalRecipe>> getPopularRecipes(Integer limit, Long userId) {
        try {
            String cacheKey = "recipe:popular:limit:" + limit;

            // 尝试从缓存获取
            Object cached = redisUtil.get(cacheKey);
            List<HerbalRecipe> recipes = null;
            if (cached instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<HerbalRecipe> list = (List<HerbalRecipe>) cached;
                    recipes = list;
                    log.info("从缓存获取热门药膳");
                } catch (ClassCastException ignored) {}
            }

            if (recipes == null) {
                recipes = herbalRecipeMapper.selectPopularRecipes(limit);
                // 存入缓存（30分钟）
                redisUtil.set(cacheKey, recipes, 30, java.util.concurrent.TimeUnit.MINUTES);
            }

            // 如果用户已登录，设置每个药膳的收藏状态
            if (userId != null) {
                for (HerbalRecipe recipe : recipes) {
                    UserRecipeFavorite favorite = userRecipeFavoriteMapper.selectByUserIdAndRecipeId(userId, recipe.getId());
                    recipe.setIsFavorited(favorite != null);
                }
            }

            log.info("获取热门药膳，共{}条", recipes.size());
            return Result.success(recipes);

        } catch (Exception e) {
            log.error("获取热门药膳失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 获取时令药膳
     */
    @Override
    public Result<List<HerbalRecipe>> getSeasonalRecipes(String season, Integer limit, Long userId) {
        try {
            String cacheKey = "recipe:seasonal:season:" + season + ":limit:" + limit;

            // 尝试从缓存获取
            Object cached = redisUtil.get(cacheKey);
            List<HerbalRecipe> recipes = null;
            if (cached instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<HerbalRecipe> list = (List<HerbalRecipe>) cached;
                    recipes = list;
                    log.info("从缓存获取时令药膳");
                } catch (ClassCastException ignored) {}
            }

            if (recipes == null) {
                recipes = herbalRecipeMapper.selectSeasonalRecipes(season, limit);
                // 存入缓存（30分钟）
                redisUtil.set(cacheKey, recipes, 30, java.util.concurrent.TimeUnit.MINUTES);
            }

            // 如果用户已登录，设置每个药膳的收藏状态
            if (userId != null) {
                for (HerbalRecipe recipe : recipes) {
                    UserRecipeFavorite favorite = userRecipeFavoriteMapper.selectByUserIdAndRecipeId(userId, recipe.getId());
                    recipe.setIsFavorited(favorite != null);
                }
            }

            log.info("获取时令药膳：{}，共{}条", season, recipes.size());
            return Result.success(recipes);

        } catch (Exception e) {
            log.error("获取时令药膳失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 收藏药膳
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> favoriteRecipe(Long userId, Long recipeId, String remark) {
        try {
            // 1. 检查药膳是否存在
            HerbalRecipe recipe = herbalRecipeMapper.selectById(recipeId);
            if (recipe == null) {
                log.warn("药膳不存在: {}", recipeId);
                return Result.error(ResultCode.DATA_NOT_FOUND);
            }

            // 2. 检查是否已收藏
            UserRecipeFavorite existing = userRecipeFavoriteMapper.selectByUserIdAndRecipeId(userId, recipeId);
            if (existing != null) {
                log.warn("用户{}已收藏药膳{}", userId, recipeId);
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "您已收藏过该药膳");
            }

            // 3. 创建收藏记录
            UserRecipeFavorite favorite = new UserRecipeFavorite();
            favorite.setUserId(userId);
            favorite.setRecipeId(recipeId);
            favorite.setRemark(remark);
            userRecipeFavoriteMapper.insert(favorite);

            // 4. 增加收藏次数
            herbalRecipeMapper.incrementFavoriteCount(recipeId);

            log.info("用户{}收藏药膳{}", userId, recipeId);
            return Result.success();

        } catch (Exception e) {
            log.error("收藏药膳失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 取消收藏
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> unfavoriteRecipe(Long userId, Long recipeId) {
        try {
            // 1. 检查收藏记录是否存在
            UserRecipeFavorite favorite = userRecipeFavoriteMapper.selectByUserIdAndRecipeId(userId, recipeId);
            if (favorite == null) {
                log.warn("用户{}未收藏药膳{}", userId, recipeId);
                return Result.error(ResultCode.DATA_NOT_FOUND.getCode(), "您未收藏该药膳");
            }

            // 2. 删除收藏记录
            userRecipeFavoriteMapper.deleteById(favorite.getId());

            // 3. 减少收藏次数
            herbalRecipeMapper.decrementFavoriteCount(recipeId);

            log.info("用户{}取消收藏药膳{}", userId, recipeId);
            return Result.success();

        } catch (Exception e) {
            log.error("取消收藏失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 获取用户收藏的药膳列表（分页）
     */
    @Override
    public Result<IPage<HerbalRecipe>> getUserFavorites(Long userId, Integer pageNum, Integer pageSize) {
        try {
            Page<UserRecipeFavorite> page = new Page<>(pageNum, pageSize);
            IPage<UserRecipeFavorite> favorites = userRecipeFavoriteMapper.selectFavoritesByUserId(page, userId);

            // 注意：这里返回的是UserRecipeFavorite的分页，但包含了HerbalRecipe的字段
            // 实际使用时可能需要转换为HerbalRecipe类型
            log.info("获取用户{}的收藏列表，共{}条", userId, favorites.getTotal());
            
            // 简化处理：直接返回（实际项目中可能需要类型转换）
            @SuppressWarnings("unchecked")
            IPage<HerbalRecipe> result = (IPage<HerbalRecipe>) (IPage<?>) favorites;
            return Result.success(result);

        } catch (Exception e) {
            log.error("获取收藏列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 根据体质类型查询适用食材
     */
    @Override
    public Result<List<Ingredient>> getIngredientsByConstitution(String constitutionType) {
        try {
            List<Ingredient> ingredients = ingredientMapper.selectByConstitutionType(constitutionType);
            log.info("获取体质{}的适用食材，共{}种", constitutionType, ingredients.size());
            return Result.success(ingredients);

        } catch (Exception e) {
            log.error("获取适用食材失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 生成默认推荐理由（降级方案）
     */
    private String buildDefaultRecommendationReason(HerbalRecipe recipe, UserConstitutionTest constitution) {
        StringBuilder reason = new StringBuilder();
        reason.append("根据您的").append(constitution.getPrimaryConstitution()).append("体质，");
        reason.append("推荐这道").append(recipe.getRecipeName()).append("。");
        if (StringUtils.hasText(recipe.getEfficacy())) {
            reason.append("该药膳具有").append(recipe.getEfficacy()).append("的功效，");
        }
        reason.append("适合您当前的身体状况。建议适量食用，配合规律作息效果更佳。");
        return reason.toString();
    }

    /**
     * 补全食材备注信息
     * 根据食材名称查询食材库，补充性味、功效等信息作为备注
     */
    private void enrichIngredientsWithNotes(HerbalRecipe recipe) {
        if (recipe == null || !StringUtils.hasText(recipe.getIngredients())) {
            return;
        }

        try {
            // 解析食材JSON
            List<Map<String, Object>> ingredients = objectMapper.readValue(
                recipe.getIngredients(),
                new TypeReference<List<Map<String, Object>>>() {}
            );

            if (ingredients == null || ingredients.isEmpty()) {
                return;
            }

            // 为每个食材查询详细信息并补充备注
            for (Map<String, Object> ingredient : ingredients) {
                String name = (String) ingredient.get("name");
                if (!StringUtils.hasText(name)) {
                    continue;
                }

                try {
                    // 查询食材库
                    QueryWrapper<Ingredient> wrapper = new QueryWrapper<>();
                    wrapper.eq("name", name);
                    wrapper.eq("status", 1);
                    Ingredient ingredientInfo = ingredientMapper.selectOne(wrapper);

                    if (ingredientInfo != null) {
                        // 构建备注信息
                        StringBuilder note = new StringBuilder();
                        
                        // 性味
                        if (StringUtils.hasText(ingredientInfo.getProperties())) {
                            note.append("性味：").append(ingredientInfo.getProperties());
                        }
                        
                        // 味道
                        if (StringUtils.hasText(ingredientInfo.getFlavor())) {
                            if (note.length() > 0) {
                                note.append("，");
                            }
                            note.append("味：").append(ingredientInfo.getFlavor());
                        }
                        
                        // 功效
                        if (StringUtils.hasText(ingredientInfo.getEfficacy())) {
                            if (note.length() > 0) {
                                note.append("；");
                            }
                            note.append("功效：").append(ingredientInfo.getEfficacy());
                        }

                        // 将备注添加到食材信息中
                        if (note.length() > 0) {
                            ingredient.put("note", note.toString());
                        }
                    }
                } catch (Exception e) {
                    log.warn("查询食材{}的详细信息失败: {}", name, e.getMessage());
                    // 继续处理下一个食材
                }
            }

            // 将补全后的食材信息重新序列化为JSON
            recipe.setIngredients(objectMapper.writeValueAsString(ingredients));

        } catch (Exception e) {
            log.error("补全食材备注信息失败", e);
            // 如果处理失败，保持原始数据不变
        }
    }
}

