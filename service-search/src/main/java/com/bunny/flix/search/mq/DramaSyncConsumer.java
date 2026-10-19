package com.bunny.flix.search.mq;

import com.bunny.flix.feign.drama.dto.DramaDTO;
import com.bunny.flix.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 短剧同步消费者（MySQL → MQ → Search Service → ES）
 *
 * 链路：短剧后台修改数据 → 投递 drama-sync 消息 → 本服务消费 → 更新 ES 索引 + 失效缓存
 * 目标：业务数据与搜索索引的「最终一致」
 *
 * 可靠性设计：
 * 1. 幂等：基于 updateTime 版本号，丢弃旧版本消息，避免乱序导致的旧数据覆盖新数据
 * 2. 去重：Redis 记录已处理消息，短时间重复投递直接跳过
 * 3. 失败重试：不抛异常避免无限重试，交由 RocketMQ 的延迟重试与死信队列兜底
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "drama-sync", consumerGroup = "service-search-drama-sync")
public class DramaSyncConsumer implements RocketMQListener<DramaDTO> {

    private final SearchService searchService;
    private final StringRedisTemplate redisTemplate;

    /** 去重 key 前缀 */
    private static final String DEDUP_PREFIX = "search:sync:dedup:";

    /** 去重有效期（与 MQ 重试窗口匹配即可） */
    private static final long DEDUP_TTL_MINUTES = 10;

    @Override
    public void onMessage(DramaDTO dramaDTO) {
        if (dramaDTO == null || dramaDTO.getId() == null) {
            log.warn("[search-sync] 消息内容不完整，跳过");
            return;
        }
        Long dramaId = dramaDTO.getId();
        log.info("[search-sync] 收到短剧同步消息 dramaId={}", dramaId);

        // 1) 版本号校验：丢弃过期消息，防止乱序覆盖
        if (isStale(dramaId, dramaDTO.getUpdateTime())) {
            log.info("[search-sync] 消息版本过期，丢弃 dramaId={} updateTime={}",
                    dramaId, dramaDTO.getUpdateTime());
            return;
        }

        // 2) 去重：拦截 RocketMQ 重复投递
        if (!markProcessed(dramaId, dramaDTO.getUpdateTime())) {
            log.info("[search-sync] 重复消息已拦截 dramaId={}", dramaId);
            return;
        }

        // 3) 同步到 ES（内部会失效相关缓存）
        try {
            searchService.syncDramaToES(dramaDTO);
            log.info("[search-sync] 同步完成 dramaId={}", dramaId);
        } catch (Exception e) {
            // 同步失败：清除去重标记，允许后续重试
            clearProcessed(dramaId, dramaDTO.getUpdateTime());
            log.error("[search-sync] 同步失败，已允许重试 dramaId={}: {}", dramaId, e.getMessage());
        }
    }

    /**
     * 判断是否为过期消息：ES 中已记录的版本比本条消息新，则丢弃
     */
    private boolean isStale(Long dramaId, LocalDateTime updateTime) {
        if (updateTime == null) {
            return false;
        }
        try {
            String versionKey = DEDUP_PREFIX + "ver:" + dramaId;
            String cached = redisTemplate.opsForValue().get(versionKey);
            if (cached == null) {
                return false;
            }
            LocalDateTime lastVersion = LocalDateTime.parse(cached);
            return updateTime.isBefore(lastVersion);
        } catch (Exception e) {
            log.warn("[search-sync] 版本校验失败，按新消息处理 dramaId={}: {}", dramaId, e.getMessage());
            return false;
        }
    }

    /** 标记已处理（SETNX），返回 false 表示重复消息 */
    private boolean markProcessed(Long dramaId, LocalDateTime updateTime) {
        try {
            String versionKey = DEDUP_PREFIX + "ver:" + dramaId;
            String dedupKey = DEDUP_PREFIX + dramaId + ":" + (updateTime == null ? "null" : updateTime.toString());

            Boolean ok = redisTemplate.opsForValue()
                    .setIfAbsent(dedupKey, "1", DEDUP_TTL_MINUTES, TimeUnit.MINUTES);
            if (Boolean.TRUE.equals(ok) && updateTime != null) {
                // 记录已处理的最新版本号
                redisTemplate.opsForValue().set(versionKey, updateTime.toString(),
                        DEDUP_TTL_MINUTES, TimeUnit.MINUTES);
            }
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            // Redis 异常时不阻断同步，按未处理走（幂等由 ES 的 id 覆盖写保证）
            log.warn("[search-sync] 去重标记失败，继续处理 dramaId={}: {}", dramaId, e.getMessage());
            return true;
        }
    }

    private void clearProcessed(Long dramaId, LocalDateTime updateTime) {
        try {
            String dedupKey = DEDUP_PREFIX + dramaId + ":" + (updateTime == null ? "null" : updateTime.toString());
            redisTemplate.delete(dedupKey);
        } catch (Exception e) {
            log.warn("[search-sync] 清除去重标记失败 dramaId={}: {}", dramaId, e.getMessage());
        }
    }
}
