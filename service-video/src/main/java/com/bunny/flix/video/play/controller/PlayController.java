package com.bunny.flix.video.play.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.bunny.flix.core.Result;
import com.bunny.flix.security.UserContext;
import com.bunny.flix.video.play.dto.PlayAuthResponse;
import com.bunny.flix.video.play.dto.PlayEventType;
import com.bunny.flix.video.play.service.PlayEventService;
import com.bunny.flix.video.play.service.PlayService;
import com.bunny.flix.video.play.service.RewardIdempotentService;
import com.bunny.flix.video.play.service.WatchProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 播放控制器（设计文档第 3、4 节）
 *
 * 接口：
 *   GET  /api/play/{episodeId}      播放入口（返回带防盗链 token 的播放地址）
 *   GET  /api/play/verify           凭证校验（CDN 回源鉴权）
 *   POST /api/play/progress         观看进度上报（客户端每 5 秒一次，只写 Redis）
 *   POST /api/play/event            播放行为采集（开始/暂停/进度/完成）
 */
@Slf4j
@RestController
@RequestMapping("/api/play")
@RequiredArgsConstructor
public class PlayController {

    private final PlayService playService;
    private final WatchProgressService watchProgressService;
    private final PlayEventService playEventService;
    private final RewardIdempotentService rewardIdempotentService;

    /**
     * 播放入口：返回 CDN 播放地址与临时凭证
     * Sentinel 保护：突发流量时限流，异常时降级
     */
    @GetMapping("/{episodeId}")
    @SentinelResource(value = "play:authorize",
            blockHandler = "authorizeBlockHandler",
            fallback = "authorizeFallback")
    public Result<PlayAuthResponse> play(@PathVariable Long episodeId) {
        Long userId = UserContext.getUserId();
        return Result.success(playService.authorizePlay(userId, episodeId));
    }

    /** CDN 回源时的凭证校验接口 */
    @GetMapping("/verify")
    public Result<Boolean> verify(@RequestParam Long userId,
                                  @RequestParam Long episodeId,
                                  @RequestParam Long expire,
                                  @RequestParam String token) {
        return Result.success(playService.verifyToken(userId, episodeId, expire, token));
    }

    /**
     * 观看进度上报（高频接口，只写 Redis，由定时任务批量落库）
     */
    @PostMapping("/progress")
    @SentinelResource(value = "play:progress",
            blockHandler = "progressBlockHandler",
            fallback = "progressFallback")
    public Result<String> reportProgress(@RequestParam Long episodeId,
                                         @RequestParam Integer position,
                                         @RequestParam(required = false) Integer duration,
                                         @RequestParam(required = false) Long dramaId) {
        Long userId = UserContext.getUserId();
        watchProgressService.reportProgress(userId, dramaId, episodeId, position, duration);
        return Result.success("ok");
    }

    /** 获取续播位置（断点续播） */
    @GetMapping("/progress/{episodeId}")
    public Result<Integer> getProgress(@PathVariable Long episodeId) {
        Long userId = UserContext.getUserId();
        Integer position = watchProgressService.getLastPosition(userId, episodeId);
        return Result.success(position == null ? 0 : position);
    }

    /**
     * 播放行为采集
     * PLAY_COMPLETE 会触发金币奖励，使用 Redis SETNX 做幂等，
     * 避免 MQ 重复投递/用户重复请求导致重复发币。
     */
    @PostMapping("/event")
    @SentinelResource(value = "play:event",
            blockHandler = "eventBlockHandler",
            fallback = "eventFallback")
    public Result<String> reportEvent(@RequestParam Long episodeId,
                                      @RequestParam PlayEventType event,
                                      @RequestParam(required = false) Integer time,
                                      @RequestParam(required = false) Integer duration,
                                      @RequestParam(required = false) Long dramaId) {
        Long userId = UserContext.getUserId();

        // 播放完成：先做幂等抢占，抢到才允许投递"可发奖励"的事件
        if (event == PlayEventType.PLAY_COMPLETE) {
            boolean acquired = rewardIdempotentService.tryAcquire(userId, episodeId);
            if (!acquired) {
                log.info("[play] 重复完成事件已拦截 userId={} episodeId={}", userId, episodeId);
                return Result.success("duplicate ignored");
            }
        }

        playEventService.sendEvent(userId, dramaId, episodeId, event, time, duration,
                time == null ? 0L : time.longValue());
        return Result.success("ok");
    }

    // ---------------- Sentinel 限流与降级 ----------------

    public Result<PlayAuthResponse> authorizeBlockHandler(Long episodeId, BlockException ex) {
        log.warn("[play] 播放接口限流 episodeId={}", episodeId);
        return Result.error(429, "当前观看人数过多，请稍后重试");
    }

    public Result<PlayAuthResponse> authorizeFallback(Long episodeId, Throwable t) {
        log.error("[play] 播放接口降级 episodeId={} err={}", episodeId, t.getMessage());
        return Result.error(503, "播放服务暂时不可用：" + t.getMessage());
    }

    public Result<String> progressBlockHandler(Long episodeId, Integer position,
                                               Integer duration, Long dramaId, BlockException ex) {
        // 进度上报被限流可直接丢弃，不影响用户观看体验（进度允许少量丢失）
        return Result.success("limited");
    }

    public Result<String> progressFallback(Long episodeId, Integer position,
                                           Integer duration, Long dramaId, Throwable t) {
        return Result.success("degraded");
    }

    public Result<String> eventBlockHandler(Long episodeId, PlayEventType event, Integer time,
                                            Integer duration, Long dramaId, BlockException ex) {
        return Result.success("limited");
    }

    public Result<String> eventFallback(Long episodeId, PlayEventType event, Integer time,
                                        Integer duration, Long dramaId, Throwable t) {
        return Result.success("degraded");
    }
}
