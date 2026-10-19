package com.bunny.flix.marketing.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.marketing.entity.CouponTemplate;

public interface CouponTemplateService {

    CouponTemplate create(CouponTemplate template);

    CouponTemplate update(CouponTemplate template);

    void delete(Long id);

    CouponTemplate getById(Long id);

    Page<CouponTemplate> list(Integer page, Integer size);

    void enable(Long id);

    void disable(Long id);

    Page<CouponTemplate> getAvailableTemplates(Integer page, Integer size);
}
