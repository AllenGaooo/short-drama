package com.bunny.flix.search.service;

import com.bunny.flix.search.entity.SearchHistory;

import java.util.List;

public interface SearchHistoryService {

    /**
     * 添加搜索历史
     */
    void addHistory(Long userId, String keyword);

    /**
     * 获取用户搜索历史
     */
    List<SearchHistory> getHistory(Long userId);

    /**
     * 清空搜索历史
     */
    void clearHistory(Long userId);
}
