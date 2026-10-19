package com.bunny.flix.interaction.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 弹幕实体类，对应数据库 t_danmaku 表
 * 记录用户发送的弹幕，包含内容、颜色、字体和在视频中出现的时间点
 */
@Data
@TableName("t_danmaku")
public class Danmaku {
    @TableId private Long id;
    /** 发送用户ID */
    private Long userId;
    /** 短剧ID */
    private Long dramaId;
    /** 剧集ID */
    private Long episodeId;
    /** 弹幕内容 */
    private String content;
    /** 弹幕在视频中出现的时间点（秒） */
    private Double playTime;
    /** 弹幕颜色 */
    private String color;
    /** 字体大小 */
    private Integer font;
    private LocalDateTime createTime;
}
