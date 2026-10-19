package com.bunny.flix.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.member.entity.VipOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * VIP订单数据访问层
 */
@Mapper
public interface VipOrderMapper extends BaseMapper<VipOrder> {
}
