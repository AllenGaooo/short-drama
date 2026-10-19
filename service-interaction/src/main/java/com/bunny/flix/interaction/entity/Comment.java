package com.bunny.flix.interaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论实体类，对应数据库 t_comment 表
 * 支持一级评论和多级回复（通过 parentId 关联）
 */
@Data
@TableName("t_comment")
public class Comment {
    @TableId(type = IdType.INPUT) private Long id;
    /** 评论人ID */
    private Long userId;
    /** 短剧ID */
    private Long dramaId;
    /** 剧集ID */
    private Long episodeId;
    /** 父评论ID（0=一级评论，非0=回复某条评论） */
    private Long parentId;
    /** 被回复的用户ID */
    private Long replyToUserId;
    /** 评论内容 */
    private String content;
    /** 点赞数 */
    private Integer likeCount;
    /** 回复数 */
    private Integer replyCount;
    /** 是否置顶 */
    private Integer isTop;
    /** 是否删除 */
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
