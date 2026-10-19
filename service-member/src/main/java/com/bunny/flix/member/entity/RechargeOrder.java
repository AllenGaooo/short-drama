package com.bunny.flix.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 充值订单实体类，对应数据库 t_recharge_order 表
 * 记录用户充值金币的订单
 */
@Data
@TableName("t_recharge_order")
public class RechargeOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 订单号 */
    private String orderNo;
    /** 充值金币数 */
    private Long amount;
    /** 充值金额（分） */
    private Integer money;
    /** 状态：0-待支付，1-已支付，2-已取消 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
