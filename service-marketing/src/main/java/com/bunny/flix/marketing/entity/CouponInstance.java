package com.bunny.flix.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 优惠券实例实体类，对应数据库 t_coupon_instance 表
 * 记录用户领取的每一张优惠券的具体信息和使用状态
 */
@Data
@TableName("t_coupon_instance")
public class CouponInstance {
    @TableId private Long id;
    /** 用户ID */
    private Long userId;
    /** 优惠券模板ID */
    private Long templateId;
    /** 状态：0-未使用，1-已使用，2-已过期 */
    private Integer status;
    /** 使用的订单号 */
    private String orderNo;
    /** 领取时间 */
    private LocalDateTime receiveTime;
    /** 使用时间 */
    private LocalDateTime useTime;
    /** 过期时间 */
    private LocalDateTime expireTime;
}
