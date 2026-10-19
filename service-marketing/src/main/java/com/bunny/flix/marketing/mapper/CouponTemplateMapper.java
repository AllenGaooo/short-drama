package com.bunny.flix.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.marketing.entity.CouponTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券模板数据访问层
 */
@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {
}
