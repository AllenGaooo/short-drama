package com.bunny.flix.video.play.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 播放入口返回（对应设计文档 GET /api/play/{episodeId} 响应）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayAuthResponse implements Serializable {

    /** 剧集ID */
    private Long episodeId;

    /** 短剧ID */
    private Long dramaId;

    /** CDN 播放地址（带 token 与过期时间参数） */
    private String playUrl;

    /** 临时播放凭证 */
    private String token;

    /** 凭证有效期（秒） */
    private Long expireTime;

    /** 续播位置（秒），支持断点续播 */
    private Integer lastPosition;

    /** 视频总时长（秒） */
    private Integer duration;
}
