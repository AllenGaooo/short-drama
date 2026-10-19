package com.bunny.flix.video.play.dto;

/**
 * 播放行为事件类型
 * 对应设计文档中的播放行为采集：播放、暂停、进度、完成
 */
public enum PlayEventType {

    /** 开始播放 */
    PLAY_START,

    /** 暂停播放 */
    PLAY_PAUSE,

    /** 播放进度上报（心跳，客户端每 5 秒一次） */
    PLAY_PROGRESS,

    /** 播放完成（触发金币奖励与推荐画像更新） */
    PLAY_COMPLETE
}
