package com.bunny.flix.video.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayEventProducer {

    private final RocketMQTemplate rocketMQTemplate;

    public void sendPlayEvent(Long userId, Long episodeId, Long dramaId) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId);
        event.put("episodeId", episodeId);
        event.put("dramaId", dramaId);
        event.put("timestamp", System.currentTimeMillis());

        rocketMQTemplate.convertAndSend("play-event", event);
        log.debug("发送播放事件：userId={}, episodeId={}, dramaId={}", userId, episodeId, dramaId);
    }
}
