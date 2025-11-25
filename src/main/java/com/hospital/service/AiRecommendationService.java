package com.hospital.service;

import com.hospital.entity.HerbalRecipe;
import com.hospital.entity.UserConstitutionTest;

import java.util.List;

/**
 * AI推荐服务接口
 *
 * @author Hospital Team
 * @since 2025-01-XX
 */
public interface AiRecommendationService {

    /**
     * 生成个性化推荐理由
     *
     * @param recipe 药膳信息
     * @param constitution 用户体质测试结果
     * @return 推荐理由文本
     */
    String generateRecommendationReason(HerbalRecipe recipe, UserConstitutionTest constitution);

    /**
     * 基于对话内容推荐药膳
     *
     * @param conversationContent 对话内容
     * @param userId 用户ID
     * @return 推荐的药膳列表
     */
    List<HerbalRecipe> recommendByConversation(String conversationContent, Long userId);

    /**
     * 智能问答
     *
     * @param question 用户问题
     * @param userId 用户ID
     * @return AI回答
     */
    String answerQuestion(String question, Long userId);

    /**
     * 基于协同过滤的药膳推荐
     *
     * @param userId 用户ID
     * @param limit  推荐数量
     * @return 药膳列表
     */
    List<HerbalRecipe> recommendByCollaborativeFiltering(Long userId, int limit);

    /**
     * 基于内容画像的药膳推荐
     *
     * @param userId 用户ID
     * @param limit  推荐数量
     * @return 药膳列表
     */
    List<HerbalRecipe> recommendByContentPreference(Long userId, int limit);

    /**
     * 综合个性化推荐
     *
     * @param userId 用户ID
     * @param limit  推荐数量
     * @return 药膳列表
     */
    List<HerbalRecipe> recommendPersonalized(Long userId, int limit);
}

