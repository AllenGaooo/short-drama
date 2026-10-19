package com.bunny.flix.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * VIP套餐实体类，对应数据库 t_vip_plan 表
 * 定义不同档位的VIP套餐（如月卡、季卡、年卡）
 */
@Data
@TableName("t_vip_plan")
public class VipPlan {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 套餐名称，如 "月度VIP" */
    private String name;
    /** 价格（分） */
    private Integer price;
    /** 有效天数 */
    private Integer durationDays;
    /** 套餐描述 */
    private String description;
    /** 是否启用 */
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
