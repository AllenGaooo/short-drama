package com.bunny.flix.interaction.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.interaction.entity.Like;
import com.bunny.flix.interaction.mq.InteractionEventProducer;
import com.bunny.flix.interaction.service.LikeService;
import com.bunny.flix.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 点赞控制器，提供点赞/取消点赞、点赞状态查询接口
 */
@RestController
@RequestMapping("/api/interaction/like")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;
    private final InteractionEventProducer interactionEventProducer;

    @PostMapping
    public Result<Void> like(@RequestBody Map<String, Long> body) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        Long dramaId = body.get("dramaId");
        likeService.like(userId, dramaId);
        interactionEventProducer.sendInteractionEvent("LIKE", userId, dramaId, null);
        return Result.success();
    }

    @DeleteMapping("/{dramaId}")
    public Result<Void> unlike(@PathVariable Long dramaId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        likeService.unlike(userId, dramaId);
        return Result.success();
    }

    @GetMapping("/check/{dramaId}")
    public Result<Boolean> checkLiked(@PathVariable Long dramaId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.success(false);
        }
        return Result.success(likeService.isLiked(userId, dramaId));
    }

    @GetMapping("/list")
    public Result<List<Like>> getUserLikes(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        return Result.success(likeService.getUserLikes(userId, page, size));
    }
}
