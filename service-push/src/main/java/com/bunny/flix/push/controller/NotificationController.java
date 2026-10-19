package com.bunny.flix.push.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.core.Result;
import com.bunny.flix.push.entity.Notification;
import com.bunny.flix.push.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/push/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Result<Page<Notification>> list(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(notificationService.getUserNotifications(userId, page, size));
    }

    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@RequestParam Long userId) {
        return Result.success(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return Result.success();
    }
}
