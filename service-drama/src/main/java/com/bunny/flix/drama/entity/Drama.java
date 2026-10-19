package com.bunny.flix.drama.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 短剧实体类，对应数据库 t_drama 表
 * 存储短剧的完整信息：标题、封面、付费方式、统计数据等
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_drama")
public class Drama implements Serializable {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 短剧标题 */
    private String title;

    /** 封面图片URL */
    private String cover;

    /** 短剧简介 */
    private String description;

    /** 上架状态：0-下架，1-上架 */
    private Integer status;

    /** 完结状态：0-连载中，1-已完结 */
    private Integer completed;

    /** 付费类型：0-免费，1-付费，2-部分付费 */
    private Integer payType;

    /** 价格（金币） */
    private Integer price;

    /** 总集数 */
    private Integer totalEpisodes;

    /** 发行方ID */
    private Long publisherId;

    /** 播放次数 */
    private Long views;

    /** 点赞数 */
    private Long likes;

    /** 收藏数 */
    private Long collects;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标记：0-未删除，1-已删除 */
    @TableLogic
    private Integer deleted;
}
