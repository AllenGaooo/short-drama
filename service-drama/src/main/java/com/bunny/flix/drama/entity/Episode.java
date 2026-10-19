package com.bunny.flix.drama.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 剧集实体类，对应数据库 t_episode 表
 * 一部短剧包含多个剧集（第1集、第2集...）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_episode")
public class Episode implements Serializable {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 所属短剧ID */
    private Long dramaId;

    /** 剧集标题，如 "第1集：相遇" */
    private String title;

    /** 第几集 */
    private Integer episodeNum;

    /** 剧集封面 */
    private String cover;

    /** 视频播放地址 */
    private String videoUrl;

    /** 视频时长（秒） */
    private Integer duration;

    /** 是否免费：0-付费，1-免费 */
    private Integer free;

    /** 是否启用：0-禁用，1-启用 */
    private Integer enabled;

    /** 排序号 */
    private Integer sortOrder;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
