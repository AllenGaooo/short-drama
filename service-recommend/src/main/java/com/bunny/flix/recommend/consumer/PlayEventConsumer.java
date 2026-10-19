package com.bunny.flix.recommend.consumer;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bunny.flix.recommend.service.RecommendService;
import com.bunny.flix.recommend.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "play-event", consumerGroup = "recommend-play-event-group")
public class PlayEventConsumer implements RocketMQListener<String> {

    private final UserProfileService userProfileService;
    private final RecommendService recommendService;
    private final StringRedisTemplate redisTemplate;

    private static final String ACTIVE_USER_KEY = "user:similarity:active:";
    private static final int RECOMMEND_REFRESH_THRESHOLD = 10;

    @Override
    public void onMessage(String message) {
        log.debug("收到播放事件: {}", message);

        try {
            JSONObject event = JSONUtil.parseObj(message);
            Long userId = event.getLong("userId");
            Long dramaId = event.getLong("dramaId");
            Long watchDuration = event.getLong("watchDuration", 0L);

            if (userId == null || dramaId == null) {
                log.warn("播放事件参数不完整: {}", message);
                return;
            }

            // 1. 更新用户画像
            userProfileService.updateUserProfile(userId, dramaId, watchDuration);

            // 2. 标记活跃用户
            String activeKey = ACTIVE_USER_KEY + userId;
            redisTemplate.opsForValue().set(activeKey, "1", 7, TimeUnit.DAYS);

            // 3. 检查是否需要触发推荐重新计算
            String refreshCounterKey = "recommend:refresh:counter:" + userId;
            Long counter = redisTemplate.opsForValue().increment(refreshCounterKey);
            redisTemplate.expire(refreshCounterKey, 1, TimeUnit.HOURS);

            if (counter != null && counter % RECOMMEND_REFRESH_THRESHOLD == 0) {
                log.info("触发推荐重新计算: userId={}, counter={}", userId, counter);
                List<Map<String, Object>> recommendations = recommendService.calculateContentBased(userId);
                recommendService.cacheRecommendations(userId, recommendations);
            }

        } catch (Exception e) {
            log.error("处理播放事件失败: message={}", message, e);
        }
    }
}
