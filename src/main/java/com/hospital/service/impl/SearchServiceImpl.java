package com.hospital.service.impl;

import com.hospital.service.SearchService;
import com.hospital.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 搜索服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final RedisUtil redisUtil;
    
    private static final String HOT_KEYWORDS_KEY = "search:hot:keywords";
    private static final int KEYWORD_EXPIRE_DAYS = 30; // 关键词统计保留30天

    @Override
    public List<String> getHotKeywords(Integer limit) {
        try {
            // 使用 Redis Sorted Set 存储搜索词和搜索次数
            // key: search:hot:keywords, score: 搜索次数, member: 关键词
            Set<ZSetOperations.TypedTuple<Object>> tuples = redisUtil.zReverseRangeWithScores(
                    HOT_KEYWORDS_KEY, 0, limit - 1);
            
            if (tuples == null || tuples.isEmpty()) {
                log.info("暂无热门搜索词");
                return new ArrayList<>();
            }
            
            List<String> keywords = new ArrayList<>();
            for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
                Object value = tuple.getValue();
                if (value != null) {
                    keywords.add(String.valueOf(value));
                }
            }
            
            log.info("获取热门搜索词，共{}条", keywords.size());
            return keywords;
        } catch (Exception e) {
            log.error("获取热门搜索词失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public void recordSearchKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        
        try {
            // 使用 Redis Sorted Set 记录搜索次数
            // 每次搜索，对应关键词的分数（搜索次数）+1
            String trimmedKeyword = keyword.trim();
            redisUtil.zIncrementScore(HOT_KEYWORDS_KEY, trimmedKeyword, 1.0);
            
            // 设置过期时间（如果key不存在则设置，存在则不更新）
            redisUtil.expire(HOT_KEYWORDS_KEY, KEYWORD_EXPIRE_DAYS, TimeUnit.DAYS);
            
            log.debug("记录搜索关键词: {}", trimmedKeyword);
        } catch (Exception e) {
            log.warn("记录搜索关键词失败: keyword={}, error={}", keyword, e.getMessage());
        }
    }
}

