package com.bunny.flix.feign.drama.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 短剧信息 DTO
 * 用于微服务之间传输短剧数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DramaDTO implements Serializable {

    private Long id;
    /** 短剧标题 */
    private String title;
    /** 封面图片地址 */
    private String cover;
    /** 短剧简介 */
    private String description;
    /** 状态：0-下架，1-上架 */
    private Integer status;
    /** 是否完结：0-连载中，1-已完结 */
    private Integer completed;
    /** 付费类型：0-免费，1-按集付费，2-VIP免费 */
    private Integer payType;
    /** 单集价格（分） */
    private Integer price;
    /** 总集数 */
    private Integer totalEpisodes;
    /** 发布者ID */
    private Long publisherId;
    /** 播放量 */
    private Long views;
    /** 点赞数 */
    private Long likes;
    /** 收藏数 */
    private Long collects;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
