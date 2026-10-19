package com.bunny.flix.interaction.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.interaction.entity.Collect;
import com.bunny.flix.interaction.mq.InteractionEventProducer;
import com.bunny.flix.interaction.service.CollectService;
import com.bunny.flix.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interaction/collect")
@RequiredArgsConstructor
public class CollectController {

    private final CollectService collectService;
    private final InteractionEventProducer interactionEventProducer;

    @PostMapping
    public Result<Void> collect(@RequestBody Map<String, Long> body) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        Long dramaId = body.get("dramaId");
        collectService.collect(userId, dramaId);
        interactionEventProducer.sendInteractionEvent("COLLECT", userId, dramaId, null);
        return Result.success();
    }

    @DeleteMapping("/{dramaId}")
    public Result<Void> uncollect(@PathVariable Long dramaId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        collectService.uncollect(userId, dramaId);
        return Result.success();
    }

    @GetMapping("/check/{dramaId}")
    public Result<Boolean> checkCollected(@PathVariable Long dramaId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.success(false);
        }
        return Result.success(collectService.isCollected(userId, dramaId));
    }

    @GetMapping("/list")
    public Result<List<Collect>> getUserCollects(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        return Result.success(collectService.getUserCollects(userId, page, size));
    }
}
