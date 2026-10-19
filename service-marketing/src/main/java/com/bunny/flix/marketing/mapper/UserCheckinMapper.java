package com.bunny.flix.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.marketing.entity.UserCheckin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户签到数据访问层
 */
@Mapper
public interface UserCheckinMapper extends BaseMapper<UserCheckin> {
}
