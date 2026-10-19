package com.bunny.flix.push.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.push.entity.Notification;

public interface NotificationService {

    Notification sendNotification(Long userId, String title, String content, String type);

    void broadcastNotification(String title, String content);

    Page<Notification> getUserNotifications(Long userId, Integer page, Integer size);

    Long getUnreadCount(Long userId);

    void markAsRead(Long notificationId);
}
