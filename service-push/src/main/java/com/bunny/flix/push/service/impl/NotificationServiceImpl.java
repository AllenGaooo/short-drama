package com.bunny.flix.push.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.core.exception.BusinessException;
import com.bunny.flix.push.entity.Notification;
import com.bunny.flix.push.mapper.NotificationMapper;
import com.bunny.flix.push.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    @Transactional
    public Notification sendNotification(Long userId, String title, String content, String type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(0);
        notificationMapper.insert(notification);
        log.info("Sent notification to user {}: {}", userId, title);
        return notification;
    }

    @Override
    @Transactional
    public void broadcastNotification(String title, String content) {
        // Broadcast to all users: userId=0 represents global notification
        Notification notification = new Notification();
        notification.setUserId(0L);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType("system");
        notification.setIsRead(0);
        notificationMapper.insert(notification);
        log.info("Broadcast notification: {}", title);
    }

    @Override
    public Page<Notification> getUserNotifications(Long userId, Integer page, Integer size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Notification::getUserId, userId)
                        .or()
                        .eq(Notification::getUserId, 0))
               .orderByDesc(Notification::getCreateTime);
        return notificationMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Notification::getUserId, userId)
                        .or()
                        .eq(Notification::getUserId, 0))
               .eq(Notification::getIsRead, 0);
        return notificationMapper.selectCount(wrapper);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException(BusinessException.ResultCode.NOT_FOUND);
        }
        notification.setIsRead(1);
        notificationMapper.updateById(notification);
    }
}
