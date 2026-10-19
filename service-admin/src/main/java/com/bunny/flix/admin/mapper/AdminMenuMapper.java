package com.bunny.flix.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.admin.entity.AdminMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 后台菜单 Mapper
 */
@Mapper
public interface AdminMenuMapper extends BaseMapper<AdminMenu> {
}
