package com.bunny.flix.video.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.core.Result;
import com.bunny.flix.video.entity.WatchHistory;
import com.bunny.flix.video.service.WatchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 观看历史控制器，提供观看历史记录的查询和清除接口
 */
@RestController
@RequestMapping("/api/video/history")
@RequiredArgsConstructor
public class WatchHistoryController {

    private final WatchHistoryService watchHistoryService;

    @GetMapping("/list")
    public Result<Page<WatchHistory>> list(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(watchHistoryService.getHistory(userId, page, pageSize));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        watchHistoryService.deleteHistory(id);
        return Result.success();
    }
}
