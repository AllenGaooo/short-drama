package com.bunny.flix.recommend.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.recommend.service.RecommendService;
import com.bunny.flix.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @GetMapping
    public Result<List<Map<String, Object>>> getRecommendations(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = UserContext.getUserId();
        List<Map<String, Object>> recommendations = recommendService.getRecommendations(userId, page, size);
        return Result.success(recommendations);
    }

    @GetMapping("/hot")
    public Result<List<Map<String, Object>>> getHotRecommendations(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        List<Map<String, Object>> hotList = recommendService.calculateHotRecommendations(page, size);
        return Result.success(hotList);
    }
}
