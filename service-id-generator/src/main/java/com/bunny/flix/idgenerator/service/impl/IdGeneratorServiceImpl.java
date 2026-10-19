package com.bunny.flix.idgenerator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bunny.flix.core.exception.BusinessException;
import com.bunny.flix.feign.idgenerator.BizTypeEnum;
import com.bunny.flix.idgenerator.config.IdGeneratorProperties;
import com.bunny.flix.idgenerator.entity.IdSegment;
import com.bunny.flix.idgenerator.mapper.IdSegmentMapper;
import com.bunny.flix.idgenerator.service.IdGeneratorService;
import com.bunny.flix.idgenerator.service.SegmentBuffer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分布式 ID 生成服务实现（美团 Leaf 号段模式）
 *
 * 核心思路：
 * - 业务服务每次要 ID 时，从本地内存的号段直接自增返回，**不访问数据库**；
 * - 号段由 max_id + step 从数据库批量申请，靠乐观锁（version）保证多实例不重复分配；
 * - 双 Buffer：当前段消耗到阈值时，用独立线程池**异步**预取下一段，
 *   使数据库加载耗时不落在业务线程上；
 * - Redis 记录节点发号统计与热点 bizTag；Nacos 支持多实例水平扩展。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdGeneratorServiceImpl implements IdGeneratorService {

    private final IdSegmentMapper idSegmentMapper;
    private final IdGeneratorProperties properties;
    private final StringRedisTemplate redisTemplate;

    /** bizTag -> 号段缓冲区 */
    private final Map<String, SegmentBuffer<SegmentBuffer.Segment>> bufferCache = new ConcurrentHashMap<>();

    /** 独立线程池：仅用于异步预取号段，避免业务线程阻塞在数据库调用 */
    private ExecutorService prefetchExecutor;

    /** QPS 统计：当前秒的请求计数（用于监控接口） */
    private final AtomicLong requestCount = new AtomicLong();
    private volatile long lastStatSecond = System.currentTimeMillis() / 1000;
    private volatile double currentQps = 0.0;

    @PostConstruct
    public void init() {
        prefetchExecutor = new ThreadPoolExecutor(
                properties.getPrefetchPoolSize(),
                properties.getPrefetchPoolSize(),
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(256),
                new ThreadFactory() {
                    private final AtomicInteger idx = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "idgen-prefetch-" + idx.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        for (BizTypeEnum bizType : BizTypeEnum.values()) {
            bufferCache.put(bizType.getCode(), createBuffer(bizType));
        }
        log.info("[idgen] 初始化完成，共 {} 个业务号段，预取线程池大小={}",
                bufferCache.size(), properties.getPrefetchPoolSize());

        if (properties.isWarmUpOnStart()) {
            warmUpAll();
        }
    }

    /**
     * 服务关闭：执行号段废弃。
     * 每个 bizTag 尚未用完的号段直接丢弃（成为 ID 空洞），
     * 由于数据库 max_id 已推进，重启后新号段从更大值开始，因此不会重复。
     */
    @PreDestroy
    public void destroy() {
        long totalAbandoned = 0;
        for (Map.Entry<String, SegmentBuffer<SegmentBuffer.Segment>> e : bufferCache.entrySet()) {
            try {
                totalAbandoned += e.getValue().abandonRemaining();
            } catch (Exception ex) {
                log.warn("[idgen] 号段废弃异常 bizTag={}: {}", e.getKey(), ex.getMessage());
            }
        }
        abandonedIds.addAndGet(totalAbandoned);
        log.info("[idgen] 服务关闭，本次废弃ID数={}（ID允许空洞，保证全局不重复）", totalAbandoned);

        if (prefetchExecutor != null) {
            prefetchExecutor.shutdownNow();
        }
    }

    @Override
    public Long getId(BizTypeEnum bizType) {
        long start = System.nanoTime();
        try {
            SegmentBuffer<SegmentBuffer.Segment> buffer = getBuffer(bizType);
            long id = buffer.nextId(() -> loadSegmentFromDb(bizType));
            recordQps();
            recordRedisStat(bizType.getCode());
            return id;
        } finally {
            traceLatency(start);
        }
    }

    @Override
    public List<Long> batchGetIds(BizTypeEnum bizType, Integer count) {
        if (count == null || count <= 0) {
            throw new BusinessException("count 必须大于 0");
        }
        if (count > properties.getMaxBatchCount()) {
            throw new BusinessException("单次批量获取数量不能超过 " + properties.getMaxBatchCount());
        }
        SegmentBuffer<SegmentBuffer.Segment> buffer = getBuffer(bizType);
        List<Long> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(buffer.nextId(() -> loadSegmentFromDb(bizType)));
        }
        recordRedisStat(bizType.getCode());
        return ids;
    }

    /** 预热所有号段（启动时或运维触发） */
    public void warmUpAll() {
        for (Map.Entry<String, SegmentBuffer<SegmentBuffer.Segment>> e : bufferCache.entrySet()) {
            BizTypeEnum bizType = BizTypeEnum.valueOfCode(e.getKey());
            try {
                e.getValue().warmUp(() -> loadSegmentFromDb(bizType));
            } catch (Exception ex) {
                log.warn("[idgen] 预热失败 bizTag={}: {}", e.getKey(), ex.getMessage());
            }
        }
        log.info("[idgen] 预热完成");
    }

    /** 监控：各 bizTag 号段状态 */
    public List<SegmentBuffer.SegmentStatus> status() {
        List<SegmentBuffer.SegmentStatus> list = new ArrayList<>();
        bufferCache.forEach((k, v) -> list.add(v.status()));
        return list;
    }

    /** 监控：QPS、耗时分布、号段状态、废弃量 */
    public Map<String, Object> metrics() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("qps", String.format("%.2f", currentQps));
        m.put("avgLatencyMs", String.format("%.3f", avgLatencyMs()));
        // 压测关注的长尾指标
        m.put("p99LatencyMs", String.format("%.3f", latencyRecorder.p99()));
        m.put("p95LatencyMs", String.format("%.3f", latencyRecorder.p95()));
        m.put("maxLatencyMs", String.format("%.3f", latencyRecorder.max()));
        m.put("samples", latencyRecorder.size());
        m.put("prefetchThreshold", properties.getPrefetchThreshold());
        m.put("prefetchPoolSize", properties.getPrefetchPoolSize());
        m.put("abandonedIds", abandonedIds.get());
        return m;
    }

    // ---------------- 内部实现 ----------------

    private SegmentBuffer<SegmentBuffer.Segment> createBuffer(BizTypeEnum bizType) {
        return new SegmentBuffer<>(bizType.getCode(), prefetchExecutor, properties.getPrefetchThreshold());
    }

    private SegmentBuffer<SegmentBuffer.Segment> getBuffer(BizTypeEnum bizType) {
        SegmentBuffer<SegmentBuffer.Segment> buffer = bufferCache.get(bizType.getCode());
        if (buffer == null) {
            // 运行时新增 bizTag，惰性创建（避免服务重启）
            buffer = bufferCache.computeIfAbsent(bizType.getCode(),
                    k -> new SegmentBuffer<>(k, prefetchExecutor, properties.getPrefetchThreshold()));
            log.info("[idgen] 惰性创建号段缓冲 bizTag={}", bizType.getCode());
        }
        return buffer;
    }

    /**
     * 从数据库申请号段：max_id = max_id + step，乐观锁保证并发安全。
     * 事务内完成，确保号段分配的原子性。
     */
    @Transactional
    protected SegmentBuffer.Segment loadSegmentFromDb(BizTypeEnum bizType) {
        String bizTag = bizType.getCode();
        IdSegment segment = idSegmentMapper.selectOne(
                new LambdaQueryWrapper<IdSegment>().eq(IdSegment::getBizType, bizTag));

        if (segment == null) {
            // bizTag 不存在则自动初始化（step 用配置默认值）
            segment = new IdSegment();
            segment.setBizType(bizTag);
            segment.setMaxId(0L);
            segment.setStep(properties.getDefaultStep());
            segment.setVersion(0);
            idSegmentMapper.insert(segment);
            log.info("[idgen] 自动初始化号段 bizTag={} step={}", bizTag, segment.getStep());
        }

        int step = segment.getStep() == null || segment.getStep() <= 0
                ? properties.getDefaultStep() : segment.getStep();

        // 乐观锁重试：多实例并发申请同一 bizTag 时，只有一个能更新成功
        int maxRetry = 3;
        for (int i = 0; i < maxRetry; i++) {
            long currentMaxId = segment.getMaxId();
            int currentVersion = segment.getVersion() == null ? 0 : segment.getVersion();
            long newMaxId = currentMaxId + step;

            LambdaUpdateWrapper<IdSegment> update = new LambdaUpdateWrapper<>();
            update.eq(IdSegment::getBizType, bizTag)
                    .eq(IdSegment::getVersion, currentVersion)
                    .set(IdSegment::getMaxId, newMaxId)
                    .set(IdSegment::getVersion, currentVersion + 1);

            int updated = idSegmentMapper.update(null, update);
            if (updated > 0) {
                // 区间为 (currentMaxId, newMaxId]，即从 currentMaxId+1 开始发号
                SegmentBuffer.Segment seg = new SegmentBuffer.Segment(currentMaxId + 1, newMaxId);
                log.debug("[idgen] 申请号段 bizTag={} {} step={} 重试={}", bizTag, seg.describe(), step, i);
                return seg;
            }

            // 版本冲突，重新读取再试
            log.warn("[idgen] 乐观锁冲突 bizTag={}，重试 {}/{}", bizTag, i + 1, maxRetry);
            segment = idSegmentMapper.selectOne(
                    new LambdaQueryWrapper<IdSegment>().eq(IdSegment::getBizType, bizTag));
            if (segment == null) {
                break;
            }
        }
        throw new BusinessException("申请 ID 号段失败，请重试: " + bizTag);
    }

    /** Redis：记录发号统计与热点 bizTag（失败不影响主流程） */
    private void recordRedisStat(String bizTag) {
        if (!properties.isRedisStatEnabled() || redisTemplate == null) {
            return;
        }
        try {
            String key = "idgen:stat:" + bizTag;
            redisTemplate.opsForValue().increment(key);
            // 热点 bizTag 排序表
            redisTemplate.opsForZSet().incrementScore("idgen:hot:biztag", bizTag, 1);
        } catch (Exception e) {
            log.debug("[idgen] Redis 统计写入失败（不影响发号）: {}", e.getMessage());
        }
    }

    /** QPS 统计：按秒滚动 */
    private void recordQps() {
        long sec = System.currentTimeMillis() / 1000;
        long count = requestCount.incrementAndGet();
        if (sec != lastStatSecond) {
            synchronized (this) {
                if (sec != lastStatSecond) {
                    currentQps = count;
                    requestCount.set(0);
                    lastStatSecond = sec;
                }
            }
        }
    }

    private final AtomicLong totalLatencyNanos = new AtomicLong();
    private final AtomicLong latencySamples = new AtomicLong();

    /** 压测前重置统计（避免历史数据干扰 P99 计算） */
    public void resetLatency() {
        latencyRecorder.reset();
        totalLatencyNanos.set(0);
        latencySamples.set(0);
    }

    /** 累计废弃的 ID 数量（号段模式允许空洞的量化指标） */
    private final AtomicLong abandonedIds = new AtomicLong();

    /** 耗时记录器（统计 P95/P99，供压测验证性能目标） */
    private final LatencyRecorder latencyRecorder = new LatencyRecorder();

    private void traceLatency(long startNanos) {
        long nanos = System.nanoTime() - startNanos;
        totalLatencyNanos.addAndGet(nanos);
        latencySamples.incrementAndGet();
        // 记录到直方图，用于计算 P95/P99
        latencyRecorder.record(nanos / 1_000_000.0);
    }

    private double avgLatencyMs() {
        long samples = latencySamples.get();
        if (samples == 0) {
            return 0;
        }
        return (totalLatencyNanos.get() / (double) samples) / 1_000_000;
    }
}
