package com.bunny.flix.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.admin.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 后台管理员用户 Mapper
 */
@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {
}
