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
 * 观看记录实体，对应数据库 drama_watch_record 表（设计文档第 7 节）
 * 由定时任务从 Redis 批量同步写入，而非播放时实时写库
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("drama_watch_record")
public class WatchRecord implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 短剧ID */
    private Long dramaId;

    /** 剧集ID */
    private Long episodeId;

    /** 已观看时长（秒） */
    private Integer watchTime;

    /** 视频总时长（秒） */
    private Integer duration;

    /** 观看状态：0-观看中，1-已看完 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
