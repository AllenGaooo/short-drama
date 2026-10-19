package com.bunny.flix.marketing.service;

import com.bunny.flix.marketing.entity.CouponInstance;
import com.bunny.flix.marketing.entity.CouponTemplate;

import java.util.List;
import java.util.Map;

/**
 * 优惠券服务接口
 */
public interface CouponService {

    /**
     * 领取优惠券
     */
    Map<String, Object> receiveCoupon(Long userId, Long templateId);

    /**
     * 查询用户优惠券
     */
    List<CouponInstance> getUserCoupons(Long userId, Integer status);

    /**
     * 使用优惠券
     */
    Map<String, Object> useCoupon(Long userId, Long couponId, Long orderAmount);

    /**
     * 锁定优惠券（订单冻结）
     */
    void lockCoupon(Long couponId);

    /**
     * 定时清理过期优惠券
     */
    void expireCoupons();

    /**
     * 查询可用券模板
     */
    List<CouponTemplate> getAvailableTemplates(int page, int size);

    /**
     * 创建券模板
     */
    CouponTemplate createTemplate(CouponTemplate template);

    /**
     * 更新券模板
     */
    CouponTemplate updateTemplate(CouponTemplate template);

    /**
     * 删除券模板
     */
    void deleteTemplate(Long id);
}
