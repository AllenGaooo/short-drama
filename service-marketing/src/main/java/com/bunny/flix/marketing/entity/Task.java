package com.bunny.flix.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 任务实体类，对应数据库 t_task 表
 * 定义营销任务（如观看视频N分钟奖励金币）
 */
@Data
@TableName("t_task")
public class Task {
    @TableId private Long id;
    /** 任务名称 */
    private String name;
    /** 任务类型 */
    private String type;
    /** 任务描述 */
    private String description;
    /** 要求时长（秒），如观看 300 秒 */
    private Integer durationRequired;
    /** 奖励金币数 */
    private Integer rewardCoin;
    /** 是否启用 */
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
