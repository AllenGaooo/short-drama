package com.bunny.flix.idgenerator.service;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 轻量耗时记录器（用于压测统计 P95 / P99）
 *
 * 为什么不用 List 存全部样本？
 *   发号接口 QPS 可达数万，全量存储会撑爆内存。
 *   这里用「分桶直方图」：维护 0~100ms 共 101 个桶（1ms 精度），
 *   超过 100ms 归入溢出桶。内存占用固定，统计 P95/P99 只需从高桶往低桶累加。
 */
public class LatencyRecorder {

    /** 桶上限：100ms（发号接口正常都在 1ms 内，超过即异常） */
    private static final int MAX_BUCKET = 100;

    /** 各桶计数（下标即毫秒数） */
    private final LongAdder[] buckets = new LongAdder[MAX_BUCKET + 1];

    /** 超过 100ms 的样本数 */
    private final LongAdder overflow = new LongAdder();

    /** 总样本数 */
    private final LongAdder total = new LongAdder();

    /** 最大耗时（毫秒，保留两位小数 * 100 存整数） */
    private final AtomicLong maxMicors = new AtomicLong();

    public LatencyRecorder() {
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = new LongAdder();
        }
    }

    /** 记录一次耗时（毫秒，可为小数） */
    public void record(double latencyMs) {
        total.increment();
        int bucket = (int) Math.ceil(latencyMs);
        if (bucket > MAX_BUCKET) {
            overflow.increment();
        } else if (bucket < 0) {
            buckets[0].increment();
        } else {
            buckets[bucket].increment();
        }
        long micros = (long) (latencyMs * 100);
        maxMicors.accumulateAndGet(micros, Math::max);
    }

    /** 计算分位值（如 0.99 表示 P99） */
    public double quantile(double q) {
        long totalCount = total.sum();
        if (totalCount == 0) {
            return 0;
        }
        // 目标：第 ceil(total * q) 个样本所在的桶
        long target = (long) Math.ceil(totalCount * q);
        long cum = 0;
        // 先从溢出桶（>100ms）开始累加
        long ovf = overflow.sum();
        if (ovf > 0 && cum + ovf >= target) {
            return MAX_BUCKET + 1;
        }
        cum += ovf;
        for (int i = 0; i <= MAX_BUCKET; i++) {
            cum += buckets[i].sum();
            if (cum >= target) {
                return i;
            }
        }
        return MAX_BUCKET;
    }

    public double p95() {
        return quantile(0.95);
    }

    public double p99() {
        return quantile(0.99);
    }

    public double max() {
        return maxMicors.get() / 100.0;
    }

    public long size() {
        return total.sum();
    }

    /** 重置（压测前调用，避免历史数据干扰） */
    public void reset() {
        for (LongAdder b : buckets) {
            b.reset();
        }
        overflow.reset();
        total.reset();
        maxMicors.set(0);
    }
}
