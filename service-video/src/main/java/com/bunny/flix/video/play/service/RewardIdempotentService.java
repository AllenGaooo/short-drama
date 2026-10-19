package com.bunny.flix.video.play.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 看剧金币奖励幂等控制（设计文档第 10 节）
 *
 * 问题：MQ 重复投递、消费者重复消费、用户反复触发完成事件，都会导致重复发金币。
 * 方案：Redis SETNX 抢占奖励标记 reward:{userId}:{episodeId}
 *   - 抢到（返回 true）→ 允许发放
 *   - 未抢到（返回 false）→ 已发放过，直接丢弃
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RewardIdempotentService {

    private static final String REWARD_KEY_PREFIX = "reward:";

    /** 幂等标记保留时长：30 天（覆盖活动周期，过期后允许重新领取） */
    private static final long REWARD_TTL_DAYS = 30;

    private final StringRedisTemplate redisTemplate;

    /**
     * 尝试抢占发放资格
     *
     * @return true=可以发放（首次），false=已发放过（重复请求）
     */
    public boolean tryAcquire(Long userId, Long episodeId) {
        String key = rewardKey(userId, episodeId);
        try {
            Boolean ok = redisTemplate.opsForValue()
                    .setIfAbsent(key, String.valueOf(System.currentTimeMillis()),
                            REWARD_TTL_DAYS, TimeUnit.DAYS);
            boolean acquired = Boolean.TRUE.equals(ok);
            if (!acquired) {
                log.info("[play] 金币奖励重复请求已拦截 userId={} episodeId={}", userId, episodeId);
            }
            return acquired;
        } catch (Exception e) {
            // Redis 异常时的策略：拒绝发放（宁可不发，不可重复发）
            log.error("[play] 幂等标记写入失败，拒绝发放 userId={} episodeId={}: {}",
                    userId, episodeId, e.getMessage());
            return false;
        }
    }

    /** 运营手动清除标记（如活动重置） */
    public void release(Long userId, Long episodeId) {
        redisTemplate.delete(rewardKey(userId, episodeId));
    }

    public static String rewardKey(Long userId, Long episodeId) {
        return REWARD_KEY_PREFIX + userId + ":" + episodeId;
    }
}
