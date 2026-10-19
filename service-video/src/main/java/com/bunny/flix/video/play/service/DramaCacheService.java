package com.bunny.flix.video.play.service;

import cn.hutool.json.JSONUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 短剧详情热点缓存（设计文档第 11、12 节）
 *
 * 缓存策略（Cache-Aside）：
 *   读：命中 Redis 直接返回；未命中则查 DB → 写回 Redis
 *   防击穿：热点 Key 失效瞬间若有百万并发直落 DB，用分布式锁保证只有一个线程回源，
 *          其余线程短暂等待后重读缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DramaCacheService {

    private static final String DETAIL_KEY_PREFIX = "drama:detail:";
    private static final String LOCK_KEY_PREFIX = "drama:lock:";

    /** 详情缓存时长：10 分钟（加随机抖动，避免同一时刻集体失效造成雪崩） */
    private static final long DETAIL_TTL_MINUTES = 10;
    /** 回源锁持有时间：3 秒，防止持锁线程宕机导致死锁 */
    private static final long LOCK_TTL_SECONDS = 3;
    /** 未抢到锁的线程重试等待（毫秒） */
    private static final long RETRY_WAIT_MS = 50;
    private static final int MAX_RETRY = 20;

    private final StringRedisTemplate redisTemplate;

    /** 缓存异步重建线程池（逻辑过期模式专用） */
    private final ExecutorService rebuildExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "drama-cache-rebuild");
        t.setDaemon(true);
        return t;
    });

    /**
     * 查询短剧详情（带缓存与击穿保护）
     *
     * @param dramaId  短剧ID
     * @param loader   回源加载器（查 DB / 远程服务）
     * @param type     反序列化目标类型
     */
    public <T> T getDetail(Long dramaId, Supplier<T> loader, Class<T> type) {
        String key = detailKey(dramaId);

        // 1) 读缓存
        String cached = safeGet(key);
        if (cached != null) {
            return JSONUtil.toBean(cached, type);
        }

        // 2) 未命中：抢回源锁
        String lockKey = lockKey(dramaId);
        boolean locked = tryLock(lockKey);
        if (locked) {
            try {
                // 双重检查：防止等待期间别的线程已回源
                cached = safeGet(key);
                if (cached != null) {
                    return JSONUtil.toBean(cached, type);
                }
                T data = loader.get();
                if (data != null) {
                    // 写入缓存并加随机抖动，防雪崩
                    safeSet(key, JSONUtil.toJsonStr(data), randomizedTtl());
                }
                return data;
            } finally {
                releaseLock(lockKey);
            }
        }

        // 3) 未抢到锁：等待并重试读缓存
        for (int i = 0; i < MAX_RETRY; i++) {
            sleepQuietly(RETRY_WAIT_MS);
            cached = safeGet(key);
            if (cached != null) {
                return JSONUtil.toBean(cached, type);
            }
        }
        // 兜底：直接回源（保证可用性优先）
        log.warn("[play] 等待缓存回源超时，直接回源 dramaId={}", dramaId);
        return loader.get();
    }

    /**
     * 逻辑过期模式（热点剧集专用）
     *
     * 与 getDetail 的互斥锁方案区别：
     *   - 互斥锁：缓存失效后，未抢到锁的线程要等待回源完成（有阻塞，但数据一定最新）
     *   - 逻辑过期：缓存不设真正的 Redis TTL，过期判断放在 value 内部；
     *     过期后由单个线程**异步重建**，其余请求继续返回旧值 —— 永不阻塞、永不断流
     *
     * 热点剧集（百万并发）适用逻辑过期：瞬时失效时不能让海量请求等待回源，
     * 宁可短暂返回旧数据，也不能让线程池被拖垮。
     */
    public <T> T getDetailWithLogicalExpire(Long dramaId, Supplier<T> loader, Class<T> type) {
        String key = detailKey(dramaId);
        String json = safeGet(key);

        // 1. 缓存不存在：同步回源并写入（首次访问）
        if (json == null) {
            T data = loader.get();
            if (data != null) {
                safeSet(key, JSONUtil.toJsonStr(LogicalCacheValue.of(data, DETAIL_TTL_MINUTES)),
                        DETAIL_TTL_MINUTES * 2, TimeUnit.MINUTES);
            }
            return data;
        }

        // 2. 解析带逻辑过期时间的值
        LogicalCacheValue value = JSONUtil.toBean(json, LogicalCacheValue.class);
        if (value == null || value.getData() == null) {
            return loader.get();
        }
        T data = JSONUtil.toBean(JSONUtil.toJsonStr(value.getData()), type);

        // 3. 未逻辑过期：直接返回
        if (!value.isExpired()) {
            return data;
        }

        // 4. 已逻辑过期：抢锁后异步重建，本次先返回旧值（不阻塞）
        String lockKey = lockKey(dramaId);
        if (tryLock(lockKey)) {
            rebuildExecutor.submit(() -> {
                try {
                    T fresh = loader.get();
                    if (fresh != null) {
                        safeSet(key, JSONUtil.toJsonStr(LogicalCacheValue.of(fresh, DETAIL_TTL_MINUTES)),
                                DETAIL_TTL_MINUTES * 2, TimeUnit.MINUTES);
                        log.debug("[play] 热点剧集缓存异步重建完成 dramaId={}", dramaId);
                    }
                } catch (Exception e) {
                    log.error("[play] 热点剧集缓存异步重建失败 dramaId={}: {}", dramaId, e.getMessage());
                } finally {
                    releaseLock(lockKey);
                }
            });
        }
        // 未抢到锁也返回旧值，保证可用性
        return data;
    }

    /** 主动失效（短剧上下架/信息变更时调用） */
    public void evict(Long dramaId) {
        safeDelete(detailKey(dramaId));
    }

    /** 逻辑过期缓存值：数据 + 逻辑过期时间戳 */
    @Getter
    public static class LogicalCacheValue {
        private Object data;
        private long expireAt;

        public static LogicalCacheValue of(Object data, long ttlMinutes) {
            LogicalCacheValue v = new LogicalCacheValue();
            v.setData(data);
            v.setExpireAt(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(ttlMinutes));
            return v;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expireAt;
        }
    }

    // ---------- 基础操作（Redis 异常降级，不阻断主流程） ----------

    private boolean tryLock(String lockKey) {
        try {
            Boolean ok = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", LOCK_TTL_SECONDS, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            log.warn("[play] 获取缓存锁失败，降级为直接回源: {}", e.getMessage());
            return false;
        }
    }

    private void releaseLock(String lockKey) {
        try {
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            log.warn("[play] 释放缓存锁失败（锁会自动过期）: {}", e.getMessage());
        }
    }

    private String safeGet(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("[play] 缓存读取失败，降级回源: {}", e.getMessage());
            return null;
        }
    }

    private void safeSet(String key, String value, long ttlMinutes) {
        try {
            redisTemplate.opsForValue().set(key, value, ttlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("[play] 缓存写入失败: {}", e.getMessage());
        }
    }

    private void safeDelete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("[play] 缓存删除失败: {}", e.getMessage());
        }
    }

    /** 10 分钟 + 0~2 分钟随机抖动，避免缓存雪崩 */
    private long randomizedTtl() {
        return DETAIL_TTL_MINUTES + (long) (Math.random() * 2);
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static String detailKey(Long dramaId) {
        return DETAIL_KEY_PREFIX + dramaId;
    }

    private static String lockKey(Long dramaId) {
        return LOCK_KEY_PREFIX + dramaId;
    }
}
