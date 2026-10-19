package com.bunny.flix.video.play.service;

import com.bunny.flix.video.play.dto.PlayEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 观看进度服务（Redis 实时态 + 异步落库）
 *
 * 设计文档第 8 节方案：
 * 每秒/每 5 秒直接写 MySQL 会带来巨大写压力（100万用户 × 60秒一次 = 巨量写），
 * 因此实时进度写 Redis（Key: watch:{userId}:{episodeId}），
 * 再由定时任务批量同步到 MySQL，把高频写转换为低频批量写。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatchProgressService {

    private static final String WATCH_KEY_PREFIX = "watch:";

    /** 进度过期时间：7 天未观看则清理 */
    private static final long WATCH_TTL_DAYS = 7;

    private final StringRedisTemplate redisTemplate;
    private final PlayEventService playEventService;

    /**
     * 上报观看进度：只写 Redis，并投递进度事件（不直接写库）
     */
    public void reportProgress(Long userId, Long dramaId, Long episodeId, Integer position, Integer duration) {
        String key = watchKey(userId, episodeId);

        // 用 Hash 存储，便于定时任务一次读取完整进度
        redisTemplate.opsForHash().put(key, "position", String.valueOf(position));
        redisTemplate.opsForHash().put(key, "duration", String.valueOf(duration));
        redisTemplate.opsForHash().put(key, "episodeId", String.valueOf(episodeId));
        redisTemplate.opsForHash().put(key, "updateTime", String.valueOf(System.currentTimeMillis()));
        if (dramaId != null) {
            redisTemplate.opsForHash().put(key, "dramaId", String.valueOf(dramaId));
        }
        redisTemplate.expire(key, WATCH_TTL_DAYS, TimeUnit.DAYS);

        // 投递播放进度事件（RocketMQ 异步化，供推荐/画像/统计消费）
        playEventService.sendEvent(userId, dramaId, episodeId, PlayEventType.PLAY_PROGRESS,
                position, duration, null);
    }

    /** 读取续播位置（断点续播） */
    public Integer getLastPosition(Long userId, Long episodeId) {
        try {
            Object v = redisTemplate.opsForHash().get(watchKey(userId, episodeId), "position");
            if (v == null) {
                return null;
            }
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            log.warn("[play] 读取续播位置失败 userId={} episodeId={}: {}", userId, episodeId, e.getMessage());
            return null;
        }
    }

    /**
     * 扫描待同步的进度 Key（定时任务使用）
     * 注意：生产环境应使用 SCAN 替代 keys，避免阻塞 Redis
     */
    public java.util.Set<String> scanWatchKeys() {
        return redisTemplate.keys(WATCH_KEY_PREFIX + "*");
    }

    /** 解析 Key 中的 userId / episodeId（格式 watch:{userId}:{episodeId}） */
    public static long[] parseKey(String key) {
        String[] parts = key.split(":");
        if (parts.length < 3) {
            return null;
        }
        try {
            return new long[]{Long.parseLong(parts[1]), Long.parseLong(parts[2])};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String watchKey(Long userId, Long episodeId) {
        return WATCH_KEY_PREFIX + userId + ":" + episodeId;
    }
}
