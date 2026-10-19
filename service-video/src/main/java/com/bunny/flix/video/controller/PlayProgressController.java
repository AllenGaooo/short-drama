package com.bunny.flix.video.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.video.entity.PlayProgress;
import com.bunny.flix.video.service.PlayProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 播放进度控制器，保存和查询用户播放进度，支持断点续播
 */
@RestController
@RequestMapping("/api/video/progress")
@RequiredArgsConstructor
public class PlayProgressController {

    private final PlayProgressService playProgressService;

    @PostMapping
    public Result<Void> saveProgress(
            @RequestParam Long userId,
            @RequestParam Long episodeId,
            @RequestParam Long dramaId,
            @RequestParam Double position,
            @RequestParam Double duration) {
        playProgressService.saveProgress(userId, episodeId, dramaId, position, duration);
        return Result.success();
    }

    @GetMapping("/{episodeId}")
    public Result<PlayProgress> getProgress(
            @RequestParam Long userId,
            @PathVariable Long episodeId) {
        PlayProgress progress = playProgressService.getProgress(userId, episodeId);
        return Result.success(progress);
    }
}
