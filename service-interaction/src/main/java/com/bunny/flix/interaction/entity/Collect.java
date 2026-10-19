package com.bunny.flix.interaction.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 收藏实体类，对应数据库 t_collect 表
 */
@Data
@TableName("t_collect")
public class Collect {
    @TableId private Long id;
    /** 用户ID */
    private Long userId;
    /** 短剧ID */
    private Long dramaId;
    /** 状态：0-取消收藏，1-已收藏 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
