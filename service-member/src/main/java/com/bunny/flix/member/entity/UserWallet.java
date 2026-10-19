package com.bunny.flix.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户钱包实体类，对应数据库 t_user_wallet 表
 * 存储用户金币余额，使用 @Version 乐观锁防止并发扣减时数据不一致
 */
@Data
@TableName("t_user_wallet")
public class UserWallet {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 可用余额（金币） */
    private Long balance;
    /** 冻结余额（TCC事务预扣的金币） */
    private Long frozenBalance;
    /** 乐观锁版本号，每次更新自动+1，配合 MyBatis-Plus 乐观锁插件使用 */
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
