package com.bunny.flix.marketing.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.core.Result;
import com.bunny.flix.marketing.entity.CouponTemplate;
import com.bunny.flix.marketing.service.CouponTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/marketing/coupon/template")
@RequiredArgsConstructor
public class CouponTemplateController {

    private final CouponTemplateService couponTemplateService;

    @PostMapping
    public Result<CouponTemplate> create(@RequestBody CouponTemplate template) {
        return Result.success(couponTemplateService.create(template));
    }

    @PutMapping("/{id}")
    public Result<CouponTemplate> update(@PathVariable Long id, @RequestBody CouponTemplate template) {
        template.setId(id);
        return Result.success(couponTemplateService.update(template));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponTemplateService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<CouponTemplate> getById(@PathVariable Long id) {
        return Result.success(couponTemplateService.getById(id));
    }

    @GetMapping
    public Result<Page<CouponTemplate>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(couponTemplateService.list(page, size));
    }

    @PutMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        couponTemplateService.enable(id);
        return Result.success();
    }

    @PutMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        couponTemplateService.disable(id);
        return Result.success();
    }

    @GetMapping("/available")
    public Result<Page<CouponTemplate>> getAvailableTemplates(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(couponTemplateService.getAvailableTemplates(page, size));
    }
}
