package com.bunny.flix.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.member.entity.WalletTransaction;
import org.apache.ibatis.annotations.Mapper;

/**
 * 钱包交易流水数据访问层
 */
@Mapper
public interface WalletTransactionMapper extends BaseMapper<WalletTransaction> {
}
