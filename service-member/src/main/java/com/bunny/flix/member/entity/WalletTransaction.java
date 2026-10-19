package com.bunny.flix.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 钱包交易流水实体类，对应数据库 t_wallet_transaction 表
 * 记录每次钱包变动前、后的余额，用于对账
 */
@Data
@TableName("t_wallet_transaction")
public class WalletTransaction {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 变动金额（正数=增加，负数=减少） */
    private Long amount;
    /** 变动前余额 */
    private Long beforeBalance;
    /** 变动后余额 */
    private Long afterBalance;
    /** 交易类型：recharge-充值，consume-消费，refund-退款 */
    private String type;
    /** 关联订单号 */
    private String orderNo;
    private LocalDateTime createTime;
}
