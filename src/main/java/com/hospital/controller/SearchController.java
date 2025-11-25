package com.hospital.controller;

import com.hospital.annotation.RateLimit;
import com.hospital.common.result.Result;
import com.hospital.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 搜索相关接口
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * 获取热门搜索词
     * @param limit 返回数量限制，默认10
     * @return 热门搜索词列表
     */
    @GetMapping("/hot-keywords")
    public Result<List<String>> getHotKeywords(@RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取热门搜索词，limit={}", limit);
        List<String> keywords = searchService.getHotKeywords(limit);
        return Result.success(keywords);
    }

    /**
     * 记录搜索关键词（用于统计热门搜索词）
     * @param keyword 搜索关键词
     */
    @RateLimit(key = "search-record", limit = 30, windowSeconds = 60, perIp = true, perUser = true)
    @PostMapping("/record")
    public Result<Void> recordSearchKeyword(@RequestParam String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            searchService.recordSearchKeyword(keyword.trim());
        }
        return Result.success();
    }
}

