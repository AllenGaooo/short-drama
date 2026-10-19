package com.bunny.flix.idgenerator.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 号段缓冲区（双 Buffer，参考美团 Leaf Segment 设计）
 *
 * 设计要点：
 * 1. 双段：current 供发号，next 由异步线程提前装载，当前段耗尽时切换，
 *    从而让"向数据库申请号段"的耗时不落在业务请求的调用链路上。
 * 2. 线程安全：发号用 AtomicLong 自增且不越界（tryNext 越界返回 -1），
 *    段切换/加载走 synchronized，保证不重复、不丢失。
 * 3. 预取阈值：按"剩余量占比"判断，而不是拿游标和 end 直接比较。
 *
 * @param <T> 号段类型
 */
@Slf4j
public class SegmentBuffer<T> {

    private final String bizType;

    private final Executor prefetchExecutor;

    /** 预取阈值：剩余量 <= 区间长度 * threshold 时触发 */
    private final double prefetchThreshold;

    /** 统计：累计发号数量 */
    private final AtomicLong totalIds = new AtomicLong();
    /** 统计：向数据库申请号段次数（用于观察号段命中率） */
    private final AtomicLong dbFetchCount = new AtomicLong();
    /** 统计：异步预取成功次数 */
    private final AtomicLong asyncPrefetchCount = new AtomicLong();

    @Getter
    private volatile T current;

    private volatile T next;

    private final AtomicBoolean prefetching = new AtomicBoolean(false);

    private final AtomicBoolean needPrefetch = new AtomicBoolean(true);

    public SegmentBuffer(String bizType, Executor prefetchExecutor, double prefetchThreshold) {
        this.bizType = bizType;
        this.prefetchExecutor = prefetchExecutor;
        this.prefetchThreshold = prefetchThreshold;
    }

    /**
     * 取下一个 ID（线程安全，保证不越界、不重复）
     *
     * @param segmentLoader 号段加载器（真正去数据库申请 max_id + step）
     * @return 全局唯一 ID
     */
    public long nextId(Supplier<Segment> segmentLoader) {
        while (true) {
            // 快路径：当前段直接发号，无锁
            Segment cur = (Segment) current;
            if (cur != null && !cur.isExhausted()) {
                long id = cur.tryNext();
                if (id != ILLEGAL_ID) {
                    totalIds.incrementAndGet();
                    triggerPrefetchIfNeeded(segmentLoader, cur);
                    return id;
                }
            }

            // 慢路径：当前段耗尽或尚未初始化，需要切换/加载（加锁保证只有一个线程做）
            synchronized (this) {
                cur = (Segment) current;
                if (cur != null && !cur.isExhausted()) {
                    long id = cur.tryNext();
                    if (id != ILLEGAL_ID) {
                        totalIds.incrementAndGet();
                        return id;
                    }
                }

                // 双 Buffer 切换：优先使用已预取好的 next，避免业务线程等数据库
                Segment nx = (Segment) next;
                if (nx != null && !nx.isExhausted()) {
                    current = (T) nx;
                    next = null;
                    needPrefetch.set(true);
                    long id = nx.tryNext();
                    if (id != ILLEGAL_ID) {
                        totalIds.incrementAndGet();
                        asyncPrefetch(segmentLoader);
                        return id;
                    }
                }

                // 兜底：next 也没准备好（如首次启动、预取失败），同步加载
                Segment loaded = segmentLoader.get();
                dbFetchCount.incrementAndGet();
                current = (T) loaded;
                next = null;
                needPrefetch.set(true);
                long id = loaded.tryNext();
                if (id != ILLEGAL_ID) {
                    totalIds.incrementAndGet();
                    asyncPrefetch(segmentLoader);
                    return id;
                }
            }
        }
    }

    /** 剩余量低于阈值时异步预取下一号段 */
    private void triggerPrefetchIfNeeded(Supplier<Segment> loader, Segment cur) {
        double ratio = cur.remainingRatio();
        if (ratio <= prefetchThreshold) {
            asyncPrefetch(loader);
        }
    }

