package com.bunny.flix.coin.mq;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bunny.flix.coin.service.CoinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 播放完成事件消费者（发放看剧金币奖励）
 *
 * 监听 topic: play-event，只处理 PLAY_COMPLETE 事件：
 *   用户观看完成 → 播放服务投递 PLAY_COMPLETE → 金币服务消费 → 发放金币
 *
 * 幂等保障：
 *   1. 消费端不抛异常（避免 RocketMQ 无限重试造成雪崩），失败仅记录日志
 *   2. 真正的去重由 CoinService 内的 Redis SETNX + 数据库唯一索引双重保障
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "play-event",
        consumerGroup = "coin-play-complete-group",
        selectorExpression = "PLAY_COMPLETE || *"
)
public class PlayCompleteConsumer implements RocketMQListener<String> {

    private static final String EVENT_COMPLETE = "PLAY_COMPLETE";

    private final CoinService coinService;

    @Override
    public void onMessage(String message) {
        try {
            JSONObject event = JSONUtil.parseObj(message);
            String eventType = event.getStr("event");

            // 只处理播放完成事件（其他事件由推荐、画像等服务消费）
            if (!EVENT_COMPLETE.equals(eventType)) {
                return;
            }

            Long userId = event.getLong("userId");
            Long episodeId = event.getLong("episodeId");
            Long dramaId = event.getLong("dramaId");

            if (userId == null || episodeId == null) {
                log.warn("[coin] 播放完成事件参数不完整: {}", message);
                return;
            }

            coinService.rewardForWatch(userId, episodeId, dramaId);

        } catch (Exception e) {
            // 消费异常只记录日志，不抛出，避免消息无限重试
            log.error("[coin] 处理播放完成事件失败: message={}", message, e);
        }
    }
}
