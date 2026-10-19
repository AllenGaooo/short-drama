package com.bunny.flix.feign.drama.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 剧集信息 DTO
 * 一部短剧有很多集，这是每一集的详细信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeDTO implements Serializable {

    private Long id;
    /** 所属短剧ID */
    private Long dramaId;
    /** 剧集标题，比如 "第1集：相遇" */
    private String title;
    /** 第几集 */
    private Integer episodeNum;
    /** 剧集封面 */
    private String cover;
    /** 视频播放地址 */
    private String videoUrl;
    /** 视频时长（秒） */
    private Integer duration;
    /** 是否免费：0-收费，1-免费 */
    private Integer free;
    /** 是否启用：0-禁用，1-启用 */
    private Integer enabled;
    /** 排序号，数字越小越靠前 */
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
