package com.bunny.flix.search.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.search.entity.SearchHistory;
import com.bunny.flix.search.service.SearchHistoryService;
import com.bunny.flix.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 搜索历史控制器，提供用户搜索历史的查询和清除接口
 */
@Tag(name = "搜索历史", description = "用户搜索历史的记录、查询和清除")
@RestController
@RequestMapping("/api/search/history")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @Operation(summary = "记录搜索历史")
    @PostMapping
    public Result<Void> addHistory(
            @Parameter(description = "搜索关键词", required = true) @RequestParam("keyword") String keyword) {
        Long userId = UserContext.getUserId();
        if (userId != null) {
            searchHistoryService.addHistory(userId, keyword);
        }
        return Result.success();
    }

    @Operation(summary = "查询搜索历史", description = "查询当前登录用户的搜索历史")
    @GetMapping
    public Result<List<SearchHistory>> getHistory() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        return Result.success(searchHistoryService.getHistory(userId));
    }

    @Operation(summary = "清空搜索历史", description = "清空当前登录用户的搜索历史")
    @DeleteMapping
    public Result<Void> clearHistory() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        searchHistoryService.clearHistory(userId);
        return Result.success();
    }
}
