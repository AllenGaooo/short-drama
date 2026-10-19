package com.bunny.flix.video.play.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 播放事件消息体（投递到 RocketMQ topic: play-event）
 *
 * 字段兼容性说明：
 * service-recommend 的 PlayEventConsumer 会读取 userId / dramaId / watchDuration 三个字段，
 * 因此这三个字段必须存在，且 watchDuration 不能为 null（缺失时下游按 0 处理）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayEvent implements Serializable {

    /** 用户ID */
    private Long userId;

    /** 短剧ID（推荐服务依赖） */
    private Long dramaId;

    /** 剧集ID */
    private Long episodeId;

    /** 事件类型：PLAY_START / PLAY_PAUSE / PLAY_PROGRESS / PLAY_COMPLETE */
    private String event;

    /** 当前观看位置（秒） */
    private Integer time;

    /** 视频总时长（秒） */
    private Integer duration;

    /** 本次观看时长（秒），推荐服务依赖该字段更新用户画像 */
    private Long watchDuration;

    /** 事件时间戳 */
    private Long timestamp;
}
