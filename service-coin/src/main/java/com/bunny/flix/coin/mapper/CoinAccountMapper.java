package com.bunny.flix.coin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.coin.entity.CoinAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * 金币账户数据访问层
 */
@Mapper
public interface CoinAccountMapper extends BaseMapper<CoinAccount> {
}
