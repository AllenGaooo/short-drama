package com.bunny.flix.idgenerator.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.bunny.flix.core.Result;
import com.bunny.flix.feign.idgenerator.BizTypeEnum;
import com.bunny.flix.idgenerator.service.IdGeneratorService;
import com.bunny.flix.idgenerator.service.SegmentBuffer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 分布式 ID 发号接口
 * 对外提供 /api/idgen/** ，业务服务通过 common-feign 的 IdGeneratorFeignClient 调用。
 */
@Slf4j
@RestController
@RequestMapping("/api/idgen")
@RequiredArgsConstructor
public class IdGeneratorController {

    private final IdGeneratorService idGeneratorService;

    /**
     * 获取下一个 ID
     * Sentinel 保护：blockHandler 处理限流，fallback 处理业务异常。
     * 注意：ID 服务不能"降级返回错误 ID"，否则会造成主键冲突，
     * 因此降级策略是**快速失败 + 明确错误**，由调用方重试或熔断。
     */
    @GetMapping("/next")
    @SentinelResource(value = "idgen:next",
            blockHandler = "nextIdBlockHandler",
            fallback = "nextIdFallback")
    public Result<Long> nextId(@RequestParam BizTypeEnum bizType) {
        return Result.success(idGeneratorService.getId(bizType));
    }

    /** 批量获取 ID */
    @PostMapping("/batch")
    @SentinelResource(value = "idgen:batch",
            blockHandler = "batchIdsBlockHandler",
            fallback = "batchIdsFallback")
    public Result<List<Long>> batchIds(@RequestParam BizTypeEnum bizType,
                                       @RequestParam Integer count) {
        return Result.success(idGeneratorService.batchGetIds(bizType, count));
    }

    /** 号段状态监控：可观察双 Buffer 命中情况 */
    @GetMapping("/status")
    public Result<List<SegmentBuffer.SegmentStatus>> status() {
        return Result.success(idGeneratorService.status());
    }

    /** 运行指标：QPS、平均耗时、预取配置 */
    @GetMapping("/metrics")
    public Result<Map<String, Object>> metrics() {
        return Result.success(idGeneratorService.metrics());
    }

    /** 手动预热（运维用） */
    @PostMapping("/warmUp")
    public Result<String> warmUp() {
        idGeneratorService.warmUpAll();
        return Result.success("预热完成");
    }

    // ---------------- Sentinel 处理 ----------------

    /** 限流：QPS 超阈值 */
    public Result<Long> nextIdBlockHandler(BizTypeEnum bizType, com.alibaba.csp.sentinel.slots.block.BlockException ex) {
        log.warn("[idgen] 触发限流 /next bizTag={} rule={}", bizType, ex.getClass().getSimpleName());
        return Result.error(429, "ID 生成请求过于频繁，请稍后重试");
    }

    /** 降级：数据库不可用等异常 */
    public Result<Long> nextIdFallback(BizTypeEnum bizType, Throwable t) {
        log.error("[idgen] /next 降级 bizTag={} err={}", bizType, t.getMessage());
        return Result.error(503, "ID 生成服务暂时不可用：" + t.getMessage());
    }

    public Result<List<Long>> batchIdsBlockHandler(BizTypeEnum bizType, Integer count,
                                                   com.alibaba.csp.sentinel.slots.block.BlockException ex) {
        log.warn("[idgen] 触发限流 /batch bizTag={} count={}", bizType, count);
        return Result.error(429, "批量 ID 请求过于频繁，请稍后重试");
    }

    public Result<List<Long>> batchIdsFallback(BizTypeEnum bizType, Integer count, Throwable t) {
        log.error("[idgen] /batch 降级 bizTag={} err={}", bizType, t.getMessage());
        return Result.error(503, "ID 生成服务暂时不可用：" + t.getMessage());
    }
}
