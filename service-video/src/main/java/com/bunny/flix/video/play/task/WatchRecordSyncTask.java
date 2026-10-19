package com.bunny.flix.video.play.task;

import com.bunny.flix.video.entity.WatchRecord;
import com.bunny.flix.video.mapper.WatchRecordMapper;
import com.bunny.flix.video.play.service.WatchProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 观看记录兜底补偿任务
 *
 * 定位说明：
 *   主链路是「Redis 实时写入 → RocketMQ 异步批量落库」（见 ProgressSyncConsumer）。
 *   本任务作为**兜底补偿**，处理 MQ 消息丢失/消费失败等极端情况下的进度数据，
 *   从 Redis 重新扫描并同步，保证数据最终不丢。
 *
 * 职责：
 *  1. 兜底同步：把 Redis 中的实时观看进度批量同步到 MySQL（分钟级）
 *  2. 清理过期的播放 Token（小时级）
 *
 * 说明：设计文档使用 XXL-JOB，本项目暂用 Spring @Scheduled 实现，
 * 生产环境替换为 XXL-JOB 只需把 @Scheduled 换成 @XxlJob，业务逻辑不变。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WatchRecordSyncTask {

    private final WatchProgressService watchProgressService;
    private final WatchRecordMapper watchRecordMapper;
    private final StringRedisTemplate redisTemplate;

    /** 单批次最大处理条数，避免一次同步过多拖垮数据库 */
    private static final int BATCH_LIMIT = 1000;

    /**
     * 同步观看记录：每分钟执行一次
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void syncWatchRecord() {
        long start = System.currentTimeMillis();
        int synced = 0;

        // 注意：生产环境大数据量时应改用 SCAN 游标遍历，keys 会阻塞 Redis
        Set<String> keys = watchProgressService.scanWatchKeys();
        if (keys == null || keys.isEmpty()) {
            return;
        }

        List<WatchRecord> batch = new ArrayList<>();
        for (String key : keys) {
            if (batch.size() >= BATCH_LIMIT) {
                break;
            }
            long[] ids = WatchProgressService.parseKey(key);
            if (ids == null) {
                continue;
            }
            long userId = ids[0];
            long episodeId = ids[1];

            Object positionObj = redisTemplate.opsForHash().get(key, "position");
            Object durationObj = redisTemplate.opsForHash().get(key, "duration");
            Object dramaIdObj = redisTemplate.opsForHash().get(key, "dramaId");
            if (positionObj == null) {
                continue;
            }

            Integer position = parseInt(positionObj);
            Integer duration = parseInt(durationObj);
            Long dramaId = dramaIdObj == null ? null : Long.parseLong(dramaIdObj.toString());

            WatchRecord record = new WatchRecord();
            record.setUserId(userId);
            record.setEpisodeId(episodeId);
            record.setDramaId(dramaId);
            record.setWatchTime(position);
            record.setDuration(duration);
            // 观看进度达到 95% 视为看完（避免结尾几秒误差导致无法标记完成）
            record.setStatus(isFinished(position, duration) ? 1 : 0);
            record.setUpdateTime(LocalDateTime.now());
            batch.add(record);
            synced++;
        }

        if (!batch.isEmpty()) {
            // 分批落库：这里逐条 upsert，生产可改为批量 SQL 或 MyBatis batch
            for (WatchRecord r : batch) {
                upsert(r);
            }
        }

        log.info("[play] 观看记录同步完成，扫描 {} 个Key，同步 {} 条，耗时 {}ms",
                keys.size(), synced, System.currentTimeMillis() - start);
    }

    /**
     * 清理过期播放 Token：每小时执行一次
     * Redis 的 token key 本身带 TTL 会自动过期，这里额外做一次兜底清理，
     * 防止因写入失败导致的残留。
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanExpiredToken() {
        try {
            Set<String> keys = redisTemplate.keys("play:token:*");
            if (keys == null || keys.isEmpty()) {
                return;
            }
            int cleaned = 0;
            for (String key : keys) {
                Long ttl = redisTemplate.getExpire(key);
                // TTL 为 -1 表示永不过期（异常残留），主动删除
                if (ttl != null && ttl < 0) {
                    redisTemplate.delete(key);
                    cleaned++;
                }
            }
            log.info("[play] 过期Token清理完成，共清理 {} 个残留Key", cleaned);
        } catch (Exception e) {
            log.error("[play] 清理过期Token失败: {}", e.getMessage());
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

    private Integer parseInt(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
