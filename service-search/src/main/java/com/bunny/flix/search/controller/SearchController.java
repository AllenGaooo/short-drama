package com.bunny.flix.search.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.feign.search.dto.SearchResultDTO;
import com.bunny.flix.search.dto.SearchPageResult;
import com.bunny.flix.search.dto.SearchQuery;
import com.bunny.flix.search.entity.HotWord;
import com.bunny.flix.search.service.HotWordService;
import com.bunny.flix.search.service.SearchHistoryService;
import com.bunny.flix.search.service.SearchService;
import com.bunny.flix.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 搜索控制器，提供短剧全文搜索、搜索联想、热门搜索词接口
 */
@Tag(name = "搜索接口", description = "短剧全文搜索、搜索联想、热门搜索词")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final SearchHistoryService searchHistoryService;
    private final HotWordService hotWordService;

    @Operation(summary = "全文搜索", description = "按关键词搜索短剧，支持分页与排序（default/heat/newest/play）")
    @GetMapping
    public Result<List<?>> search(
            @Parameter(description = "搜索关键词", required = true) @RequestParam("keyword") String keyword,
            @Parameter(description = "页码，从1开始") @RequestParam(value = "page", defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(value = "size", defaultValue = "10") Integer size,
            @Parameter(description = "排序方式: default/heat/newest/play") @RequestParam(value = "sortBy", defaultValue = "default") String sortBy) {

        // Record search history
        Long userId = UserContext.getUserId();
        if (userId != null) {
            searchHistoryService.addHistory(userId, keyword);
        }

        return Result.success(searchService.search(keyword, page, size, sortBy));
    }

    @Operation(summary = "多条件搜索", description = "支持关键词全文检索 + 题材/标签/播放量筛选，返回总数与耗时")
    @PostMapping("/advanced")
    public Result<SearchPageResult> advancedSearch(@RequestBody SearchQuery query) {
        return Result.success(searchService.advancedSearch(query));
    }

    @Operation(summary = "热门排行榜", description = "热点数据，走逻辑过期缓存，永不断流")
    @GetMapping("/ranking")
    public Result<List<SearchResultDTO>> hotRanking(
            @Parameter(description = "返回条数") @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return Result.success(searchService.hotRanking(size));
    }

    @Operation(summary = "搜索性能指标", description = "平均耗时、慢查询数、缓存命中率，用于验证 150ms 目标")
    @GetMapping("/metrics")
    public Result<Map<String, Object>> metrics() {
        return Result.success(searchService.metrics());
    }

    @Operation(summary = "搜索联想", description = "根据输入前缀返回联想词")
    @GetMapping("/suggest")
    public Result<List<String>> suggest(
            @Parameter(description = "输入前缀", required = true) @RequestParam("keyword") String keyword) {
        return Result.success(searchService.suggest(keyword));
    }

    @Operation(summary = "热门搜索词", description = "返回运营配置的热搜词列表")
    @GetMapping("/hot")
    public Result<List<String>> getHotWords() {
        List<HotWord> hotWords = hotWordService.listHotWords();
        List<String> words = hotWords.stream()
                .map(HotWord::getWord)
                .collect(Collectors.toList());
        return Result.success(words);
    }

    @Operation(summary = "创建短剧索引", description = "手动触发创建 ES drama 索引（已存在则跳过）")
    @PostMapping("/index")
    public Result<Void> createIndex() {
        searchService.createDramaIndex();
        return Result.success();
    }
}
