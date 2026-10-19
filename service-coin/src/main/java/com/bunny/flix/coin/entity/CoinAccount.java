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
 * 金币账户实体，对应数据库 t_coin_account 表
 * 每个用户一行，记录当前余额与累计获得
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_coin_account")
public class CoinAccount implements Serializable {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 用户ID（唯一） */
    private Long userId;

    /** 当前余额 */
    private Integer balance;

    /** 累计获得金币 */
    private Integer totalEarned;

    /** 累计消费金币 */
    private Integer totalSpent;

    /** 乐观锁版本号，防止并发更新导致余额错乱 */
    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
