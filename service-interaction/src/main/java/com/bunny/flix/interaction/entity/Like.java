package com.bunny.flix.interaction.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 点赞实体类，对应数据库 t_like 表
 */
@Data
@TableName("t_like")
public class Like {
    @TableId private Long id;
    /** 用户ID */
    private Long userId;
    /** 短剧ID */
    private Long dramaId;
    /** 状态：0-取消点赞，1-已点赞 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
