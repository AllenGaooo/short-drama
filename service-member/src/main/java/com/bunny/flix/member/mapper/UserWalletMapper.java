package com.bunny.flix.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.member.entity.UserWallet;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户钱包数据访问层
 */
@Mapper
public interface UserWalletMapper extends BaseMapper<UserWallet> {
}
