package com.bunny.flix.video.play.service;

import cn.hutool.json.JSONUtil;
import com.bunny.flix.video.play.dto.PlayEvent;
import com.bunny.flix.video.play.dto.PlayEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

/**
 * 播放事件投递服务（RocketMQ 异步解耦，设计文档第 9 节）
 *
 * 事件类型：PLAY_START / PLAY_PAUSE / PLAY_PROGRESS / PLAY_COMPLETE
 * 下游消费者：金币服务（发放奖励）、推荐服务（更新画像）、用户画像服务、数据统计服务。
 * topic 沿用 play-event，与 service-recommend 现有消费者保持兼容。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayEventService {

    public static final String TOPIC_PLAY_EVENT = "play-event";

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 投递播放事件
     *
     * @param watchDuration 本次观看时长（秒），推荐服务依赖该字段更新用户画像
     */
    public void sendEvent(Long userId, Long dramaId, Long episodeId,
                          PlayEventType type, Integer time, Integer duration, Long watchDuration) {
        PlayEvent event = PlayEvent.builder()
                .userId(userId)
                .dramaId(dramaId)
                .episodeId(episodeId)
                .event(type.name())
                .time(time)
                .duration(duration)
                .watchDuration(watchDuration == null ? 0L : watchDuration)
                .timestamp(System.currentTimeMillis())
                .build();

        try {
            rocketMQTemplate.convertAndSend(TOPIC_PLAY_EVENT, JSONUtil.toJsonStr(event));
            log.debug("[play] 投递播放事件 type={} userId={} episodeId={}", type, userId, episodeId);
        } catch (Exception e) {
            // 消息发送失败只记录日志，绝不能因为埋点失败影响用户正常播放
            log.error("[play] 播放事件投递失败 type={} userId={} episodeId={}: {}",
                    type, userId, episodeId, e.getMessage());
        }
    }
}
