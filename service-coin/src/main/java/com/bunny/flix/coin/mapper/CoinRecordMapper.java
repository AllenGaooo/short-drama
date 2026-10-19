package com.bunny.flix.coin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.coin.entity.CoinRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 金币流水数据访问层
 */
@Mapper
public interface CoinRecordMapper extends BaseMapper<CoinRecord> {
}
