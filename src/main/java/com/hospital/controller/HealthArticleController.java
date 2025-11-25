package com.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.common.result.Result;
import com.hospital.entity.HealthArticle;
import com.hospital.service.HealthArticleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 养生文章控制器
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Slf4j
@RestController
@RequestMapping("/api/article")
public class HealthArticleController {

    @Autowired
    private HealthArticleService articleService;

    /**
     * 分页查询文章列表
     *
     * @param category 分类（可选）
     * @param constitutionType 体质类型（可选）
     * @param isFeatured 是否精选（可选）
     * @param pageNum 页码（默认1）
     * @param pageSize 每页数量（默认10）
     * @return 文章列表
     */
    @GetMapping("/list")
    public Result<IPage<HealthArticle>> getArticleList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String constitutionType,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) Integer isFeatured,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        log.info("查询文章列表：分类={}，体质={}，标签={}，精选={}，页码={}，每页={}条",
                category, constitutionType, tags, isFeatured, pageNum, pageSize);
        return articleService.getArticleList(category, constitutionType, tags, isFeatured, pageNum, pageSize);
    }

    /**
     * 获取文章详情
     *
     * @param id 文章ID
     * @return 文章详情
     */
    @GetMapping("/{id}")
    public Result<HealthArticle> getArticleDetail(@PathVariable Long id) {
        log.info("查询文章详情：id={}", id);
        return articleService.getArticleDetail(id);
    }

    /**
     * 搜索文章
     *
     * @param keyword 关键词
     * @param pageNum 页码（默认1）
     * @param pageSize 每页数量（默认10）
     * @return 文章列表
     */
    @GetMapping("/search")
    public Result<IPage<HealthArticle>> searchArticles(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        log.info("搜索文章：关键词={}，页码={}，每页={}条", keyword, pageNum, pageSize);
        return articleService.searchArticles(keyword, pageNum, pageSize);
    }

    /**
     * 发布文章
     *
     * @param article 文章信息
     * @return 发布结果
     */
    @PostMapping("/publish")
    public Result<HealthArticle> publishArticle(@RequestBody HealthArticle article) {
        log.info("发布文章：标题={}，作者ID={}", article.getTitle(), article.getAuthorId());
        return articleService.publishArticle(article);
    }

    /**
     * 更新文章
     *
     * @param id 文章ID
     * @param article 文章信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Result<HealthArticle> updateArticle(@PathVariable Long id, @RequestBody HealthArticle article) {
        article.setId(id);
        log.info("更新文章：id={}，标题={}", id, article.getTitle());
        return articleService.updateArticle(article);
    }

    /**
     * 删除文章
     *
     * @param id 文章ID
     * @param userId 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteArticle(@PathVariable Long id, @RequestParam Long userId) {
        log.info("删除文章：id={}，用户ID={}", id, userId);
        return articleService.deleteArticle(id, userId);
    }

    /**
     * 点赞文章
     *
     * @param id 文章ID
     * @param userId 用户ID
     * @return 点赞结果
     */
    @PostMapping("/like/{id}")
    public Result<Void> likeArticle(@PathVariable Long id, @RequestParam Long userId) {
        log.info("点赞文章：文章ID={}，用户ID={}", id, userId);
        return articleService.likeArticle(id, userId);
    }

    /**
     * 取消点赞文章
     *
     * @param id 文章ID
     * @param userId 用户ID
     * @return 取消点赞结果
     */
    @DeleteMapping("/like/{id}")
    public Result<Void> unlikeArticle(@PathVariable Long id, @RequestParam Long userId) {
        log.info("取消点赞文章：文章ID={}，用户ID={}", id, userId);
        return articleService.unlikeArticle(id, userId);
    }

    /**
     * 收藏文章
     *
     * @param id 文章ID
     * @param userId 用户ID
     * @param remark 备注（可选）
     * @return 收藏结果
     */
    @PostMapping("/favorite/{id}")
    public Result<Void> favoriteArticle(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam(required = false) String remark) {
        
        log.info("收藏文章：文章ID={}，用户ID={}", id, userId);
        return articleService.favoriteArticle(id, userId, remark);
    }

    /**
     * 取消收藏文章
     *
     * @param id 文章ID
     * @param userId 用户ID
     * @return 取消收藏结果
     */
    @DeleteMapping("/favorite/{id}")
    public Result<Void> unfavoriteArticle(@PathVariable Long id, @RequestParam Long userId) {
        log.info("取消收藏文章：文章ID={}，用户ID={}", id, userId);
        return articleService.unfavoriteArticle(id, userId);
    }

    /**
     * 获取用户收藏的文章
     *
     * @param userId 用户ID
     * @param pageNum 页码（默认1）
     * @param pageSize 每页数量（默认10）
     * @return 收藏列表
     */
    @GetMapping("/favorites")
    public Result<IPage<HealthArticle>> getFavoriteArticles(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        log.info("查询用户收藏文章：用户ID={}，页码={}，每页={}条", userId, pageNum, pageSize);
        return articleService.getFavoriteArticles(userId, pageNum, pageSize);
    }

    /**
     * 获取用户发布的文章
     *
     * @param userId 用户ID
     * @param pageNum 页码（默认1）
     * @param pageSize 每页数量（默认10）
     * @return 文章列表
     */
    @GetMapping("/my")
    public Result<IPage<HealthArticle>> getMyArticles(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        log.info("查询用户发布文章：用户ID={}，页码={}，每页={}条", userId, pageNum, pageSize);
        return articleService.getMyArticles(userId, pageNum, pageSize);
    }

    /**
     * 获取精选文章
     *
     * @param limit 数量限制（默认10）
     * @return 文章列表
     */
    @GetMapping("/recommended")
    public Result<List<HealthArticle>> getRecommendedArticles(@RequestParam(defaultValue = "5") Integer limit) {
        log.info("查询推荐文章：limit={}", limit);
        return articleService.getRecommendedArticles(limit);
    }

    /**
     * 获取热门文章
     *
     * @param limit 数量限制（默认10）
     * @return 文章列表
     */
    @GetMapping("/popular")
    public Result<List<HealthArticle>> getPopularArticles(@RequestParam(defaultValue = "10") Integer limit) {
        log.info("查询热门文章：limit={}", limit);
        return articleService.getPopularArticles(limit);
    }

    /**
     * 检查用户是否已点赞文章
     *
     * @param id 文章ID
     * @param userId 用户ID
     * @return 是否已点赞
     */
    @GetMapping("/like/status/{id}")
    public Result<Boolean> checkLikeStatus(@PathVariable Long id, @RequestParam Long userId) {
        log.info("检查点赞状态：文章ID={}，用户ID={}", id, userId);
        return articleService.checkLikeStatus(id, userId);
    }

    /**
     * 检查用户是否已收藏文章
     *
     * @param id 文章ID
     * @param userId 用户ID
     * @return 是否已收藏
     */
    @GetMapping("/favorite/status/{id}")
    public Result<Boolean> checkFavoriteStatus(@PathVariable Long id, @RequestParam Long userId) {
        log.info("检查收藏状态：文章ID={}，用户ID={}", id, userId);
        return articleService.checkFavoriteStatus(id, userId);
    }
}

