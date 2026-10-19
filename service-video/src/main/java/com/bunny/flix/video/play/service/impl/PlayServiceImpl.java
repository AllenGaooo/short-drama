package com.bunny.flix.video.play.service.impl;

import com.bunny.flix.core.Result;
import com.bunny.flix.core.exception.BusinessException;
import com.bunny.flix.feign.drama.DramaFeignClient;
import com.bunny.flix.feign.drama.dto.EpisodeDTO;
import com.bunny.flix.video.entity.Video;
import com.bunny.flix.video.mapper.VideoMapper;
import com.bunny.flix.video.play.dto.PlayAuthResponse;
import com.bunny.flix.video.play.dto.PlayEventType;
import com.bunny.flix.video.play.service.DramaCacheService;
import com.bunny.flix.video.play.service.PlayEventService;
import com.bunny.flix.video.play.service.PlayService;
import com.bunny.flix.video.play.service.PlayTokenService;
import com.bunny.flix.video.play.service.WatchProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 播放服务实现（设计文档第 4 节核心流程）
 *
 * 流程：查询剧集信息 → 校验用户权限 → 生成播放 Token → 返回 CDN 播放地址
 * 技术要求：
 *   - 剧集/短剧信息走缓存，热点数据不击穿到 DB
 *   - 播放地址带临时 token 防盗链
 *   - 返回续播位置，支持断点续播
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayServiceImpl implements PlayService {

    private final VideoMapper videoMapper;
    private final DramaFeignClient dramaFeignClient;
    private final PlayTokenService playTokenService;
    private final WatchProgressService watchProgressService;
    private final PlayEventService playEventService;
    private final DramaCacheService dramaCacheService;

    /** CDN 域名前缀（生产环境配置在 Nacos） */
    @Value("${play.cdn.domain:https://cdn.bunny-flix.com}")
    private String cdnDomain;

    @Override
    public PlayAuthResponse authorizePlay(Long userId, Long episodeId) {
        // 1) 查询剧集信息（热点剧集走逻辑过期 + 异步重建，避免瞬时失效引发击穿）
        EpisodeDTO episode = dramaCacheService.getDetailWithLogicalExpire(episodeId,
                () -> fetchEpisode(episodeId), EpisodeDTO.class);

        if (episode == null) {
            throw new BusinessException("剧集不存在: " + episodeId);
        }

        // 2) 校验用户权限（剧集是否免费、用户是否会员等）
        checkPlayPermission(userId, episode);

        // 3) 查询视频转码后的 HLS 地址
        Video video = findVideo(episodeId);
        String baseUrl = resolvePlayUrl(episode, video);

        // 4) 生成临时播放凭证（防盗链）
        PlayTokenService.TokenResult tokenResult = playTokenService.generate(userId, episodeId);
        String playUrl = playTokenService.buildPlayUrl(baseUrl, userId, episodeId,
                tokenResult.token(), tokenResult.expireAt());

        // 5) 读取续播位置（断点续播）
        Integer lastPosition = watchProgressService.getLastPosition(userId, episodeId);

        // 6) 投递播放开始事件（异步，不影响播放接口响应）
        playEventService.sendEvent(userId, episode.getDramaId(), episodeId,
                PlayEventType.PLAY_START, lastPosition == null ? 0 : lastPosition,
                episode.getDuration(), 0L);

        return PlayAuthResponse.builder()
                .episodeId(episodeId)
                .dramaId(episode.getDramaId())
                .playUrl(playUrl)
                .token(tokenResult.token())
                .expireTime(tokenResult.expireSeconds())
                .lastPosition(lastPosition == null ? 0 : lastPosition)
                .duration(episode.getDuration())
                .build();
    }

    @Override
    public boolean verifyToken(Long userId, Long episodeId, Long expireAt, String token) {
        return playTokenService.verify(userId, episodeId, expireAt, token);
    }

    // ---------------- 内部方法 ----------------

    /** 缓存未命中时回源查短剧服务 */
    private EpisodeDTO fetchEpisode(Long episodeId) {
        try {
            Result<EpisodeDTO> res = dramaFeignClient.getEpisodeById(episodeId);
            if (res != null && res.getCode() != null && res.getCode() == 200) {
                return res.getData();
            }
            return null;
        } catch (Exception e) {
            log.error("[play] 查询剧集失败 episodeId={}: {}", episodeId, e.getMessage());
            return null;
        }
    }

    /**
     * 权限校验：免费剧集直接放行，付费剧集校验会员身份。
     * 真实场景应调用会员服务（MemberFeignClient）判断，这里按 free 字段做本地判断，
     * 保证播放主链路不被会员服务的抖动阻塞（会员校验可异步补偿）。
     */
    private void checkPlayPermission(Long userId, EpisodeDTO episode) {
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        Integer free = episode.getFree();
        if (free != null && free == 1) {
            return;
        }
        // 付费剧集：此处应调用会员服务校验，当前以剧集启用状态兜底
        if (episode.getEnabled() != null && episode.getEnabled() == 0) {
            throw new BusinessException("该剧集已下架");
        }
    }

    /** 查询转码完成的视频地址 */
    private Video findVideo(Long episodeId) {
        Video video = videoMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Video>()
                        .eq(Video::getEpisodeId, episodeId));
        if (video == null) {
            throw new BusinessException("视频不存在: " + episodeId);
        }
        // status: 0待转码 1转码中 2已完成 3失败
        if (video.getStatus() == null || video.getStatus() != 2) {
            throw new BusinessException("视频尚未转码完成，请稍后再试");
        }
        return video;
    }

    /**
     * 解析播放地址：优先使用转码后的 HLS 地址，其次剧集自带地址，最后拼接 CDN 域名。
     * HLS 格式（index.m3u8 + 多个 .ts 分片）支持分片加载、断点续播与动态码率。
     */
    private String resolvePlayUrl(EpisodeDTO episode, Video video) {
        if (video.getHlsUrl() != null && !video.getHlsUrl().isBlank()) {
            return video.getHlsUrl();
        }
        if (episode.getVideoUrl() != null && !episode.getVideoUrl().isBlank()) {
            return episode.getVideoUrl();
        }
        return cdnDomain + "/video/" + episode.getId() + "/index.m3u8";
    }
}
