package com.bunny.flix.interaction.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.interaction.entity.Danmaku;
import com.bunny.flix.interaction.service.DanmakuService;
import com.bunny.flix.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 弹幕控制器，提供发送弹幕、查询弹幕列表接口
 */
@RestController
@RequestMapping("/api/interaction/danmaku")
@RequiredArgsConstructor
public class DanmakuController {

    private final DanmakuService danmakuService;

    @PostMapping
    public Result<Danmaku> sendDanmaku(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        Long dramaId = body.get("dramaId") != null ? Long.valueOf(body.get("dramaId").toString()) : null;
        Long episodeId = body.get("episodeId") != null ? Long.valueOf(body.get("episodeId").toString()) : null;
        String content = (String) body.get("content");
        Double playTime = body.get("playTime") != null ? Double.valueOf(body.get("playTime").toString()) : 0.0;
        String color = (String) body.get("color");
        Integer font = body.get("font") != null ? Integer.valueOf(body.get("font").toString()) : null;

        Danmaku danmaku = danmakuService.sendDanmaku(userId, dramaId, episodeId, content, playTime, color, font);
        return Result.success(danmaku);
    }

    @GetMapping("/list")
    public Result<List<Danmaku>> getDanmakuList(
            @RequestParam("episodeId") Long episodeId,
            @RequestParam(value = "playTime", required = false) Double playTime) {
        return Result.success(danmakuService.getDanmakuList(episodeId, playTime));
    }
}
