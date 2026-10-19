package com.bunny.flix.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * VIP订单实体类，对应数据库 t_vip_order 表
 * 记录用户购买VIP的订单信息
 */
@Data
@TableName("t_vip_order")
public class VipOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 用户ID */
    private Long userId;
    /** VIP套餐ID */
    private Long planId;
    /** 订单号 */
    private String orderNo;
    /** 实付金额（分） */
    private Integer amount;
    /** 状态：0-待支付，1-已支付，2-已取消 */
    private Integer status;
    /** 支付时间 */
    private LocalDateTime payTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
