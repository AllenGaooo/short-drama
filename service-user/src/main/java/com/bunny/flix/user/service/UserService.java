package com.bunny.flix.user.service;

import com.bunny.flix.feign.user.dto.VipStatusDTO;
import com.bunny.flix.user.entity.User;

/**
 * 用户服务接口
 * 定义用户相关的核心业务操作：注册、登录、信息管理、VIP状态更新
 */
public interface UserService {

    /** 手机号+密码注册 */
    User register(String phone, String password);

    /** 密码登录，返回JWT Token */
    String loginByPassword(String phone, String password);

    /** 验证码登录（用户不存在则自动注册），返回JWT Token */
    String loginByCode(String phone, String code);

    /** 根据ID获取用户 */
    User getUserById(Long userId);

    /** 更新用户信息 */
    void updateUser(User user);

    /** 修改密码 */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /** 更新VIP状态 */
    void updateVipStatus(VipStatusDTO vipStatusDTO);
}
