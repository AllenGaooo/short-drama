package com.bunny.flix.admin.service;

import com.bunny.flix.admin.entity.AdminMenu;
import com.bunny.flix.admin.entity.AdminUser;

import java.util.List;

/**
 * 后台管理服务接口
 * 定义管理员注册、登录、菜单查询等核心操作
 */
public interface AdminService {

    /** 手机号+密码注册管理员，返回管理员实体 */
    AdminUser register(String phone, String password);

    /** 密码登录，返回JWT Token */
    String loginByPassword(String phone, String password);

    /** 根据管理员ID获取信息（隐藏敏感字段） */
    AdminUser getById(Long adminId);

    /** 查询某管理员的分级菜单列表 */
    List<AdminMenu> listMenus(Long adminId);
}
