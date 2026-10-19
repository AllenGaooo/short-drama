package com.bunny.flix.push.consumer;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bunny.flix.push.service.NotificationService;
import com.bunny.flix.push.service.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "interaction-event", consumerGroup = "push-interaction-group")
public class InteractionEventConsumer implements RocketMQListener<String> {

    private final NotificationService notificationService;
    private final PushService pushService;

    @Override
    public void onMessage(String message) {
        log.info("Received interaction event: {}", message);
        try {
            JSONObject json = JSONUtil.parseObj(message);
            String eventType = json.getStr("eventType");
            Long userId = json.getLong("userId");
            Long targetUserId = json.getLong("targetUserId");

            if (userId == null) return;

            switch (eventType) {
                case "comment_reply":
                    if (targetUserId != null) {
                        String title = "评论回复";
                        String content = json.getStr("content", "有人回复了你的评论");
                        notificationService.sendNotification(targetUserId, title, content, "interaction");
                    }
                    break;
                case "comment_like":
                    if (targetUserId != null) {
                        String title = "评论点赞";
                        String content = "有人赞了你的评论";
                        notificationService.sendNotification(targetUserId, title, content, "interaction");
                    }
                    break;
                case "drama_update":
                    String title = "短剧更新";
                    String content = json.getStr("content", "你关注的短剧有更新");
                    notificationService.sendNotification(userId, title, content, "update");
                    break;
                default:
                    log.info("Unknown interaction event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing interaction event: {}", message, e);
        }
    }
}
