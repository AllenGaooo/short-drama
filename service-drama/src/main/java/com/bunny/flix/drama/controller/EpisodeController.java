package com.bunny.flix.drama.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.drama.entity.Episode;
import com.bunny.flix.drama.service.EpisodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 剧集控制器，提供剧集的增删改查接口
 */
@RestController
@RequestMapping("/api/drama/episode")
@RequiredArgsConstructor
public class EpisodeController {

    private final EpisodeService episodeService;

    @GetMapping("/list/{dramaId}")
    public Result<List<Episode>> list(@PathVariable Long dramaId) {
        return Result.success(episodeService.listEpisodes(dramaId));
    }

    @GetMapping("/{id}")
    public Result<Episode> getById(@PathVariable Long id) {
        Episode episode = episodeService.getEpisode(id);
        if (episode == null) {
            return Result.notFound("剧集不存在");
        }
        return Result.success(episode);
    }

    @PostMapping
    public Result<Episode> create(@RequestBody Episode episode) {
        return Result.success(episodeService.createEpisode(episode));
    }

    @PutMapping("/{id}")
    public Result<Episode> update(@PathVariable Long id, @RequestBody Episode episode) {
        episode.setId(id);
        return Result.success(episodeService.updateEpisode(episode));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        episodeService.deleteEpisode(id);
        return Result.success();
    }

    @PutMapping("/{id}/sort")
    public Result<Void> updateSort(@PathVariable Long id, @RequestParam Integer sortOrder) {
        episodeService.updateSort(id, sortOrder);
        return Result.success();
    }

    @PutMapping("/{id}/free")
    public Result<Void> updateFreeStatus(@PathVariable Long id, @RequestParam Integer free) {
        episodeService.updateFreeStatus(id, free);
        return Result.success();
    }
}
