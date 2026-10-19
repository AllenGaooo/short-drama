package com.bunny.flix.video.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 播放进度实体类，对应数据库 t_play_progress 表
 * 记录用户观看视频的进度，支持断点续播
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_play_progress")
public class PlayProgress implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 剧集ID */
    private Long episodeId;

    /** 短剧ID */
    private Long dramaId;

    /** 当前播放位置（秒），如 120.5 表示看到第2分0.5秒 */
    private Double position;

    /** 视频总时长（秒） */
    private Double duration;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
