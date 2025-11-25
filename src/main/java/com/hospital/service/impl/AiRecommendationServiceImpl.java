package com.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hospital.config.DeepSeekConfig;
import com.hospital.entity.HerbalRecipe;
import com.hospital.entity.UserConstitutionTest;
import com.hospital.entity.UserRecipeFavorite;
import com.hospital.mapper.HerbalRecipeMapper;
import com.hospital.mapper.UserConstitutionTestMapper;
import com.hospital.mapper.UserRecipeFavoriteMapper;
import com.hospital.service.AiRecommendationService;
import com.hospital.util.RedisUtil;
import com.hospital.common.constant.CacheConstants;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeUnit;

/**
 * AI推荐服务实现类
 * 使用OpenAI Java SDK（兼容DeepSeek API）
 *
 * @author Hospital Team
 * @since 2025-01-XX
 */
@Slf4j
@Service
public class AiRecommendationServiceImpl implements AiRecommendationService {

    @Autowired
    private DeepSeekConfig deepSeekConfig;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private HerbalRecipeMapper herbalRecipeMapper;

    @Autowired
    private UserRecipeFavoriteMapper userRecipeFavoriteMapper;

    @Autowired
    private UserConstitutionTestMapper userConstitutionTestMapper;

    private OpenAiService openAiService;

    private static final int DEFAULT_RECOMMENDATION_LIMIT = 6;
    private static final int CONTENT_CANDIDATE_LIMIT = 200;

    @PostConstruct
    public void init() {
        if (StringUtils.hasText(deepSeekConfig.getApiKey())) {
            try {
                // 创建拦截器，添加 Authorization 头
                Interceptor authInterceptor = new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .header("Authorization", "Bearer " + deepSeekConfig.getApiKey())
                                .header("Content-Type", "application/json")
                                .build();
                        return chain.proceed(request);
                    }
                };

                // 创建 OkHttpClient，添加认证拦截器
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(java.time.Duration.ofSeconds(10))
                        .readTimeout(java.time.Duration.ofSeconds(30))
                        .writeTimeout(java.time.Duration.ofSeconds(30))
                        .addInterceptor(authInterceptor)
                        .build();

                // 创建自定义 ObjectMapper，配置忽略未知属性（DeepSeek API 可能返回 SDK 不支持的字段）
                ObjectMapper retrofitObjectMapper = new ObjectMapper();
                retrofitObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                retrofitObjectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);

                // 创建 Retrofit 实例，使用 DeepSeek 的 API 地址
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(deepSeekConfig.getApiUrl() + "/")
                        .client(client)
                        .addConverterFactory(JacksonConverterFactory.create(retrofitObjectMapper))
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .build();

                // 创建 OpenAiApi 实例
                OpenAiApi api = retrofit.create(OpenAiApi.class);

                // 创建 OpenAiService 实例
                openAiService = new OpenAiService(api);

