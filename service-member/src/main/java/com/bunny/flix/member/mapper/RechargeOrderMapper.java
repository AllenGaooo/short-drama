package com.bunny.flix.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.member.entity.RechargeOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 充值订单数据访问层
 */
@Mapper
public interface RechargeOrderMapper extends BaseMapper<RechargeOrder> {
}
