package com.bunny.flix.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户任务实体类，对应数据库 t_user_task 表
 * 记录用户参与任务的进度和完成状态
 */
@Data
@TableName("t_user_task")
public class UserTask {
    @TableId private Long id;
    /** 用户ID */
    private Long userId;
    /** 任务ID */
    private Long taskId;
    /** 状态：0-进行中，1-已完成，2-已领取奖励 */
    private Integer status;
    /** 当前进度（如已观看秒数） */
    private Integer progress;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
