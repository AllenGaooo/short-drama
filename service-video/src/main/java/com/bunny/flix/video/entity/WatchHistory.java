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
 * 观看历史实体类，对应数据库 t_watch_history 表
 * 记录用户看过的剧集，按时间排序
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_watch_history")
public class WatchHistory implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 剧集ID */
    private Long episodeId;

    /** 短剧ID */
    private Long dramaId;

    /** 观看时间 */
    private LocalDateTime watchTime;
}
