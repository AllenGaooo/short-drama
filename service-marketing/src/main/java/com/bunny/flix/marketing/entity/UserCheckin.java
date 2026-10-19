package com.bunny.flix.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户签到实体类，对应数据库 t_user_checkin 表
 * 记录用户每日签到，连续签到天数用于计算奖励
 */
@Data
@TableName("t_user_checkin")
public class UserCheckin {
    @TableId private Long id;
    /** 用户ID */
    private Long userId;
    /** 签到日期 */
    private LocalDate checkinDate;
    /** 连续签到天数 */
    private Integer consecutiveDays;
    private LocalDateTime createTime;
}
