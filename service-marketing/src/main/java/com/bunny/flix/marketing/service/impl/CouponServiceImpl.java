package com.bunny.flix.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bunny.flix.core.exception.BusinessException;
import com.bunny.flix.marketing.entity.CouponInstance;
import com.bunny.flix.marketing.entity.CouponTemplate;
import com.bunny.flix.marketing.mapper.CouponInstanceMapper;
import com.bunny.flix.marketing.mapper.CouponTemplateMapper;
import com.bunny.flix.marketing.mq.CouponReceiveMessage;
import com.bunny.flix.marketing.mq.CouponReceiveProducer;
import com.bunny.flix.marketing.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final CouponInstanceMapper couponInstanceMapper;
    private final StringRedisTemplate redisTemplate;
    private final CouponReceiveProducer couponReceiveProducer;

    private static final String COUPON_STOCK_KEY = "coupon:stock:";
    private static final String COUPON_RECEIVED_KEY = "coupon:received:";

    // Lua script for atomic stock deduction with idempotency check
    private static final String DEDUCT_STOCK_LUA = """
            local stockKey = KEYS[1]
            local receivedKey = KEYS[2]
            local userId = ARGV[1]
            
            -- Check idempotency
            if redis.call('SISMEMBER', receivedKey, userId) == 1 then
                return -1
            end
            
            -- Check stock
            local stock = tonumber(redis.call('GET', stockKey) or '0')
            if stock <= 0 then
                return 0
            end
            
            -- Deduct stock and mark received
            redis.call('DECR', stockKey)
            redis.call('SADD', receivedKey, userId)
            return 1
            """;

    @Override
    public Map<String, Object> receiveCoupon(Long userId, Long templateId) {
        // 1. 模板校验
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null || template.getEnabled() == 0) {
            throw new BusinessException("优惠券模板不存在或已禁用");
        }

        // 2. Redis Lua 原子扣减库存 + 幂等校验（SISMEMBER 防止重复领取）
        String receivedKey = COUPON_RECEIVED_KEY + templateId;
        String stockKey = COUPON_STOCK_KEY + templateId;

        // 库存未初始化时先初始化
        if (redisTemplate.opsForValue().get(stockKey) == null) {
            redisTemplate.opsForValue().set(stockKey, String.valueOf(template.getRemainingStock()));
        }

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(DEDUCT_STOCK_LUA, Long.class);
        Long result = redisTemplate.execute(script, List.of(stockKey, receivedKey), userId.toString());

        if (result == -1) {
            throw new BusinessException("您已领取过该优惠券");
        }
        if (result == 0) {
            throw new BusinessException(BusinessException.ResultCode.COUPON_STOCK_EMPTY);
        }

        // 3. 发送领券消息（异步削峰，由消费者异步入库）
        String requestId = UUID.randomUUID().toString().replace("-", "");
        CouponReceiveMessage message = new CouponReceiveMessage(userId, templateId, requestId);
        couponReceiveProducer.sendCouponReceive(message);

        log.info("User {} receive coupon request accepted, template: {}, requestId: {}",
                userId, templateId, requestId);

        // 4. 返回受理结果（券实际到账由 MQ 消费异步完成）
        Map<String, Object> response = new HashMap<>();
        response.put("requestId", requestId);
        response.put("templateName", template.getName());
        response.put("message", "领取成功，优惠券即将到账");
        return response;
    }

    @Override
    public List<CouponInstance> getUserCoupons(Long userId, Integer status) {
        LambdaQueryWrapper<CouponInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponInstance::getUserId, userId);
        if (status != null) {
            wrapper.eq(CouponInstance::getStatus, status);
        }
        wrapper.orderByDesc(CouponInstance::getReceiveTime);
        return couponInstanceMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public Map<String, Object> useCoupon(Long userId, Long couponId, Long orderAmount) {
        CouponInstance instance = couponInstanceMapper.selectById(couponId);
        if (instance == null || !instance.getUserId().equals(userId)) {
            throw new BusinessException("优惠券不存在");
        }
        if (instance.getStatus() == 1) {
            throw new BusinessException(BusinessException.ResultCode.COUPON_USED);
        }
        if (instance.getStatus() == 2 || instance.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(BusinessException.ResultCode.COUPON_EXPIRED);
        }

        CouponTemplate template = couponTemplateMapper.selectById(instance.getTemplateId());
        if (template == null) {
            throw new BusinessException("优惠券模板不存在");
        }

        // Check threshold
        if (orderAmount < template.getThreshold()) {
            throw new BusinessException("订单金额不满足使用门槛");
        }

        // Calculate discount
        long discount = 0;
        switch (template.getType()) {
            case "full_subtract":
            case "cash":
                discount = template.getFaceValue();
                break;
            case "discount":
                discount = orderAmount * template.getFaceValue() / 100;
                break;
            default:
                discount = template.getFaceValue();
        }

        instance.setStatus(1);
        instance.setUseTime(LocalDateTime.now());
        couponInstanceMapper.updateById(instance);

        Map<String, Object> result = new HashMap<>();
        result.put("couponId", couponId);
        result.put("discount", discount);
        result.put("message", "使用成功");
        log.info("User {} used coupon {}, discount: {}", userId, couponId, discount);
        return result;
    }

    @Override
    @Transactional
    public void lockCoupon(Long couponId) {
        CouponInstance instance = couponInstanceMapper.selectById(couponId);
        if (instance == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (instance.getStatus() != 0) {
            throw new BusinessException("优惠券状态不正确");
        }
        instance.setStatus(3);
        couponInstanceMapper.updateById(instance);
        log.info("Locked coupon: {}", couponId);
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 2 * * ?")
    public void expireCoupons() {
        LambdaQueryWrapper<CouponInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponInstance::getStatus, 0)
               .lt(CouponInstance::getExpireTime, LocalDateTime.now());
        List<CouponInstance> expiredList = couponInstanceMapper.selectList(wrapper);

        for (CouponInstance instance : expiredList) {
            instance.setStatus(2);
            couponInstanceMapper.updateById(instance);
        }
        log.info("Expired {} coupons", expiredList.size());
    }

    @Override
    public List<CouponTemplate> getAvailableTemplates(int page, int size) {
        LambdaQueryWrapper<CouponTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponTemplate::getEnabled, 1)
               .gt(CouponTemplate::getRemainingStock, 0);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<CouponTemplate> result =
                couponTemplateMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), wrapper);
        return result.getRecords();
    }

    @Override
    public CouponTemplate createTemplate(CouponTemplate template) {
        template.setRemainingStock(template.getTotalStock());
        couponTemplateMapper.insert(template);
        // 初始化Redis库存
        redisTemplate.opsForValue().set(COUPON_STOCK_KEY + template.getId(), String.valueOf(template.getTotalStock()));
        return template;
    }

    @Override
    public CouponTemplate updateTemplate(CouponTemplate template) {
        couponTemplateMapper.updateById(template);
        return template;
    }

    @Override
    public void deleteTemplate(Long id) {
        couponTemplateMapper.deleteById(id);
    }
}
