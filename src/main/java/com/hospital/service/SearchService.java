package com.hospital.service;

import java.util.List;

/**
 * 搜索服务接口
 */
public interface SearchService {

    /**
     * 获取热门搜索词
     * @param limit 返回数量限制
     * @return 热门搜索词列表
     */
    List<String> getHotKeywords(Integer limit);

    /**
     * 记录搜索关键词（用于统计）
     * @param keyword 搜索关键词
     */
    void recordSearchKeyword(String keyword);
}

