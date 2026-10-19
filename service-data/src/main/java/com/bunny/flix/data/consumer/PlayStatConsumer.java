package com.bunny.flix.data.consumer;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bunny.flix.data.service.PlayStatService;
import com.bunny.flix.data.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "play-event", consumerGroup = "data-play-stat-group")
public class PlayStatConsumer implements RocketMQListener<String> {

    private final PlayStatService playStatService;
    private final UserProfileService userProfileService;

    @Override
    public void onMessage(String message) {
        log.info("Received play event: {}", message);
        try {
            JSONObject json = JSONUtil.parseObj(message);
            Long dramaId = json.getLong("dramaId");
            Long userId = json.getLong("userId");
            Long watchDuration = json.getLong("watchDuration", 0L);

            if (dramaId != null) {
                playStatService.incrementPlayCount(dramaId);
            }

            if (userId != null && dramaId != null && watchDuration > 0) {
                userProfileService.updateUserProfile(userId, dramaId, watchDuration);
            }
        } catch (Exception e) {
            log.error("Error processing play event: {}", message, e);
        }
    }
}
