package com.bunny.flix.search.service;

import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 搜索缓存服务
 *
 * 解决三个经典缓存问题：
 *
 * 1. 缓存穿透（查询不存在的数据，缓存永远不命中，请求直落 ES）
 *    → 空值缓存：回源结果为空时，缓存一个空标记（短 TTL），避免同一无效 key 反复打穿
 *
 * 2. 缓存击穿（热点 key 过期瞬间，海量并发同时回源）
 *    → 互斥锁：只有一个线程回源，其余线程等待后重读缓存
 *    → 逻辑过期：热点数据不设 Redis TTL，而是在 value 里存逻辑过期时间；
 *      过期后由单个线程异步刷新，其余请求先返回旧值，做到"永不阻塞、永不断流"
 *
 * 3. 缓存雪崩（大量 key 同时失效）
 *    → TTL 加随机抖动（见 randomTtl）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchCacheService {

    /** 空值缓存标记（与真实 JSON 区分） */
    private static final String EMPTY_MARK = "__EMPTY__";

    private final StringRedisTemplate redisTemplate;

    /** 缓存重建线程池（逻辑过期异步刷新用） */
    private final ExecutorService rebuildExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "search-cache-rebuild");
        t.setDaemon(true);
        return t;
    });

    @Value("${search.cache.search-ttl-minutes:10}")
    private long searchTtlMinutes;

    @Value("${search.cache.empty-ttl-minutes:2}")
    private long emptyTtlMinutes;

    @Value("${search.cache.ranking-ttl-minutes:30}")
    private long rankingTtlMinutes;

    /** 互斥锁等待参数 */
    private static final long LOCK_WAIT_MS = 50;
    private static final int LOCK_MAX_RETRY = 20;
    private static final long LOCK_TTL_SECONDS = 10;

    // ---------- 统计（用于监控缓存命中率） ----------
    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();
    private final AtomicLong emptyHitCount = new AtomicLong();

    /**
     * 缓存穿透防护 + 互斥锁防击穿（适用于普通搜索结果）
     *
     * 流程：查缓存 → 命中（含空值）直接返回 → 未命中抢锁 → 回源 → 写缓存 → 释放锁
     */
    public <T> CacheResult<T> queryWithPassThrough(String key, Class<T> type, Supplier<T> loader) {
        // 1. 查缓存
        String json = safeGet(key);
        if (json != null) {
            if (EMPTY_MARK.equals(json)) {
                emptyHitCount.incrementAndGet();
                return CacheResult.empty();
            }
            hitCount.incrementAndGet();
            return CacheResult.of(JSONUtil.toBean(json, type), true);
        }

        // 2. 未命中：获取互斥锁，保证只有一个线程回源
        String lockKey = lockKey(key);
        boolean locked = tryLock(lockKey);
        if (!locked) {
            // 抢锁失败：短暂等待后重读缓存（不回源，避免雪崩）
            for (int i = 0; i < LOCK_MAX_RETRY; i++) {
                sleepQuietly(LOCK_WAIT_MS);
                json = safeGet(key);
                if (json != null) {
                    if (EMPTY_MARK.equals(json)) {
                        return CacheResult.empty();
                    }
                    hitCount.incrementAndGet();
                    return CacheResult.of(JSONUtil.toBean(json, type), true);
                }
            }
            // 等待超时：直接回源（可用性优先）
            log.warn("[search-cache] 等待缓存重建超时，直接回源 key={}", key);
            return CacheResult.of(loader.get(), false);
        }

        try {
            // 3. 双重检查：防止等待期间其他线程已经写好缓存
            json = safeGet(key);
            if (json != null) {
                return EMPTY_MARK.equals(json)
                        ? CacheResult.empty()
                        : CacheResult.of(JSONUtil.toBean(json, type), true);
            }

            // 4. 回源
            missCount.incrementAndGet();
            T data = loader.get();

            // 5. 写缓存：空结果写空值标记（防穿透），正常结果写 TTL（带抖动防雪崩）
            if (data == null || isEmptyList(data)) {
                safeSet(key, EMPTY_MARK, emptyTtlMinutes, TimeUnit.MINUTES);
                return CacheResult.empty();
            }
            safeSet(key, JSONUtil.toJsonStr(data), randomTtl(searchTtlMinutes), TimeUnit.MINUTES);
            return CacheResult.of(data, false);
        } finally {
            releaseLock(lockKey);
        }
    }

    /**
     * 逻辑过期（适用于热点数据，如排行榜）：
     * 热点 key 不设置 Redis TTL（永不真正过期），过期判断放在 value 内部。
     * 逻辑过期后：单个线程异步刷新，其余请求继续返回旧值，不阻塞、不击穿。
     */
    public <T> CacheResult<T> queryWithLogicalExpire(String key, Class<T> type,
                                                     Supplier<T> loader, long logicalTtlMinutes) {
        String json = safeGet(key);

        // 1. 缓存不存在：同步回源并写入（首次）
        if (json == null) {
            missCount.incrementAndGet();
            T data = loader.get();
            if (data == null) {
                return CacheResult.empty();
            }
            LogicalCacheValue<T> value = LogicalCacheValue.of(data, logicalTtlMinutes);
            // 逻辑过期模式下 Redis TTL 设为业务 TTL 的 2 倍，作为兜底清理
            safeSet(key, JSONUtil.toJsonStr(value), logicalTtlMinutes * 2, TimeUnit.MINUTES);
            return CacheResult.of(data, false);
        }

        // 2. 解析带逻辑过期时间的缓存值
        LogicalCacheValue<T> value = JSONUtil.toBean(json, LogicalCacheValue.class);
        if (value == null || value.getData() == null) {
            return CacheResult.empty();
        }
        T data = JSONUtil.toBean(JSONUtil.toJsonStr(value.getData()), type);

        // 3. 未逻辑过期：直接返回
        if (!value.isExpired()) {
            hitCount.incrementAndGet();
            return CacheResult.of(data, true);
        }

        // 4. 已逻辑过期：抢锁异步刷新，本次先返回旧值（不阻塞用户）
        String lockKey = lockKey(key);
        if (tryLock(lockKey)) {
            rebuildExecutor.submit(() -> {
                try {
                    T fresh = loader.get();
                    if (fresh != null) {
                        LogicalCacheValue<T> newValue = LogicalCacheValue.of(fresh, logicalTtlMinutes);
                        safeSet(key, JSONUtil.toJsonStr(newValue), logicalTtlMinutes * 2, TimeUnit.MINUTES);
                        log.debug("[search-cache] 热点数据异步刷新完成 key={}", key);
                    }
                } catch (Exception e) {
                    log.error("[search-cache] 热点数据异步刷新失败 key={}: {}", key, e.getMessage());
                } finally {
                    releaseLock(lockKey);
                }
            });
        }
        // 未抢到锁也直接返回旧值，保证可用性
        hitCount.incrementAndGet();
        return CacheResult.of(data, true);
    }

    /**
     * 排行榜/热点数据缓存（走逻辑过期，保证热点永不断流）
     */
    public <T> CacheResult<T> queryRanking(String key, Class<T> type, Supplier<T> loader) {
        return queryWithLogicalExpire(key, type, loader, rankingTtlMinutes);
    }

    /** 主动失效（短剧上下架后由 MQ 消费端调用） */
    public void evict(String... keys) {
        if (keys == null) return;
        for (String key : keys) {
            safeDelete(key);
        }
    }

    /** 按前缀批量失效（短剧变更时清空相关搜索缓存；生产建议用 SCAN，此处数量可控） */
    public void evictByPrefix(String prefix) {
        try {
            var keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("[search-cache] 前缀失效完成 prefix={} count={}", prefix, keys.size());
            }
        } catch (Exception e) {
            log.warn("[search-cache] 前缀失效失败 prefix={}: {}", prefix, e.getMessage());
        }
    }

    /** 缓存命中率等指标 */
    public CacheMetrics metrics() {
        long hit = hitCount.get();
        long miss = missCount.get();
        long total = hit + miss;
        double rate = total == 0 ? 0 : hit * 100.0 / total;
        return new CacheMetrics(hit, miss, emptyHitCount.get(), Math.round(rate * 100.0) / 100.0);
    }

    // ---------- 内部方法 ----------

    private boolean tryLock(String lockKey) {
        try {
            Boolean ok = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", LOCK_TTL_SECONDS, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            log.warn("[search-cache] 获取锁失败，降级为直接回源: {}", e.getMessage());
            return false;
        }
    }

    private void releaseLock(String lockKey) {
        try {
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            log.warn("[search-cache] 释放锁失败（锁会自动过期）: {}", e.getMessage());
        }
    }

    /** Redis 异常时降级：缓存不可用不应影响搜索主流程 */
    private String safeGet(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("[search-cache] 缓存读取失败，降级回源: {}", e.getMessage());
            return null;
        }
    }

    private void safeSet(String key, String value, long ttl, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl, unit);
        } catch (Exception e) {
            log.warn("[search-cache] 缓存写入失败: {}", e.getMessage());
        }
    }

    private void safeDelete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("[search-cache] 缓存删除失败: {}", e.getMessage());
        }
    }

    /** 判断是否是空列表（用于决定是否写空值缓存） */
    private boolean isEmptyList(Object data) {
        return data instanceof java.util.Collection<?> c && c.isEmpty();
    }

    /** TTL 随机抖动，避免大量 key 同时失效造成雪崩 */
    private long randomTtl(long baseMinutes) {
        return baseMinutes + (long) (Math.random() * 5);
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static String lockKey(String key) {
        return "search:lock:" + key;
    }

    // ---------- 内部数据结构 ----------

    /** 逻辑过期缓存值：数据 + 逻辑过期时间戳 */
    @Data
    public static class LogicalCacheValue<T> {
        private T data;
        private long expireAt;

        public static <T> LogicalCacheValue<T> of(T data, long ttlMinutes) {
            LogicalCacheValue<T> v = new LogicalCacheValue<>();
            v.setData(data);
            v.setExpireAt(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(ttlMinutes));
            return v;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expireAt;
        }
    }

    /** 缓存查询结果 */
    @Data
    public static class CacheResult<T> {
        private T data;
        private boolean fromCache;
        private boolean empty;

        public static <T> CacheResult<T> of(T data, boolean fromCache) {
            CacheResult<T> r = new CacheResult<>();
            r.setData(data);
            r.setFromCache(fromCache);
            r.setEmpty(false);
            return r;
        }

        public static <T> CacheResult<T> empty() {
            CacheResult<T> r = new CacheResult<>();
            r.setData(null);
            r.setFromCache(true);
            r.setEmpty(true);
            return r;
        }
    }

    /** 缓存指标 */
    public record CacheMetrics(long hit, long miss, long emptyHit, double hitRate) {
    }
}
