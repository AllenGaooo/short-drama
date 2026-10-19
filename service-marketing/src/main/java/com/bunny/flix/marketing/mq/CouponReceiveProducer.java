package com.bunny.flix.marketing.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

/**
 * 领券消息生产者
 * 领券接口在 Redis 原子扣减成功后，发送领券消息，由消费者异步入库
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponReceiveProducer {

    private static final String TOPIC = "coupon-receive";

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送领券消息
     *
     * @param message 领券消息体
     */
    public void sendCouponReceive(CouponReceiveMessage message) {
        log.info("发送领券消息: userId={}, templateId={}, requestId={}",
                message.getUserId(), message.getTemplateId(), message.getRequestId());
        rocketMQTemplate.convertAndSend(TOPIC, message);
    }
}
