package com.bunny.flix.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.marketing.entity.CouponInstance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券实例数据访问层
 */
@Mapper
public interface CouponInstanceMapper extends BaseMapper<CouponInstance> {
}
