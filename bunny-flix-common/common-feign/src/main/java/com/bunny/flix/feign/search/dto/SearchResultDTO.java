package com.bunny.flix.feign.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 搜索结果 DTO
 * 从 Elasticsearch 搜索短剧的返回结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDTO implements Serializable {

    private Long dramaId;
    /** 短剧标题 */
    private String title;
    /** 封面图 */
    private String cover;
    /** 简介 */
    private String description;
    /** 演员名单 */
    private String actors;
    /** 标签（逗号分隔） */
    private String tags;
    /** 热度分数，分数越高越热门 */
    private Double heatScore;
    /** 播放次数 */
    private Long playCount;
    /** 点赞数 */
    private Long likeCount;
    private LocalDateTime createTime;
}
