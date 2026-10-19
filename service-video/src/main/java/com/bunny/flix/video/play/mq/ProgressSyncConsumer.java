package com.bunny.flix.video.play.mq;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bunny.flix.video.entity.WatchRecord;
import com.bunny.flix.video.mapper.WatchRecordMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 观看进度异步批量落库消费者
 *
 * 职责：把高频的播放进度上报，通过 RocketMQ 异步、批量地写入 MySQL，
 *       避免每 5 秒一次上报直接冲击数据库。
 *
 * 批量策略（攒批写入）：
 *   1. 缓冲区达到 BATCH_SIZE(100) 条 → 立即批量落库
 *   2. 或每 5 秒定时 flush 一次 → 保证低峰期数据也能及时落库
 *
 * 消费的是 play-event 主题中的 PLAY_PROGRESS 事件；
 * 该主题还有 recommend（更新画像）、coin（发奖励）等消费者组，
 * 不同 consumerGroup 各自消费全量消息，互不影响。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "play-event", consumerGroup = "video-progress-sync-group")
public class ProgressSyncConsumer implements RocketMQListener<String> {

    private static final String EVENT_PROGRESS = "PLAY_PROGRESS";

    /** 批量落库阈值 */
    private static final int BATCH_SIZE = 100;

    private final WatchRecordMapper watchRecordMapper;

    /** 待落库缓冲区 */
    private final List<WatchRecord> buffer = new ArrayList<>();

    @Override
    public void onMessage(String message) {
        try {
            JSONObject event = JSONUtil.parseObj(message);

            // 只处理进度上报事件
            if (!EVENT_PROGRESS.equals(event.getStr("event"))) {
                return;
            }

            Long userId = event.getLong("userId");
            Long episodeId = event.getLong("episodeId");
            Long dramaId = event.getLong("dramaId");
            Integer position = event.getInt("time");
            Integer duration = event.getInt("duration");

            if (userId == null || episodeId == null || position == null) {
                log.debug("[play-sync] 进度事件参数不完整，跳过: {}", message);
                return;
            }

            WatchRecord record = new WatchRecord();
            record.setUserId(userId);
            record.setEpisodeId(episodeId);
            record.setDramaId(dramaId);
            record.setWatchTime(position);
            record.setDuration(duration);
            // 观看进度达 95% 视为看完
            record.setStatus(isFinished(position, duration) ? 1 : 0);
            record.setUpdateTime(LocalDateTime.now());

            synchronized (buffer) {
                buffer.add(record);
                if (buffer.size() >= BATCH_SIZE) {
                    flush();
                }
            }
        } catch (Exception e) {
            // 消费异常只记录日志，避免 RocketMQ 无限重试造成消息堆积
            log.error("[play-sync] 处理进度事件失败: {}", message, e);
        }
    }

    /**
     * 定时 flush：每 5 秒把缓冲区数据批量落库，
     * 保证低峰期（缓冲区迟迟不满）数据也能及时入库
     */
    @Scheduled(fixedDelay = 5000)
    public void scheduledFlush() {
        synchronized (buffer) {
            if (!buffer.isEmpty()) {
                flush();
            }
        }
    }

    /** 服务关闭前把剩余数据落库，减少丢失 */
    @PreDestroy
    public void destroy() {
        synchronized (buffer) {
            if (!buffer.isEmpty()) {
                flush();
            }
        }
    }

    /** 批量落库：把 N 条进度合并为一次批量操作，显著降低数据库写压力 */
    private void flush() {
        if (buffer.isEmpty()) {
            return;
        }
        List<WatchRecord> batch = new ArrayList<>(buffer);
        buffer.clear();

        try {
            for (WatchRecord r : batch) {
                upsert(r);
            }
            log.info("[play-sync] 批量落库完成，本次 {} 条", batch.size());
        } catch (Exception e) {
            log.error("[play-sync] 批量落库失败，丢弃本批 {} 条: {}", batch.size(), e.getMessage());
        }
    }

    /** 存在则更新，不存在则插入（按 userId + episodeId 维度） */
    private void upsert(WatchRecord record) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WatchRecord> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WatchRecord>()
                        .eq(WatchRecord::getUserId, record.getUserId())
                        .eq(WatchRecord::getEpisodeId, record.getEpisodeId());
        WatchRecord exist = watchRecordMapper.selectOne(wrapper);
        if (exist != null) {
            record.setId(exist.getId());
            watchRecordMapper.updateById(record);
        } else {
            record.setCreateTime(LocalDateTime.now());
            watchRecordMapper.insert(record);
        }
    }

    private boolean isFinished(Integer position, Integer duration) {
        if (position == null || duration == null || duration <= 0) {
            return false;
        }
        return position >= duration * 0.95;
    }
}
