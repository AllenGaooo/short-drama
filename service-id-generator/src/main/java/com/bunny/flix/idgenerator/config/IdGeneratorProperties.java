package com.bunny.flix.idgenerator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ID 生成器配置
 * 号段步长、预取阈值、线程池、限流降级等均可配置，便于按业务调优
 */
@Data
@Component
@ConfigurationProperties(prefix = "idgen")
public class IdGeneratorProperties {

    /** 预取阈值：当前号段剩余量 <= 区间长度 * 该比例时，异步预取下一段（默认 20%） */
    private double prefetchThreshold = 0.2;

    /** 异步预取线程池大小 */
    private int prefetchPoolSize = 4;

    /** 新建 bizTag 时的默认号段步长 */
    private int defaultStep = 1000;

    /** 单次批量获取 ID 的最大数量 */
    private int maxBatchCount = 1000;

    /** 启动时是否预热所有号段（提前装载，避免首个请求同步等数据库） */
    private boolean warmUpOnStart = true;

    /** 是否开启 Redis 发号统计 */
    private boolean redisStatEnabled = true;
}
