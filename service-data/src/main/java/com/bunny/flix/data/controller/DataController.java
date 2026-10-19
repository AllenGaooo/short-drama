package com.bunny.flix.data.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.data.service.PlayStatService;
import com.bunny.flix.data.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataController {

    private final RankingService rankingService;
    private final PlayStatService playStatService;

    @GetMapping("/ranking/hot")
    public Result<List<Map<String, Object>>> getHotRanking(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(rankingService.getHotRanking(page, size));
    }

    @GetMapping("/ranking/new")
    public Result<List<Map<String, Object>>> getNewRanking(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(rankingService.getNewRanking(page, size));
    }

    @GetMapping("/ranking/category")
    public Result<List<Map<String, Object>>> getCategoryRanking(
            @RequestParam Long tagId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(rankingService.getCategoryRanking(tagId, page, size));
    }

    @GetMapping("/stats/{dramaId}")
    public Result<Map<String, Long>> getPlayStats(@PathVariable Long dramaId) {
        Map<String, Long> stats = Map.of(
                "day", playStatService.getPlayCount(dramaId, "day"),
                "week", playStatService.getPlayCount(dramaId, "week"),
                "month", playStatService.getPlayCount(dramaId, "month"),
                "total", playStatService.getPlayCount(dramaId, "total")
        );
        return Result.success(stats);
    }
}
