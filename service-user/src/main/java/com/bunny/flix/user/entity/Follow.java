package com.bunny.flix.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注关系实体，对应数据库 t_follow 表
 * 记录用户之间的关注关系
 */
@Data
@TableName("t_follow")
public class Follow {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关注者（谁的关注列表） */
    private Long userId;

    /** 被关注者（被谁关注） */
    private Long followUserId;

    /** 关注时间 */
    private LocalDateTime createTime;
}
