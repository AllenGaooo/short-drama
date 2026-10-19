package com.bunny.flix.search.service;

import com.bunny.flix.feign.drama.dto.DramaDTO;
import com.bunny.flix.feign.search.dto.SearchResultDTO;
import com.bunny.flix.search.dto.SearchPageResult;
import com.bunny.flix.search.dto.SearchQuery;

import java.util.List;
import java.util.Map;

public interface SearchService {

    /**
     * ES全文搜索（兼容原签名，内部走多条件查询 + 缓存）
     */
    List<SearchResultDTO> search(String keyword, Integer page, Integer size, String sortBy);

    /**
     * 多条件搜索：全文检索 + 精确筛选 + 混合排序 + 缓存
     */
    SearchPageResult advancedSearch(SearchQuery query);

    /**
     * 热门排行榜（走逻辑过期缓存，热点永不断流）
     */
    List<SearchResultDTO> hotRanking(Integer size);

    /**
     * 搜索联想
     */
    List<String> suggest(String keyword);

    /**
     * 同步短剧到ES，并失效相关缓存
     */
    void syncDramaToES(DramaDTO dramaDTO);

    /**
     * 删除ES索引
     */
    void deleteDramaFromES(Long dramaId);

    /**
     * 创建短剧ES索引（含 mapping），索引已存在则跳过
     */
    void createDramaIndex();

    /**
     * 搜索性能指标：平均耗时、慢查询数、缓存命中率
     */
    Map<String, Object> metrics();
}
