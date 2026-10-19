package com.bunny.flix.member.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.member.entity.RechargeOrder;
import com.bunny.flix.member.service.RechargeService;
import com.bunny.flix.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/member/recharge")
@RequiredArgsConstructor
public class RechargeController {

    private final RechargeService rechargeService;

    @PostMapping("/order")
    public Result<RechargeOrder> createRechargeOrder(@RequestBody Map<String, Long> request) {
        Long userId = UserContext.getUserId();
        Long amount = request.get("amount");
        RechargeOrder order = rechargeService.createRechargeOrder(userId, amount);
        return Result.success(order);
    }

    @PostMapping("/callback")
    public Result<Void> handlePayCallback(@RequestBody Map<String, Object> request) {
        String orderNo = (String) request.get("orderNo");
        boolean success = (boolean) request.get("success");
        rechargeService.handlePayCallback(orderNo, success);
        return Result.success();
    }

    @GetMapping("/orders")
    public Result<List<RechargeOrder>> getRechargeOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = UserContext.getUserId();
        List<RechargeOrder> orders = rechargeService.getRechargeOrders(userId, page, size);
        return Result.success(orders);
    }
}