                log.info("DeepSeek API服务初始化成功，API地址: {}", deepSeekConfig.getApiUrl());
            } catch (Exception e) {
                log.error("初始化DeepSeek API服务失败", e);
            }
        } else {
            log.warn("DeepSeek API Key未配置，AI推荐功能将不可用");
        }
    }

    @PreDestroy
    public void destroy() {
        if (openAiService != null) {
            try {
                openAiService.shutdownExecutor();
            } catch (Exception e) {
                log.warn("关闭OpenAI服务时出错", e);
            }
        }
    }

    @Override
    public String generateRecommendationReason(HerbalRecipe recipe, UserConstitutionTest constitution) {
        if (recipe == null || constitution == null) {
            log.warn("生成推荐理由失败：参数为空");
            return null;
        }

        if (openAiService == null) {
            log.warn("DeepSeek API服务未初始化，返回默认推荐理由");
            return buildDefaultRecommendationReason(recipe, constitution);
        }

        // 生成缓存键
        String cacheKey = CacheConstants.AI_RECOMMENDATION_REASON_CACHE_PREFIX +
                generateCacheKey(recipe.getId().toString(), constitution.getPrimaryConstitution());

        // 尝试从缓存获取
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof String) {
            log.debug("从缓存获取推荐理由：recipeId={}", recipe.getId());
            return (String) cached;
        }

        try {
            // 构建Prompt
            String prompt = buildRecommendationReasonPrompt(recipe, constitution);

            // 调用DeepSeek API
            String response = callDeepSeekApi(prompt);

            if (StringUtils.hasText(response)) {
                // 缓存结果
                redisUtil.set(cacheKey, response,
                        deepSeekConfig.getCacheTtlHours(), TimeUnit.HOURS);
                log.info("生成推荐理由成功：recipeId={}", recipe.getId());
                return response;
            }

        } catch (Exception e) {
            log.error("生成推荐理由失败：recipeId={}", recipe.getId(), e);
        }

        // 降级：返回默认推荐理由
        return buildDefaultRecommendationReason(recipe, constitution);
    }

    @Override
    public List<HerbalRecipe> recommendByConversation(String conversationContent, Long userId) {
        if (!StringUtils.hasText(conversationContent)) {
            log.warn("对话内容为空，无法推荐");
            return Collections.emptyList();
        }

        if (openAiService == null) {
            log.warn("DeepSeek API服务未初始化，返回空列表");
            return Collections.emptyList();
        }

        // 生成缓存键
        String cacheKey = CacheConstants.AI_CONVERSATION_CACHE_PREFIX +
                generateCacheKey(conversationContent, userId != null ? userId.toString() : "anonymous");

        // 尝试从缓存获取
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<HerbalRecipe> cachedList = (List<HerbalRecipe>) cached;
                log.debug("从缓存获取对话推荐结果");
                return cachedList;
            } catch (ClassCastException ignored) {}
        }

        try {
            // 构建Prompt
            String prompt = buildConversationRecommendationPrompt(conversationContent);

            // 调用DeepSeek API
            String response = callDeepSeekApi(prompt);

            if (StringUtils.hasText(response)) {
                // 解析推荐结果
                List<HerbalRecipe> recommendations = parseRecommendationResponse(response);

                if (!recommendations.isEmpty()) {
                    // 缓存结果
                    redisUtil.set(cacheKey, recommendations,
                            deepSeekConfig.getCacheTtlHours(), TimeUnit.HOURS);
                    log.info("对话推荐成功：userId={}, count={}", userId, recommendations.size());
                    return recommendations;
                }
            }

        } catch (Exception e) {
            log.error("对话推荐失败：userId={}", userId, e);
        }

        // 降级：返回空列表
        return Collections.emptyList();
    }

    @Override
    public String answerQuestion(String question, Long userId) {
        if (!StringUtils.hasText(question)) {
            return "您的问题不能为空，请重新提问。";
        }

        if (openAiService == null) {
            log.warn("DeepSeek API服务未初始化，返回默认回答");
            return "抱歉，AI服务暂时不可用，请稍后再试或咨询专业医生。";
        }

        // 生成缓存键
        String cacheKey = CacheConstants.AI_QUESTION_CACHE_PREFIX + generateCacheKey(question,
                userId != null ? userId.toString() : "anonymous");

        // 尝试从缓存获取
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof String) {
            log.debug("从缓存获取问答结果");
            return (String) cached;
        }

        try {
            // 构建Prompt
            String prompt = buildQuestionAnswerPrompt(question);

            // 调用DeepSeek API
            String response = callDeepSeekApi(prompt);

            if (StringUtils.hasText(response)) {
                // 缓存结果
                redisUtil.set(cacheKey, response,
                        deepSeekConfig.getCacheTtlHours(), TimeUnit.HOURS);
                log.info("智能问答成功：userId={}", userId);
                return response;
            }

        } catch (Exception e) {
            log.error("智能问答失败：userId={}", userId, e);
        }

        // 降级：返回默认回答
        return "抱歉，我现在无法回答您的问题，请稍后再试或咨询专业医生。";
    }

    @Override
    public List<HerbalRecipe> recommendByCollaborativeFiltering(Long userId, int limit) {
        limit = normalizeLimit(limit);
        if (userId == null) {
            log.warn("协同过滤推荐失败：用户未登录");
            return Collections.emptyList();
        }

        String cacheKey = CacheConstants.AI_CF_RECOMMEND_CACHE_PREFIX + userId + ":limit:" + limit;
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<HerbalRecipe> cachedList = (List<HerbalRecipe>) cached;
                return cachedList;
            } catch (ClassCastException ignored) {}
        }

        List<UserRecipeFavorite> allFavorites = userRecipeFavoriteMapper.selectList(null);
        Map<Long, Set<Long>> userFavoriteMap = buildUserFavoriteMap(allFavorites);
        Set<Long> targetFavorites = userFavoriteMap.getOrDefault(userId, Collections.emptySet());

        if (targetFavorites.isEmpty()) {
            log.info("用户{}无收藏记录，协同过滤降级为热门推荐", userId);
            List<HerbalRecipe> fallback = herbalRecipeMapper.selectPopularRecipes(limit);
            applyFavoriteFlag(fallback, userId);
            redisUtil.set(cacheKey, fallback, CacheConstants.AI_RECOMMENDATION_TTL_SECONDS, TimeUnit.SECONDS);
            return fallback;
        }

        Map<Long, Double> scoreMap = new HashMap<>();

        userFavoriteMap.forEach((otherUserId, otherFavorites) -> {
            if (otherUserId.equals(userId) || otherFavorites.isEmpty()) {
                return;
            }
            double similarity = computeSimilarity(targetFavorites, otherFavorites);
            if (similarity <= 0) {
                return;
            }
            for (Long recipeId : otherFavorites) {
                if (targetFavorites.contains(recipeId)) {
                    continue;
                }
                scoreMap.merge(recipeId, similarity, Double::sum);
            }
        });

        if (scoreMap.isEmpty()) {
            log.info("协同过滤得分为空，降级为热门推荐");
            List<HerbalRecipe> fallback = herbalRecipeMapper.selectPopularRecipes(limit);
            applyFavoriteFlag(fallback, userId);
            redisUtil.set(cacheKey, fallback, CacheConstants.AI_RECOMMENDATION_TTL_SECONDS, TimeUnit.SECONDS);
            return fallback;
        }

        List<Long> recommendIds = scoreMap.entrySet()
                .stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .limit(limit)
                .collect(Collectors.toList());

        List<HerbalRecipe> recipes = fetchActiveRecipesByIds(recommendIds);
        setCollaborativeReason(recipes, userFavoriteMap, userId);
        applyFavoriteFlag(recipes, userId);
        redisUtil.set(cacheKey, recipes, CacheConstants.AI_RECOMMENDATION_TTL_SECONDS, TimeUnit.SECONDS);
        return recipes;
    }

    @Override
    public List<HerbalRecipe> recommendByContentPreference(Long userId, int limit) {
        limit = normalizeLimit(limit);
        if (userId == null) {
            log.warn("内容推荐失败：用户未登录");
            return Collections.emptyList();
        }

        String cacheKey = CacheConstants.AI_CONTENT_RECOMMEND_CACHE_PREFIX + userId + ":limit:" + limit;
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<HerbalRecipe> cachedList = (List<HerbalRecipe>) cached;
                return cachedList;
            } catch (ClassCastException ignored) {}
        }

        UserPreferenceProfile profile = buildUserPreferenceProfile(userId);
        List<HerbalRecipe> candidates = herbalRecipeMapper.selectActiveRecipesForRecommendation(CONTENT_CANDIDATE_LIMIT);

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        String currentSeason = resolveCurrentSeason();
        Map<Long, Double> scored = new HashMap<>();
        for (HerbalRecipe recipe : candidates) {
            if (profile.favoriteRecipeIds.contains(recipe.getId())) {
                continue;
            }
            double score = computeContentScore(recipe, profile, currentSeason);
            if (score > 0) {
                scored.put(recipe.getId(), score);
            }
        }

        if (scored.isEmpty()) {
            log.info("内容推荐得分为空，返回热门数据");
            List<HerbalRecipe> fallback = herbalRecipeMapper.selectPopularRecipes(limit);
            applyFavoriteFlag(fallback, userId);
            redisUtil.set(cacheKey, fallback, CacheConstants.AI_RECOMMENDATION_TTL_SECONDS, TimeUnit.SECONDS);
            return fallback;
        }

        List<Long> recommendIds = scored.entrySet()
                .stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .limit(limit)
                .collect(Collectors.toList());

        List<HerbalRecipe> recipes = fetchActiveRecipesByIds(recommendIds);
        setContentReason(recipes, profile, currentSeason);
        applyFavoriteFlag(recipes, userId);
        redisUtil.set(cacheKey, recipes, CacheConstants.AI_RECOMMENDATION_TTL_SECONDS, TimeUnit.SECONDS);
        return recipes;
    }

    @Override
    public List<HerbalRecipe> recommendPersonalized(Long userId, int limit) {
        limit = normalizeLimit(limit);
        if (userId == null) {
            log.warn("个性化推荐失败：用户未登录");
            return Collections.emptyList();
        }

        String cacheKey = CacheConstants.AI_PERSONALIZED_RECOMMEND_CACHE_PREFIX + userId + ":limit:" + limit;
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<HerbalRecipe> cachedList = (List<HerbalRecipe>) cached;
                return cachedList;
            } catch (ClassCastException ignored) {}
        }

        List<HerbalRecipe> cfList = recommendByCollaborativeFiltering(userId, limit * 2);
        List<HerbalRecipe> contentList = recommendByContentPreference(userId, limit * 2);

        LinkedHashMap<Long, HerbalRecipe> merged = new LinkedHashMap<>();
        mergeRecommendations(merged, cfList);
        mergeRecommendations(merged, contentList);

        if (merged.size() < limit) {
            List<HerbalRecipe> fallback = herbalRecipeMapper.selectPopularRecipes(limit * 2);
            applyFavoriteFlag(fallback, userId);
            fallback.forEach(recipe -> recipe.setRecommendationReason(
                    recipe.getRecommendationReason() != null ? recipe.getRecommendationReason() : "根据综合热度推荐"));
            mergeRecommendations(merged, fallback);
        }

        List<HerbalRecipe> result = merged.values()
                .stream()
                .limit(limit)
                .collect(Collectors.toList());

        redisUtil.set(cacheKey, result, CacheConstants.AI_RECOMMENDATION_TTL_SECONDS, TimeUnit.SECONDS);
        return result;
    }

    /**
     * 调用DeepSeek API（使用OpenAI SDK）
     */
    private String callDeepSeekApi(String prompt) {
        try {
            // 构建消息列表
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                    "你是一位专业的中医健康顾问，擅长根据用户体质和症状推荐合适的药膳和养生建议。请始终使用中文回答，不要使用英文或其他语言。"));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));

            // 构建请求
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(deepSeekConfig.getModel())
                    .messages(messages)
                    .maxTokens(deepSeekConfig.getMaxTokens())
                    .temperature(deepSeekConfig.getTemperature())
                    .build();

            // 调用API
            String response = openAiService.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

            // 清理响应，确保只返回中文内容
            if (response != null) {
                response = cleanChineseResponse(response.trim());
            }

            return response;

        } catch (Exception e) {
            log.error("调用DeepSeek API失败", e);
            throw new RuntimeException("API调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建推荐理由Prompt
     */
    private String buildRecommendationReasonPrompt(HerbalRecipe recipe, UserConstitutionTest constitution) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("根据以下信息，生成一段个性化的药膳推荐理由（100-150字，仅使用中文）：\n\n");
        prompt.append("【用户体质】").append(constitution.getPrimaryConstitution());
        if (StringUtils.hasText(constitution.getSecondaryConstitution())) {
            prompt.append("（次要体质：").append(constitution.getSecondaryConstitution()).append("）");
        }
        prompt.append("\n\n");
        prompt.append("【药膳信息】\n");
        prompt.append("名称：").append(recipe.getRecipeName()).append("\n");
        if (StringUtils.hasText(recipe.getEfficacy())) {
            prompt.append("功效：").append(recipe.getEfficacy()).append("\n");
        }
        if (StringUtils.hasText(recipe.getSuitableSymptoms())) {
            prompt.append("适用症状：").append(recipe.getSuitableSymptoms()).append("\n");
        }
        prompt.append("\n请用专业、温暖的中文语言说明为什么这道药膳适合该用户，并给出食用建议。不要使用英文、拼音或任何非中文字符。");
        return prompt.toString();
    }

    /**
     * 构建对话推荐Prompt
     */
    private String buildConversationRecommendationPrompt(String conversationContent) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("根据以下用户对话内容，推荐3-5道适合的药膳，只返回中文药膳名称列表（每行一个，仅使用中文）：\n\n");
        prompt.append("【对话内容】\n").append(conversationContent).append("\n\n");
        prompt.append("请基于中医理论，推荐适合的药膳。只返回中文药膳名称，不要使用英文或其他语言。");
        return prompt.toString();
    }

    /**
     * 构建问答Prompt
     */
    private String buildQuestionAnswerPrompt(String question) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请作为专业的中医健康顾问，回答以下问题。回答要专业、准确、易懂（200字以内，仅使用中文）：\n\n");
        prompt.append("【问题】\n").append(question);
        prompt.append("\n\n请用中文回答，不要使用英文或其他语言。");
        return prompt.toString();
    }

    /**
     * 解析推荐响应（简化实现）
     */
    private List<HerbalRecipe> parseRecommendationResponse(String response) {
        List<HerbalRecipe> recipes = new ArrayList<>();
        if (!StringUtils.hasText(response)) {
            return recipes;
        }

        // 按行分割，提取药膳名称
        String[] lines = response.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("【")) {
                continue;
            }

            // 移除序号和特殊字符
            line = line.replaceAll("^\\d+[.、]\\s*", "").trim();

            // 搜索匹配的药膳
            List<HerbalRecipe> found = herbalRecipeMapper.selectList(
                    new QueryWrapper<HerbalRecipe>()
                            .like("recipe_name", line)
                            .eq("status", 1)
                            .last("LIMIT 1")
            );

            if (!found.isEmpty()) {
                recipes.add(found.get(0));
            }

            // 限制最多返回5个
            if (recipes.size() >= 5) {
                break;
            }
        }

        return recipes;
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

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_RECOMMENDATION_LIMIT;
        }
        return Math.min(limit, 20);
    }

    private Map<Long, Set<Long>> buildUserFavoriteMap(List<UserRecipeFavorite> favorites) {
        Map<Long, Set<Long>> map = new HashMap<>();
        for (UserRecipeFavorite favorite : favorites) {
            if (favorite.getUserId() == null || favorite.getRecipeId() == null) {
                continue;
            }
            map.computeIfAbsent(favorite.getUserId(), k -> new HashSet<>())
                    .add(favorite.getRecipeId());
        }
        return map;
    }

    private double computeSimilarity(Set<Long> target, Set<Long> other) {
        if (target.isEmpty() || other.isEmpty()) {
            return 0;
        }
        int intersection = 0;
        for (Long id : target) {
            if (other.contains(id)) {
                intersection++;
            }
        }
        if (intersection == 0) {
            return 0;
        }
        int union = target.size() + other.size() - intersection;
        return union == 0 ? 0 : (double) intersection / union;
    }

    private List<HerbalRecipe> fetchActiveRecipesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<HerbalRecipe> recipeList = herbalRecipeMapper.selectActiveRecipesByIds(ids);
        Map<Long, HerbalRecipe> recipeMap = new HashMap<>();
        for (HerbalRecipe recipe : recipeList) {
            recipeMap.put(recipe.getId(), recipe);
        }
        List<HerbalRecipe> ordered = new ArrayList<>();
        for (Long id : ids) {
            HerbalRecipe recipe = recipeMap.get(id);
            if (recipe != null) {
                ordered.add(recipe);
            }
        }
        return ordered;
    }

    private void setCollaborativeReason(List<HerbalRecipe> recipes, Map<Long, Set<Long>> userFavoriteMap, Long userId) {
        if (recipes == null || recipes.isEmpty()) {
            return;
        }
        for (HerbalRecipe recipe : recipes) {
            recipe.setRecommendationReason("基于与您口味相近用户的收藏偏好，为您推荐此药膳。");
        }
    }

    private UserPreferenceProfile buildUserPreferenceProfile(Long userId) {
        UserPreferenceProfile profile = new UserPreferenceProfile();
        if (userId == null) {
            return profile;
        }

        UserConstitutionTest latestTest = userConstitutionTestMapper.selectLatestByUserId(userId);
        if (latestTest != null) {
            profile.primaryConstitution = latestTest.getPrimaryConstitution();
            profile.secondaryConstitution = latestTest.getSecondaryConstitution();
        }

        List<Long> favoriteIds = userRecipeFavoriteMapper.selectRecipeIdsByUserId(userId);
        if (favoriteIds != null) {
            profile.favoriteRecipeIds.addAll(favoriteIds);
        }

        if (!profile.favoriteRecipeIds.isEmpty()) {
            List<HerbalRecipe> favoriteRecipes = herbalRecipeMapper.selectActiveRecipesByIds(new ArrayList<>(profile.favoriteRecipeIds));
            for (HerbalRecipe recipe : favoriteRecipes) {
                accumulatePreference(profile.preferredCategories, recipe.getCategory());
                accumulatePreference(profile.preferredSeasons, recipe.getSeason());
                accumulatePreference(profile.preferredEffects, recipe.getEfficacy());
            }
        }

        return profile;
    }

    private void accumulatePreference(Map<String, Integer> counter, String source) {
        if (!StringUtils.hasText(source)) {
            return;
        }
        for (String token : splitToTokens(source)) {
            if (!token.isEmpty()) {
                counter.merge(token, 1, Integer::sum);
            }
        }
    }

    private List<String> splitToTokens(String source) {
        if (!StringUtils.hasText(source)) {
            return Collections.emptyList();
        }
        String normalized = source.replace("、", ",")
                .replace("，", ",")
                .replace("/", ",");
        String[] parts = normalized.split(",");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                tokens.add(trimmed.toUpperCase());
            }
        }
        return tokens;
    }

    private double computeContentScore(HerbalRecipe recipe, UserPreferenceProfile profile, String currentSeason) {
        if (recipe == null) {
            return 0;
        }
        double score = 0;

        if (StringUtils.hasText(profile.primaryConstitution) &&
                containsIgnoreCase(recipe.getConstitutionType(), profile.primaryConstitution)) {
            score += 3.0;
        }
        if (StringUtils.hasText(profile.secondaryConstitution) &&
                containsIgnoreCase(recipe.getConstitutionType(), profile.secondaryConstitution)) {
            score += 1.5;
        }

        score += preferenceScore(profile.preferredCategories, recipe.getCategory(), 1.2);
        score += preferenceScore(profile.preferredSeasons, recipe.getSeason(), 0.8);

        if (StringUtils.hasText(recipe.getSeason()) &&
                recipe.getSeason().toUpperCase().contains(currentSeason)) {
            score += 0.5;
        }

        for (String effect : splitToTokens(recipe.getEfficacy())) {
            score += profile.preferredEffects.getOrDefault(effect, 0) * 0.3;
        }

        if (recipe.getFavoriteCount() != null) {
            score += Math.min(recipe.getFavoriteCount(), 200) * 0.01;
        }
        if (recipe.getViewCount() != null) {
            score += Math.log(recipe.getViewCount() + 1) * 0.2;
        }

        return score;
    }

    private double preferenceScore(Map<String, Integer> preference, String value, double weight) {
        if (!StringUtils.hasText(value) || preference.isEmpty()) {
            return 0;
        }
        double score = 0;
        for (String token : splitToTokens(value)) {
            int count = preference.getOrDefault(token, 0);
            if (count > 0) {
                score += weight * count;
            }
        }
        return score;
    }

    private boolean containsIgnoreCase(String source, String target) {
        if (!StringUtils.hasText(source) || !StringUtils.hasText(target)) {
            return false;
        }
        return source.toUpperCase().contains(target.toUpperCase());
    }

    private String resolveCurrentSeason() {
        java.time.Month month = java.time.LocalDate.now().getMonth();
        switch (month) {
            case DECEMBER:
            case JANUARY:
            case FEBRUARY:
                return "WINTER";
            case MARCH:
            case APRIL:
            case MAY:
                return "SPRING";
            case JUNE:
            case JULY:
            case AUGUST:
                return "SUMMER";
            default:
                return "AUTUMN";
        }
    }

    private void applyFavoriteFlag(List<HerbalRecipe> recipes, Long userId) {
        if (recipes == null || recipes.isEmpty() || userId == null) {
            return;
        }
        List<Long> favoriteIds = userRecipeFavoriteMapper.selectRecipeIdsByUserId(userId);
        Set<Long> favoriteSet = favoriteIds == null ? Collections.emptySet() : new HashSet<>(favoriteIds);
        for (HerbalRecipe recipe : recipes) {
            recipe.setIsFavorited(favoriteSet.contains(recipe.getId()));
        }
    }

    private void setContentReason(List<HerbalRecipe> recipes, UserPreferenceProfile profile, String currentSeason) {
        if (recipes == null) {
            return;
        }
        for (HerbalRecipe recipe : recipes) {
            StringBuilder reason = new StringBuilder("结合您的体质与历史偏好推荐：");
            if (StringUtils.hasText(profile.primaryConstitution) &&
                    containsIgnoreCase(recipe.getConstitutionType(), profile.primaryConstitution)) {
                reason.append("适合").append(profile.primaryConstitution).append("体质，");
            }
            if (StringUtils.hasText(recipe.getSeason()) &&
                    recipe.getSeason().toUpperCase().contains(currentSeason)) {
                reason.append("当前季节食用更佳，");
            }
            if (StringUtils.hasText(recipe.getEfficacy())) {
                reason.append("功效：").append(recipe.getEfficacy()).append("。");
            } else {
                reason.append("帮助更好地平衡身体。");
            }
            recipe.setRecommendationReason(reason.toString());
        }
    }

    private void mergeRecommendations(Map<Long, HerbalRecipe> container, List<HerbalRecipe> candidates) {
        if (candidates == null) {
            return;
        }
        for (HerbalRecipe recipe : candidates) {
            if (recipe == null || recipe.getId() == null) {
                continue;
            }
            container.putIfAbsent(recipe.getId(), recipe);
        }
    }

    private static class UserPreferenceProfile {
        private String primaryConstitution;
        private String secondaryConstitution;
        private final Map<String, Integer> preferredCategories = new HashMap<>();
        private final Map<String, Integer> preferredEffects = new HashMap<>();
        private final Map<String, Integer> preferredSeasons = new HashMap<>();
        private final Set<Long> favoriteRecipeIds = new HashSet<>();
    }

    /**
     * 清理响应内容，确保主要使用中文
     * 移除明显的英文段落，但保留中文内容中的标点和数字
     */
    private String cleanChineseResponse(String response) {
        if (!StringUtils.hasText(response)) {
            return response;
        }
        
        // 如果内容主要是中文（中文字符占比超过70%），直接返回
        long chineseCharCount = response.codePoints()
                .filter(cp -> (cp >= 0x4E00 && cp <= 0x9FFF) || 
                              (cp >= 0x3400 && cp <= 0x4DBF) || 
                              (cp >= 0x20000 && cp <= 0x2A6DF))
                .count();
        double chineseRatio = (double) chineseCharCount / response.length();
        
        if (chineseRatio >= 0.7) {
            // 内容主要是中文，只做轻微清理
            response = response.replaceAll("\\s+", " "); // 合并多个空格
            return response.trim();
        }
        
        // 如果英文内容较多，提取中文部分
        // 按句分割，保留包含中文的句子
        String[] sentences = response.split("[。！？\n]");
        StringBuilder cleaned = new StringBuilder();
        for (String sentence : sentences) {
            if (sentence.matches(".*[\\u4e00-\\u9fa5].*")) {
                // 包含中文字符的句子
                cleaned.append(sentence.trim()).append("。");
            }
        }
        
        String result = cleaned.toString().trim();
        // 如果清理后为空，返回原内容（避免丢失所有信息）
        return result.isEmpty() ? response : result;
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(String... parts) {
        try {
            String combined = String.join(":", parts);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("生成缓存键失败", e);
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}

