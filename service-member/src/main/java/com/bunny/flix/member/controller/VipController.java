package com.bunny.flix.member.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.member.entity.VipPlan;
import com.bunny.flix.member.entity.VipRecord;
import com.bunny.flix.member.service.VipService;
import com.bunny.flix.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/member/vip")
@RequiredArgsConstructor
public class VipController {

    private final VipService vipService;

    @GetMapping("/status")
    public Result<VipRecord> getUserVipStatus() {
        Long userId = UserContext.getUserId();
        VipRecord record = vipService.getUserVipStatus(userId);
        return Result.success(record);
    }

    @GetMapping("/plans")
    public Result<List<VipPlan>> getVipPlans() {
        List<VipPlan> plans = vipService.getVipPlans();
        return Result.success(plans);
    }

    @PostMapping("/purchase")
    public Result<Map<String, String>> purchaseVip(@RequestBody Map<String, Long> request) {
        Long userId = UserContext.getUserId();
        Long planId = request.get("planId");
        String orderNo = vipService.purchaseVip(userId, planId);
        return Result.success(Map.of("orderNo", orderNo));
    }
}
