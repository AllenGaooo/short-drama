package com.bunny.flix.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.core.exception.BusinessException;
import com.bunny.flix.marketing.entity.CouponTemplate;
import com.bunny.flix.marketing.mapper.CouponTemplateMapper;
import com.bunny.flix.marketing.service.CouponTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl implements CouponTemplateService {

    private final CouponTemplateMapper couponTemplateMapper;

    @Override
    public CouponTemplate create(CouponTemplate template) {
        template.setRemainingStock(template.getTotalStock());
        template.setEnabled(1);
        couponTemplateMapper.insert(template);
        log.info("Created coupon template: {}", template.getName());
        return template;
    }

    @Override
    public CouponTemplate update(CouponTemplate template) {
        CouponTemplate existing = couponTemplateMapper.selectById(template.getId());
        if (existing == null) {
            throw new BusinessException(BusinessException.ResultCode.NOT_FOUND);
        }
        couponTemplateMapper.updateById(template);
        return template;
    }

    @Override
    public void delete(Long id) {
        couponTemplateMapper.deleteById(id);
    }

    @Override
    public CouponTemplate getById(Long id) {
        return couponTemplateMapper.selectById(id);
    }

    @Override
    public Page<CouponTemplate> list(Integer page, Integer size) {
        return couponTemplateMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<>());
    }

    @Override
    public void enable(Long id) {
        CouponTemplate template = couponTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(BusinessException.ResultCode.NOT_FOUND);
        }
        template.setEnabled(1);
        couponTemplateMapper.updateById(template);
    }

    @Override
    public void disable(Long id) {
        CouponTemplate template = couponTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(BusinessException.ResultCode.NOT_FOUND);
        }
        template.setEnabled(0);
        couponTemplateMapper.updateById(template);
    }

    @Override
    public Page<CouponTemplate> getAvailableTemplates(Integer page, Integer size) {
        LambdaQueryWrapper<CouponTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponTemplate::getEnabled, 1)
               .gt(CouponTemplate::getRemainingStock, 0);
        return couponTemplateMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
