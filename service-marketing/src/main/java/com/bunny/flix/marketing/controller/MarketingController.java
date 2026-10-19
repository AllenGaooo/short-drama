package com.bunny.flix.marketing.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.marketing.entity.*;
import com.bunny.flix.marketing.service.CheckinService;
import com.bunny.flix.marketing.service.CouponService;
import com.bunny.flix.marketing.service.TaskService;
import com.bunny.flix.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marketing")
@RequiredArgsConstructor
public class MarketingController {

    private final CheckinService checkinService;
    private final TaskService taskService;
    private final CouponService couponService;

    // === 签到 ===
    @PostMapping("/checkin")
    public Result<Map<String, Object>> checkin() {
        Long userId = UserContext.getUserId();
        int reward = checkinService.checkin(userId);
        return Result.success(Map.of("reward", reward));
    }

    @GetMapping("/checkin/calendar")
    public Result<List<UserCheckin>> getCalendar(
            @RequestParam int year,
            @RequestParam int month) {
        return Result.success(checkinService.getCheckinCalendar(UserContext.getUserId(), year, month));
    }

    // === 任务 ===
    @GetMapping("/task/list")
    public Result<List<Task>> getTasks() {
        return Result.success(taskService.getEnabledTasks());
    }

    @GetMapping("/task/my")
    public Result<List<UserTask>> getMyTasks() {
        return Result.success(taskService.getUserTasks(UserContext.getUserId()));
    }

    @PostMapping("/task/claim/{taskId}")
    public Result<Map<String, Object>> claimReward(@PathVariable Long taskId) {
        int reward = taskService.claimReward(UserContext.getUserId(), taskId);
        return Result.success(Map.of("reward", reward));
    }

    @PostMapping("/task")
    public Result<Task> createTask(@RequestBody Task task) {
        return Result.success(taskService.createTask(task));
    }

    // === 优惠券 ===
    @PostMapping("/coupon/receive/{templateId}")
    public Result<Map<String, Object>> receiveCoupon(@PathVariable Long templateId) {
        return Result.success(couponService.receiveCoupon(UserContext.getUserId(), templateId));
    }

    @GetMapping("/coupon/list")
    public Result<List<CouponInstance>> getUserCoupons(
            @RequestParam(required = false) Integer status) {
        return Result.success(couponService.getUserCoupons(UserContext.getUserId(), status));
    }

    @PostMapping("/coupon/use/{couponId}")
    public Result<Map<String, Object>> useCoupon(@PathVariable Long couponId,
                                                  @RequestBody Map<String, Long> body) {
        Map<String, Object> result = couponService.useCoupon(UserContext.getUserId(), couponId,
                body.getOrDefault("orderAmount", 0L));
        return Result.success(result);
    }

    // === 券模板管理 ===
    @GetMapping("/coupon/template/list")
    public Result<List<CouponTemplate>> getTemplates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(couponService.getAvailableTemplates(page, size));
    }

    @PostMapping("/coupon/template")
    public Result<CouponTemplate> createTemplate(@RequestBody CouponTemplate template) {
        return Result.success(couponService.createTemplate(template));
    }

    @PutMapping("/coupon/template/{id}")
    public Result<CouponTemplate> updateTemplate(@PathVariable Long id, @RequestBody CouponTemplate template) {
        template.setId(id);
        return Result.success(couponService.updateTemplate(template));
    }

    @DeleteMapping("/coupon/template/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        couponService.deleteTemplate(id);
        return Result.success();
    }
}
