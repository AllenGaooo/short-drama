package com.bunny.flix.video.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.video.entity.Video;
import com.bunny.flix.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 视频控制器
 * 提供视频上传、播放地址获取、多清晰度支持等接口
 */
@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @PostMapping("/upload")
    public Result<Video> upload(@RequestParam Long episodeId, @RequestParam String fileUrl) {
        Video video = videoService.uploadVideo(episodeId, fileUrl);
        return Result.success(video);
    }

    @GetMapping("/play/{episodeId}")
    public Result<Map<String, String>> getPlayUrl(
            @PathVariable Long episodeId,
            @RequestParam(required = false) Long userId) {
        String url = videoService.getVideoUrl(episodeId, userId);
        if (url == null) {
            return Result.error("视频不存在或尚未转码完成");
        }
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        result.put("episodeId", episodeId.toString());
        return Result.success(result);
    }

    @GetMapping("/{videoId}")
    public Result<Video> getVideoInfo(@PathVariable Long videoId) {
        Video video = videoService.getVideoInfo(videoId);
        if (video == null) {
            return Result.notFound("视频不存在");
        }
        return Result.success(video);
    }

    @GetMapping("/resolution/{episodeId}")
    public Result<Map<String, Object>> getResolution(@PathVariable Long episodeId) {
        String hlsUrl = videoService.getHlsUrl(episodeId, null);
        Map<String, Object> result = new HashMap<>();
        result.put("episodeId", episodeId);
        result.put("hlsUrl", hlsUrl);
        result.put("resolutions", new String[]{"360p", "480p", "720p", "1080p"});
        return Result.success(result);
    }
}
