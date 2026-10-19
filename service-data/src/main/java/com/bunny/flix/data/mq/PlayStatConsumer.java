package com.bunny.flix.data.mq;

import com.bunny.flix.data.service.PlayStatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "play-event", consumerGroup = "data-play-stat-group")
public class PlayStatConsumer implements RocketMQListener<Map<String, Object>> {

    private final PlayStatService playStatService;

    @Override
    public void onMessage(Map<String, Object> message) {
        try {
            Long dramaId = Long.valueOf(message.get("dramaId").toString());
            playStatService.incrementPlayCount(dramaId);
            log.debug("播放统计更新: dramaId={}", dramaId);
        } catch (Exception e) {
            log.error("处理播放统计消息失败", e);
        }
    }
}
