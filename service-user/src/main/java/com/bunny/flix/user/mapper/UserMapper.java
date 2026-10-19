package com.bunny.flix.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问层
 * 继承 MyBatis-Plus 的 BaseMapper，自带增删改查方法
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