    /**
     * 异步预取：提交到独立线程池，不阻塞业务线程。
     * 通过 prefetching 标志避免重复提交，needPrefetch 标志控制是否需要。
     */
    private void asyncPrefetch(Supplier<Segment> loader) {
        if (!needPrefetch.compareAndSet(true, false)) {
            return;
        }
        if (!prefetching.compareAndSet(false, true)) {
            return;
        }
        prefetchExecutor.execute(() -> {
            try {
                Segment seg = loader.get();
                dbFetchCount.incrementAndGet();
                synchronized (this) {
                    if (next == null) {
                        next = (T) seg;
                        asyncPrefetchCount.incrementAndGet();
                        log.debug("[idgen] 异步预取成功 bizType={} segment={}", bizType, seg);
                    }
                }
            } catch (Exception e) {
                // 预取失败：恢复标志，等待下一次触发重试
                needPrefetch.set(true);
                log.warn("[idgen] 异步预取失败 bizType={}: {}", bizType, e.getMessage());
            } finally {
                prefetching.set(false);
            }
        });
    }

    /** 启动预热：同步装载当前段，并异步预取下一段 */
    public void warmUp(Supplier<Segment> loader) {
        synchronized (this) {
            if (current != null) {
                return;
            }
            Segment seg = loader.get();
            dbFetchCount.incrementAndGet();
            current = (T) seg;
        }
        needPrefetch.set(true);
        asyncPrefetch(loader);
    }

    /**
     * 号段废弃：服务关闭时统计当前尚未使用的 ID 数量并丢弃。
     *
     * 为什么可以安全废弃？
     *   数据库中的 max_id 已经推进过，重启后新申请的号段从更大的值开始，
     *   因此废弃的这些 ID 永远不会被再次分配 —— **保证 ID 不重复**。
     *
     * 代价：这部分 ID 成为"空洞"（不连续）。这是号段模式的设计取舍：
     *   用「允许少量 ID 空洞」换取「不发号时零数据库访问的高可用」。
     *
     * @return 本次废弃的 ID 数量
     */
    public long abandonRemaining() {
        long abandoned = 0;
        Segment cur = (Segment) current;
        if (cur != null) {
            abandoned += cur.remaining();
        }
        Segment nx = (Segment) next;
        if (nx != null) {
            abandoned += nx.remaining();
        }
        // 清空引用，避免继续使用已废弃的号段
        current = null;
        next = null;
        if (abandoned > 0) {
            log.info("[idgen] 号段废弃 bizTag={} 废弃ID数={}（允许空洞，保证不重复）", bizType, abandoned);
        }
        return abandoned;
    }

    /** 号段状态快照（供监控接口使用） */
    public SegmentStatus status() {
        Segment cur = (Segment) current;
        Segment nx = (Segment) next;
        return new SegmentStatus(
                bizType,
                cur == null ? null : cur.describe(),
                nx == null ? null : nx.describe(),
                totalIds.get(),
                dbFetchCount.get(),
                asyncPrefetchCount.get(),
                prefetching.get()
        );
    }

    public static final long ILLEGAL_ID = -1L;

    public record SegmentStatus(String bizType, String current, String next,
                                long totalIds, long dbFetchCount, long asyncPrefetchCount,
                                boolean prefetching) {
    }

    /**
     * 单个号段：[start, end]，游标从 start 自增到 end
     */
    @Getter
    public static class Segment {
        private final long start;
        private final long end;
        private final AtomicLong cursor;

        public Segment(long start, long end) {
            this.start = start;
            this.end = end;
            this.cursor = new AtomicLong(start);
        }

        /** 原子取号，越界返回 ILLEGAL_ID（不抛异常，交由上层切换号段） */
        public long tryNext() {
            while (true) {
                long cur = cursor.get();
                if (cur > end) {
                    return ILLEGAL_ID;
                }
                if (cursor.compareAndSet(cur, cur + 1)) {
                    return cur;
                }
            }
        }

        public boolean isExhausted() {
            return cursor.get() > end;
        }

        /** 剩余可用 ID 数量 */
        public long remaining() {
            long left = end - cursor.get() + 1;
            return left < 0 ? 0 : left;
        }

        /** 剩余量占区间长度的比例（0~1），用剩余量而非游标判断，避免号段起点非 0 时误判 */
        public double remainingRatio() {
            long size = end - start + 1;
            if (size <= 0) {
                return 0;
            }
            return (double) remaining() / size;
        }

        public String describe() {
            return String.format("[start=%d, end=%d, cursor=%d, remaining=%d]",
                    start, end, cursor.get(), remaining());
        }
    }
}
