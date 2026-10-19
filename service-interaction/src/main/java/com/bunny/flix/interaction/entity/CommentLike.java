package com.bunny.flix.interaction.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论点赞实体类，对应数据库 t_comment_like 表
 * 记录用户对某条评论的点赞状态
 */
@Data
@TableName("t_comment_like")
public class CommentLike {
    @TableId private Long id;
    /** 用户ID */
    private Long userId;
    /** 评论ID */
    private Long commentId;
    /** 状态：0-取消，1-点赞 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
