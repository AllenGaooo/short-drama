package com.bunny.flix.video.play.service;

import com.bunny.flix.video.play.dto.PlayAuthResponse;

/**
 * 播放服务（播放链路编排）
 */
public interface PlayService {

    /**
     * 播放入口：查询剧集 → 校验权限 → 生成临时凭证 → 返回 CDN 播放地址
     */
    PlayAuthResponse authorizePlay(Long userId, Long episodeId);

    /**
     * 校验播放凭证（CDN 回源鉴权 / 客户端续期校验）
     */
    boolean verifyToken(Long userId, Long episodeId, Long expireAt, String token);
}
