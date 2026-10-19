package com.bunny.flix.marketing.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 领券消息体
 * 领券接口校验通过后发送到 MQ，由消费者异步入库，实现领券接口的削峰与异步化
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponReceiveMessage implements Serializable {

    /** 用户ID */
    private Long userId;

    /** 优惠券模板ID */
    private Long templateId;

    /** 消息唯一ID（幂等判断使用），由生产端生成，如 UUID */
    private String requestId;
}
