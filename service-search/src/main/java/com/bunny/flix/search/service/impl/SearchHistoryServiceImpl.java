package com.bunny.flix.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bunny.flix.search.entity.SearchHistory;
import com.bunny.flix.search.mapper.SearchHistoryMapper;
import com.bunny.flix.search.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private final SearchHistoryMapper searchHistoryMapper;

    @Override
    @Transactional
    public void addHistory(Long userId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }

        // Remove duplicate keyword for this user
        LambdaQueryWrapper<SearchHistory> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SearchHistory::getUserId, userId)
                .eq(SearchHistory::getKeyword, keyword.trim());
        searchHistoryMapper.delete(deleteWrapper);

        SearchHistory history = new SearchHistory();
        history.setUserId(userId);
        history.setKeyword(keyword.trim());
        history.setCreateTime(LocalDateTime.now());
        searchHistoryMapper.insert(history);

        // Keep only latest 50 records
        LambdaQueryWrapper<SearchHistory> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(SearchHistory::getUserId, userId)
                .orderByDesc(SearchHistory::getCreateTime);
        List<SearchHistory> allHistory = searchHistoryMapper.selectList(countWrapper);
        if (allHistory.size() > 50) {
            List<SearchHistory> toDelete = allHistory.subList(50, allHistory.size());
            toDelete.forEach(h -> searchHistoryMapper.deleteById(h.getId()));
        }

        log.info("添加搜索历史: userId={}, keyword={}", userId, keyword);
    }

    @Override
    public List<SearchHistory> getHistory(Long userId) {
        LambdaQueryWrapper<SearchHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SearchHistory::getUserId, userId)
                .orderByDesc(SearchHistory::getCreateTime)
                .last("LIMIT 50");
        return searchHistoryMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void clearHistory(Long userId) {
        LambdaQueryWrapper<SearchHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SearchHistory::getUserId, userId);
        searchHistoryMapper.delete(wrapper);
        log.info("清空搜索历史: userId={}", userId);
    }
}
