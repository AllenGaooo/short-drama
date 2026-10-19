package com.bunny.flix.push.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.dto.PageResponse;
import com.bunny.flix.push.entity.Notification;
import com.bunny.flix.push.entity.PushRecord;
import com.bunny.flix.push.mapper.NotificationMapper;
import com.bunny.flix.push.mapper.PushRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {

    private final NotificationMapper notificationMapper;
    private final PushRecordMapper pushRecordMapper;

    // === 站内信 ===
    public void sendNotification(Long userId, String title, String content, String type) {
        Notification notif = new Notification();
        notif.setUserId(userId != null ? userId : 0L);
        notif.setTitle(title);
        notif.setContent(content);
        notif.setType(type != null ? type : "system");
        notif.setIsRead(0);
        notificationMapper.insert(notif);
    }

    public void broadcastNotification(String title, String content) {
        sendNotification(0L, title, content, "system");
    }

    public PageResponse<Notification> getUserNotifications(Long userId, int page, int size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .or().eq(Notification::getUserId, 0L)
               .orderByDesc(Notification::getCreateTime);
        Page<Notification> result = notificationMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResponse.of(result.getRecords(), result.getTotal(), page, size);
    }

    public long getUnreadCount(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getIsRead, 0);
        return notificationMapper.selectCount(wrapper);
    }

    public void markAsRead(Long notificationId) {
        Notification notif = notificationMapper.selectById(notificationId);
        if (notif != null) {
            notif.setIsRead(1);
            notificationMapper.updateById(notif);
        }
    }

    // === App推送 ===
    public void pushToApp(Long userId, String title, String content) {
        log.info("App推送: userId={}, title={}, content={}", userId, title, content);
        // 实际对接极光/个推等第三方推送平台
        PushRecord record = new PushRecord();
        record.setUserId(userId);
        record.setTitle(title);
        record.setContent(content);
        record.setPushType("app");
        record.setPushTime(LocalDateTime.now());
        record.setResult("success");
        pushRecordMapper.insert(record);
    }

    // === 互动事件处理 ===
    public void handleCommentReply(Map<String, Object> event) {
        Long userId = Long.valueOf(event.get("replyToUserId").toString());
        String title = "评论被回复";
        String content = "有人回复了你的评论";
        sendNotification(userId, title, content, "interaction");
        pushToApp(userId, title, content);
    }

    public void handleCommentLike(Map<String, Object> event) {
        Long userId = Long.valueOf(event.get("userId").toString());
        String title = "评论被点赞";
        String content = "你的评论获得了新的点赞";
        sendNotification(userId, title, content, "interaction");
    }
}
