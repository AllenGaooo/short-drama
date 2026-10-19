package com.bunny.flix.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * VIP记录实体类，对应数据库 t_vip_record 表
 * 记录用户的VIP有效期（起始时间、到期时间）
 */
@Data
@TableName("t_vip_record")
public class VipRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 订单ID */
    private Long orderId;
    /** VIP生效时间 */
    private LocalDateTime startTime;
    /** VIP到期时间 */
    private LocalDateTime endTime;
    /** 来源：1-购买，2-赠送，3-兑换码 */
    private Integer source;
    private LocalDateTime createTime;
}
