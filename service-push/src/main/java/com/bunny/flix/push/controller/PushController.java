package com.bunny.flix.push.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.dto.PageResponse;
import com.bunny.flix.push.entity.Notification;
import com.bunny.flix.push.service.PushService;
import com.bunny.flix.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushService pushService;

    // === 站内信 ===
    @GetMapping("/notification")
    public Result<PageResponse<Notification>> getNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(pushService.getUserNotifications(UserContext.getUserId(), page, size));
    }

    @GetMapping("/notification/unread-count")
    public Result<Long> getUnreadCount() {
        return Result.success(pushService.getUnreadCount(UserContext.getUserId()));
    }

    @PutMapping("/notification/read/{id}")
    public Result<Void> markAsRead(@PathVariable Long id) {
        pushService.markAsRead(id);
        return Result.success();
    }

    // === 管理端发送通知 ===
    @PostMapping("/send")
    public Result<Void> sendNotification(@RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") != null ? Long.valueOf(body.get("userId").toString()) : null;
        pushService.sendNotification(userId,
                (String) body.get("title"),
                (String) body.get("content"),
                (String) body.getOrDefault("type", "system"));
        return Result.success();
    }

    @PostMapping("/broadcast")
    public Result<Void> broadcastNotification(@RequestBody Map<String, String> body) {
        pushService.broadcastNotification(body.get("title"), body.get("content"));
        return Result.success();
    }
}
