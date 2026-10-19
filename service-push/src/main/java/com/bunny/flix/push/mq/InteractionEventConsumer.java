package com.bunny.flix.push.mq;

import com.bunny.flix.push.service.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "interaction-event", consumerGroup = "push-interaction-event-group")
public class InteractionEventConsumer implements RocketMQListener<Map<String, Object>> {

    private final PushService pushService;

    @Override
    public void onMessage(Map<String, Object> message) {
        try {
            String eventType = (String) message.get("eventType");
            if ("comment_reply".equals(eventType)) {
                pushService.handleCommentReply(message);
            } else if ("comment_like".equals(eventType)) {
                pushService.handleCommentLike(message);
            }
            log.debug("处理互动事件: type={}", eventType);
        } catch (Exception e) {
            log.error("处理互动事件失败", e);
        }
    }
}
