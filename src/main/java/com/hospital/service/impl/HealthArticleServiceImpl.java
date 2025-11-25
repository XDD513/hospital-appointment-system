package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.common.result.Result;
import com.hospital.common.result.ResultCode;
import com.hospital.entity.HealthArticle;
import com.hospital.entity.UserArticleFavorite;
import com.hospital.entity.UserLike;
import com.hospital.entity.User;
import com.hospital.mapper.HealthArticleMapper;
import com.hospital.mapper.UserArticleFavoriteMapper;
import com.hospital.mapper.UserLikeMapper;
import com.hospital.mapper.UserMapper;
import com.hospital.service.HealthArticleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 养生文章服务实现类
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Slf4j
@Service
public class HealthArticleServiceImpl implements HealthArticleService {

    @Autowired
    private HealthArticleMapper articleMapper;

    @Autowired
    private UserLikeMapper userLikeMapper;

    @Autowired
    private UserArticleFavoriteMapper userArticleFavoriteMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private com.hospital.util.RedisUtil redisUtil;

    /**
     * 分页查询文章列表
     */
    @Override
    public Result<IPage<HealthArticle>> getArticleList(String category, String constitutionType, String tags, Integer isFeatured, Integer pageNum, Integer pageSize) {
        try {
            // 只缓存前3页，使用参数哈希简化层级
            Map<String, Object> filterParams = new java.util.HashMap<>();
            if (category != null && !"all".equals(category)) {
                filterParams.put("cat", category);
            }
            if (constitutionType != null && !"all".equals(constitutionType)) {
                filterParams.put("type", constitutionType);
            }
            if (tags != null && !tags.trim().isEmpty()) {
                filterParams.put("tags", tags);
            }
            if (isFeatured != null && !"all".equals(String.valueOf(isFeatured))) {
                filterParams.put("featured", isFeatured);
            }
            String cacheKey = redisUtil.buildCacheKey("hospital:common:article:list", pageNum, pageSize, filterParams);

            if (pageNum <= 3) {
                Object cached = redisUtil.get(cacheKey);
                if (cached instanceof IPage) {
                    try {
                        @SuppressWarnings("unchecked")
                        IPage<HealthArticle> cachedPage = (IPage<HealthArticle>) cached;
                        log.info("从缓存获取文章列表");
                        return Result.success(cachedPage);
                    } catch (ClassCastException ignored) {}
                }
            }

            Page<HealthArticle> page = new Page<>(pageNum, pageSize);
            
            // 处理标签参数：将逗号分隔的字符串转换为List
            List<String> tagList = null;
            if (tags != null && !tags.trim().isEmpty()) {
                tagList = new java.util.ArrayList<>();
                String[] tagArray = tags.split(",");
                for (String tag : tagArray) {
                    String trimmedTag = tag.trim();
                    if (!trimmedTag.isEmpty()) {
                        tagList.add(trimmedTag);
                    }
                }
            }
            
            IPage<HealthArticle> result = articleMapper.selectArticlePage(page, category, constitutionType, tagList, isFeatured);

            // 缓存前3页（15分钟）
            if (pageNum <= 3) {
                redisUtil.set(cacheKey, result, 15, java.util.concurrent.TimeUnit.MINUTES);
            }

            log.info("查询文章列表：分类={}，体质={}，标签={}，精选={}，共{}条", category, constitutionType, tags, isFeatured, result.getTotal());
            return Result.success(result);

        } catch (Exception e) {
            log.error("查询文章列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 获取文章详情
     */
    @Override
    public Result<HealthArticle> getArticleDetail(Long id) {
        try {
            String cacheKey = "hospital:common:article:detail:id:" + id;

            // 尝试从缓存获取
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof HealthArticle) {
                HealthArticle article = (HealthArticle) cached;
                // 即使从缓存获取，也要增加浏览次数
                articleMapper.incrementViewCount(id);
                log.info("从缓存获取文章详情：id={}", id);
                return Result.success(article);
            }

            HealthArticle article = articleMapper.selectById(id);
            if (article == null) {
                return Result.error(ResultCode.DATA_NOT_FOUND.getCode(), "文章不存在");
            }

            // 查询作者信息并设置作者姓名
            if (article.getAuthorId() != null) {
                User author = userMapper.selectById(article.getAuthorId());
                if (author != null) {
                    article.setAuthorName(author.getRealName() != null ? author.getRealName() : author.getUsername());
                }
            }

            // 增加浏览次数
            articleMapper.incrementViewCount(id);

            // 存入缓存（30分钟）
            redisUtil.set(cacheKey, article, 30, java.util.concurrent.TimeUnit.MINUTES);

            log.info("查询文章详情：id={}，标题={}", id, article.getTitle());
            return Result.success(article);

        } catch (Exception e) {
            log.error("查询文章详情失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 搜索文章
     */
    @Override
    public Result<IPage<HealthArticle>> searchArticles(String keyword, Integer pageNum, Integer pageSize) {
        try {
            // 只缓存前3页，关键词作为特殊参数处理（保留可读性）
            Map<String, Object> filterParams = new java.util.HashMap<>();
            if (keyword != null && !keyword.isEmpty()) {
                filterParams.put("keyword", keyword);
            }
            String cacheKey = redisUtil.buildCacheKey("hospital:common:article:search", pageNum, pageSize, filterParams);

            if (pageNum <= 3) {
                Object cached = redisUtil.get(cacheKey);
                if (cached instanceof IPage) {
                    try {
                        @SuppressWarnings("unchecked")
                        IPage<HealthArticle> cachedPage = (IPage<HealthArticle>) cached;
                        log.info("从缓存获取文章搜索结果");
                        return Result.success(cachedPage);
                    } catch (ClassCastException ignored) {}
                }
            }

            Page<HealthArticle> page = new Page<>(pageNum, pageSize);
            IPage<HealthArticle> result = articleMapper.searchArticles(page, keyword);

            // 缓存前3页（15分钟）
            if (pageNum <= 3) {
                redisUtil.set(cacheKey, result, 15, java.util.concurrent.TimeUnit.MINUTES);
            }

            log.info("搜索文章：关键词={}，共{}条", keyword, result.getTotal());
            return Result.success(result);

        } catch (Exception e) {
            log.error("搜索文章失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 发布文章
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<HealthArticle> publishArticle(HealthArticle article) {
        try {
            // 设置初始值
            article.setViewCount(0);
            article.setLikeCount(0);
            article.setFavoriteCount(0);
            article.setCommentCount(0);
            article.setStatus(1); // 已发布
            article.setPublishTime(LocalDateTime.now());

            articleMapper.insert(article);

            // 查询作者信息并设置作者姓名（用于返回给前端）
            if (article.getAuthorId() != null) {
                User author = userMapper.selectById(article.getAuthorId());
                if (author != null) {
                    article.setAuthorName(author.getRealName() != null ? author.getRealName() : author.getUsername());
                }
            }

            // 失效文章列表缓存
            redisUtil.deleteByPattern("hospital:common:article:list:*");
            redisUtil.deleteByPattern("hospital:common:article:search:*");

            log.info("发布文章成功：id={}，标题={}", article.getId(), article.getTitle());
            return Result.success(article);

        } catch (Exception e) {
            log.error("发布文章失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 更新文章
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<HealthArticle> updateArticle(HealthArticle article) {
        try {
            HealthArticle existingArticle = articleMapper.selectById(article.getId());
            if (existingArticle == null) {
                return Result.error(ResultCode.DATA_NOT_FOUND.getCode(), "文章不存在");
            }

            // 只允许作者更新自己的文章
            if (!existingArticle.getAuthorId().equals(article.getAuthorId())) {
                return Result.error(ResultCode.FORBIDDEN.getCode(), "无权限更新此文章");
            }

            articleMapper.updateById(article);

            // 失效相关缓存
            redisUtil.delete("hospital:common:article:detail:id:" + article.getId());
            redisUtil.deleteByPattern("hospital:common:article:list:*");
            redisUtil.deleteByPattern("hospital:common:article:search:*");

            log.info("更新文章成功：id={}，标题={}", article.getId(), article.getTitle());
            return Result.success(article);

        } catch (Exception e) {
            log.error("更新文章失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 删除文章
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteArticle(Long id, Long userId) {
        try {
            HealthArticle article = articleMapper.selectById(id);
            if (article == null) {
                return Result.error(ResultCode.DATA_NOT_FOUND.getCode(), "文章不存在");
            }

            // 只允许作者删除自己的文章
            if (!article.getAuthorId().equals(userId)) {
                return Result.error(ResultCode.FORBIDDEN.getCode(), "无权限删除此文章");
            }

            // 软删除：更新状态为已下架
            article.setStatus(2);
            articleMapper.updateById(article);

            // 失效相关缓存
            redisUtil.delete("hospital:common:article:detail:id:" + id);
            redisUtil.deleteByPattern("hospital:common:article:list:*");
            redisUtil.deleteByPattern("hospital:common:article:search:*");

            log.info("删除文章成功：id={}，标题={}", id, article.getTitle());
            return Result.success();

        } catch (Exception e) {
            log.error("删除文章失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 点赞文章
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> likeArticle(Long articleId, Long userId) {
        try {
            // 检查是否已点赞
            UserLike existingLike = userLikeMapper.selectByUserAndTarget(userId, articleId, "ARTICLE");
            if (existingLike != null) {
                return Result.error(ResultCode.OPERATION_FAILED.getCode(), "已点赞过该文章");
            }

            // 创建点赞记录
            UserLike userLike = new UserLike();
            userLike.setUserId(userId);
            userLike.setTargetId(articleId);
            userLike.setTargetType("ARTICLE");
            userLikeMapper.insert(userLike);

            // 增加文章点赞数
            articleMapper.incrementLikeCount(articleId);

            log.info("点赞文章成功：文章ID={}，用户ID={}", articleId, userId);
            return Result.success();

        } catch (Exception e) {
            log.error("点赞文章失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 取消点赞文章
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> unlikeArticle(Long articleId, Long userId) {
        try {
            // 检查是否已点赞
            UserLike existingLike = userLikeMapper.selectByUserAndTarget(userId, articleId, "ARTICLE");
            if (existingLike == null) {
                return Result.error(ResultCode.OPERATION_FAILED.getCode(), "未点赞过该文章");
            }

            // 删除点赞记录
            userLikeMapper.deleteByUserAndTarget(userId, articleId, "ARTICLE");

            // 减少文章点赞数
            articleMapper.decrementLikeCount(articleId);

            log.info("取消点赞文章成功：文章ID={}，用户ID={}", articleId, userId);
            return Result.success();

        } catch (Exception e) {
            log.error("取消点赞文章失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 收藏文章
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> favoriteArticle(Long articleId, Long userId, String remark) {
        try {
            // 检查是否已收藏
            UserArticleFavorite existingFavorite = userArticleFavoriteMapper.selectByUserAndArticle(userId, articleId);
            if (existingFavorite != null) {
                return Result.error(ResultCode.OPERATION_FAILED.getCode(), "已收藏过该文章");
            }

            // 创建收藏记录
            UserArticleFavorite favorite = new UserArticleFavorite();
            favorite.setUserId(userId);
            favorite.setArticleId(articleId);
            favorite.setRemark(remark);
            userArticleFavoriteMapper.insert(favorite);

            // 增加文章收藏数
            articleMapper.incrementFavoriteCount(articleId);

            log.info("收藏文章成功：文章ID={}，用户ID={}", articleId, userId);
            return Result.success();

        } catch (Exception e) {
            log.error("收藏文章失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 取消收藏文章
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> unfavoriteArticle(Long articleId, Long userId) {
        try {
            // 检查是否已收藏
            UserArticleFavorite existingFavorite = userArticleFavoriteMapper.selectByUserAndArticle(userId, articleId);
            if (existingFavorite == null) {
                return Result.error(ResultCode.OPERATION_FAILED.getCode(), "未收藏过该文章");
            }

            // 删除收藏记录
            userArticleFavoriteMapper.deleteByUserAndArticle(userId, articleId);

            // 减少文章收藏数
            articleMapper.decrementFavoriteCount(articleId);

            log.info("取消收藏文章成功：文章ID={}，用户ID={}", articleId, userId);
            return Result.success();

        } catch (Exception e) {
            log.error("取消收藏文章失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 获取用户收藏的文章
     */
    @Override
    public Result<IPage<HealthArticle>> getFavoriteArticles(Long userId, Integer pageNum, Integer pageSize) {
        try {
            Page<UserArticleFavorite> page = new Page<>(pageNum, pageSize);
            IPage<UserArticleFavorite> favorites = userArticleFavoriteMapper.selectFavoritesByUserId(page, userId);

            // 转换为文章列表（这里简化处理，实际应该创建DTO）
            log.info("查询用户收藏文章：用户ID={}，共{}条", userId, favorites.getTotal());
            
            // 注意：这里需要创建一个新的IPage<HealthArticle>对象
            // 实际项目中应该创建DTO或使用更优雅的方式处理
            return Result.success((IPage) favorites);

        } catch (Exception e) {
            log.error("查询用户收藏文章失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 获取用户发布的文章
     */
    @Override
    public Result<IPage<HealthArticle>> getMyArticles(Long userId, Integer pageNum, Integer pageSize) {
        try {
            Page<HealthArticle> page = new Page<>(pageNum, pageSize);
            IPage<HealthArticle> result = articleMapper.selectByAuthorId(page, userId);
            log.info("查询用户发布文章：用户ID={}，共{}条", userId, result.getTotal());
            return Result.success(result);

        } catch (Exception e) {
            log.error("查询用户发布文章失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 获取精选文章
     */
    @Override
    public Result<List<HealthArticle>> getRecommendedArticles(Integer limit) {
        try {
            List<HealthArticle> articles = articleMapper.selectRecommendedArticles(limit);
            log.info("查询精选文章：共{}篇", articles.size());
            return Result.success(articles);

        } catch (Exception e) {
            log.error("查询精选文章失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 获取热门文章
     */
    @Override
    public Result<List<HealthArticle>> getPopularArticles(Integer limit) {
        try {
            List<HealthArticle> articles = articleMapper.selectPopularArticles(limit);
            log.info("查询热门文章：共{}篇", articles.size());
            return Result.success(articles);

        } catch (Exception e) {
            log.error("查询热门文章失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 检查用户是否已点赞文章
     */
    @Override
    public Result<Boolean> checkLikeStatus(Long articleId, Long userId) {
        try {
            UserLike existingLike = userLikeMapper.selectByUserAndTarget(userId, articleId, "ARTICLE");
            boolean isLiked = existingLike != null;
            log.info("检查点赞状态：文章ID={}，用户ID={}，已点赞={}", articleId, userId, isLiked);
            return Result.success(isLiked);

        } catch (Exception e) {
            log.error("检查点赞状态失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 检查用户是否已收藏文章
     */
    @Override
    public Result<Boolean> checkFavoriteStatus(Long articleId, Long userId) {
        try {
            UserArticleFavorite existingFavorite = userArticleFavoriteMapper.selectByUserAndArticle(userId, articleId);
            boolean isFavorited = existingFavorite != null;
            log.info("检查收藏状态：文章ID={}，用户ID={}，已收藏={}", articleId, userId, isFavorited);
            return Result.success(isFavorited);

        } catch (Exception e) {
            log.error("检查收藏状态失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 获取所有标签列表（用于筛选）
     */
    @Override
    public Result<List<String>> getAllTags() {
        try {
            String cacheKey = "hospital:common:article:tags:all";
            
            // 尝试从缓存获取
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<String> tags = (List<String>) cached;
                    log.info("从缓存获取所有标签，共{}个", tags.size());
                    return Result.success(tags);
                } catch (ClassCastException ignored) {}
            }
            
            // 从数据库查询所有不重复的标签
            List<HealthArticle> articles = articleMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HealthArticle>()
                            .select(HealthArticle::getTags)
                            .eq(HealthArticle::getStatus, 1)
                            .isNotNull(HealthArticle::getTags)
                            .ne(HealthArticle::getTags, "")
            );
            
            // 提取所有标签并去重
            java.util.Set<String> tagSet = new java.util.HashSet<>();
            for (HealthArticle article : articles) {
                if (article.getTags() != null && !article.getTags().trim().isEmpty()) {
                    String[] tags = article.getTags().split(",");
                    for (String tag : tags) {
                        String trimmedTag = tag.trim();
                        if (!trimmedTag.isEmpty()) {
                            tagSet.add(trimmedTag);
                        }
                    }
                }
            }
            
            List<String> tagList = new java.util.ArrayList<>(tagSet);
            java.util.Collections.sort(tagList);
            
            // 存入缓存（1小时）
            redisUtil.set(cacheKey, tagList, 1, java.util.concurrent.TimeUnit.HOURS);
            
            log.info("获取所有标签，共{}个", tagList.size());
            return Result.success(tagList);
            
        } catch (Exception e) {
            log.error("获取所有标签失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
    }
}

