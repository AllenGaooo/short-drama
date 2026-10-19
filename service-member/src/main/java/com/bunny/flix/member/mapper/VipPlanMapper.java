package com.bunny.flix.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.member.entity.VipPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * VIP套餐数据访问层
 */
@Mapper
public interface VipPlanMapper extends BaseMapper<VipPlan> {
}
