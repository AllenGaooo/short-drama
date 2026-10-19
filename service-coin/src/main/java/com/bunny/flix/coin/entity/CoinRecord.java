package com.bunny.flix.coin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 金币流水实体，对应数据库 t_coin_record 表
 * 每笔金币变动一条记录，用于对账与明细查询
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_coin_record")
public class CoinRecord implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 变动金额（正数为获得，负数为消费） */
    private Integer amount;

    /** 变动后余额 */
    private Integer balanceAfter;

    /** 业务类型：WATCH_REWARD-看剧奖励，SIGN-签到，CONSUME-消费，REFUND-退款 */
    private String bizType;

    /** 业务ID（如剧集ID），用于幂等追溯 */
    private String bizId;

    /** 说明 */
    private String remark;

    private LocalDateTime createTime;
}
