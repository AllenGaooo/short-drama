package com.bunny.flix.marketing.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.marketing.entity.CouponInstance;
import com.bunny.flix.marketing.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marketing/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/receive/{templateId}")
    public Result<Map<String, Object>> receiveCoupon(
            @RequestParam Long userId,
            @PathVariable Long templateId) {
        return Result.success(couponService.receiveCoupon(userId, templateId));
    }

    @GetMapping
    public Result<List<CouponInstance>> getUserCoupons(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer status) {
        return Result.success(couponService.getUserCoupons(userId, status));
    }

    @PostMapping("/use/{couponId}")
    public Result<Map<String, Object>> useCoupon(
            @RequestParam Long userId,
            @PathVariable Long couponId,
            @RequestParam Long orderAmount) {
        return Result.success(couponService.useCoupon(userId, couponId, orderAmount));
    }
}
