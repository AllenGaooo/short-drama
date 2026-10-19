package com.bunny.flix.marketing.mq;

import com.bunny.flix.marketing.entity.CouponInstance;
import com.bunny.flix.marketing.entity.CouponTemplate;
import com.bunny.flix.marketing.mapper.CouponInstanceMapper;
import com.bunny.flix.marketing.mapper.CouponTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 领券消息消费者
 * 异步消费领券消息，做幂等判断后异步入库，实现领券接口的削峰与异步化
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "coupon-receive", consumerGroup = "service-marketing-coupon-receive")
public class CouponReceiveConsumer implements RocketMQListener<CouponReceiveMessage> {

    /** 幂等标记 Redis key 前缀 */
    private static final String COUPON_PROCESS_KEY = "coupon:process:";

    private final CouponInstanceMapper couponInstanceMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void onMessage(CouponReceiveMessage message) {
        Long userId = message.getUserId();
        Long templateId = message.getTemplateId();
        String requestId = message.getRequestId();
        log.info("收到领券消息: userId={}, templateId={}, requestId={}", userId, templateId, requestId);

        try {
            // 1. 幂等判断：用 requestId SETNX，避免重复消费重复入库
            String processKey = COUPON_PROCESS_KEY + requestId;
            Boolean first = redisTemplate.opsForValue().setIfAbsent(processKey, "1");
            if (!Boolean.TRUE.equals(first)) {
                log.info("领券消息已处理过，跳过: requestId={}", requestId);
                return;
            }

            // 2. 异步入库：创建优惠券实例
            processCoupon(userId, templateId);

            log.info("领券入库成功: userId={}, templateId={}, requestId={}", userId, templateId, requestId);
        } catch (Exception e) {
            // 入库失败：删除幂等标记，允许 RocketMQ 重试
            redisTemplate.delete(COUPON_PROCESS_KEY + requestId);
            log.error("领券入库失败: userId={}, templateId={}, requestId={}", userId, templateId, requestId, e);
            throw new RuntimeException("领券入库失败", e);
        }
    }

    /**
     * 异步入库优惠券实例并同步扣减模板库存
     */
    @Transactional
    protected void processCoupon(Long userId, Long templateId) {
        // 查模板
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("优惠券模板不存在: templateId=" + templateId);
        }

        // 扣减模板剩余库存
        if (template.getRemainingStock() != null && template.getRemainingStock() > 0) {
            template.setRemainingStock(template.getRemainingStock() - 1);
            template.setUpdateTime(LocalDateTime.now());
            couponTemplateMapper.updateById(template);
        }

        // 创建券实例
        CouponInstance instance = new CouponInstance();
        instance.setId(System.currentTimeMillis()); // id-type: input，此处用时间戳作为ID（演示，可用雪花算法替换）
        instance.setUserId(userId);
        instance.setTemplateId(templateId);
        instance.setStatus(0); // 未使用
        instance.setReceiveTime(LocalDateTime.now());
        instance.setExpireTime(LocalDateTime.now().plusDays(
                template.getValidDays() != null ? template.getValidDays() : 7));
        couponInstanceMapper.insert(instance);
    }
}
