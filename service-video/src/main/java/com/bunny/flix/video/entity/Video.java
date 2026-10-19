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
 * 视频实体类，对应数据库 t_video 表
 * 存储视频文件信息、转码状态和多清晰度地址
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_video")
public class Video implements Serializable {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 所属剧集ID */
    private Long episodeId;

    /** 原始视频URL（上传时的地址） */
    private String originalUrl;

    /** 转码状态：0-待转码，1-转码中，2-已完成，3-失败 */
    private Integer status;

    /** 多清晰度地址，JSON格式存储，如 {"360p":"url1","720p":"url2"} */
    private String resolutions;

    /** HLS 流播放地址 */
    private String hlsUrl;

    /** 视频封面图 */
    private String coverUrl;

    /** 视频时长（秒） */
    private Long duration;

    /** 文件大小（字节） */
    private Long size;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
