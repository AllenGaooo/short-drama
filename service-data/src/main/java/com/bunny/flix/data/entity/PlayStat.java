package com.bunny.flix.data.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 播放统计实体类，对应数据库 t_play_stat 表
 * 记录短剧每天的播放量、点赞量等统计数据
 */
@Data
@TableName("t_play_stat")
public class PlayStat {

    @TableId
    private Long id;
    /** 短剧ID */
    private Long dramaId;
    /** 统计类型：play-播放量，like-点赞，collect-收藏 */
    private String type;
    /** 统计日期 */
    private String statDate;
    /** 统计数值 */
    private Long count;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
