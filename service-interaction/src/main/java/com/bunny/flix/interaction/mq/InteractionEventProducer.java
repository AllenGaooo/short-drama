package com.bunny.flix.interaction.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InteractionEventProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送互动事件到推送服务
     */
    public void sendInteractionEvent(String eventType, Long userId, Long dramaId, Long targetUserId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("userId", userId);
        event.put("dramaId", dramaId);
        event.put("targetUserId", targetUserId);
        event.put("timestamp", System.currentTimeMillis());

        rocketMQTemplate.convertAndSend("interaction-event", event);
        log.info("发送互动事件: eventType={}, userId={}, dramaId={}", eventType, userId, dramaId);
    }
}
