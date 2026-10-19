package com.bunny.flix.user.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.feign.user.dto.UserVipDTO;
import com.bunny.flix.feign.user.dto.VipStatusDTO;
import com.bunny.flix.security.JwtUtils;
import com.bunny.flix.security.UserContext;
import com.bunny.flix.user.entity.User;
import com.bunny.flix.user.service.SmsService;
import com.bunny.flix.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 * 提供注册、登录（密码/验证码）、个人信息管理、VIP状态相关接口
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SmsService smsService;
    private final JwtUtils jwtUtils;

    /** 密码登录 */
    @PostMapping("/login/password")
    public Result<Map<String, Object>> loginByPassword(@RequestParam String phone,
                                                        @RequestParam String password) {
        String token = userService.loginByPassword(phone, password);
        Long userId = jwtUtils.getUserId(token);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", userId);
        return Result.success("登录成功", data);
    }

    /** 验证码登录（先验证短信验证码，再登录或自动注册） */
    @PostMapping("/login/code")
    public Result<Map<String, Object>> loginByCode(@RequestParam String phone,
                                                    @RequestParam String code) {
        // 先校验短信验证码
        smsService.verifyCode(phone, code);
        String token = userService.loginByCode(phone, code);
        Long userId = jwtUtils.getUserId(token);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", userId);
        return Result.success("登录成功", data);
    }

    /** 手机号+密码注册 */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestParam String phone,
                                                 @RequestParam String password) {
        User user = userService.register(phone, password);
        String token = jwtUtils.generateToken(user.getId(), user.getPhone(), "user");

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        return Result.success("注册成功", data);
    }

    /** 发送短信验证码 */
    @PostMapping("/sms/send")
    public Result<Void> sendSmsCode(@RequestParam String phone) {
        smsService.sendCode(phone);
        return Result.success("验证码发送成功");
    }

    /** 根据ID查询用户 */
    @GetMapping("/{userId}")
    public Result<User> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return Result.success(user);
    }

    /** 修改个人资料（昵称、头像、签名等） */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody User user) {
        Long currentUserId = UserContext.getUserId();
        user.setId(currentUserId);
        userService.updateUser(user);
        return Result.success("个人资料更新成功");
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestParam String oldPassword,
                                        @RequestParam String newPassword) {
        Long currentUserId = UserContext.getUserId();
        userService.changePassword(currentUserId, oldPassword, newPassword);
        return Result.success("密码修改成功");
    }

    /** 获取当前登录用户信息 */
    @GetMapping("/me")
    public Result<User> getCurrentUser() {
        Long currentUserId = UserContext.getUserId();
        User user = userService.getUserById(currentUserId);
        return Result.success(user);
    }

    /** 更新VIP状态（由会员服务调用） */
    @PutMapping("/vip/status")
    public Result<Void> updateVipStatus(@RequestBody VipStatusDTO vipStatusDTO) {
        userService.updateVipStatus(vipStatusDTO);
        return Result.success("VIP状态更新成功");
    }

    /** 查询用户VIP信息 */
    @GetMapping("/{userId}/vip")
    public Result<UserVipDTO> getUserVipInfo(@PathVariable Long userId) {
        User user = userService.getUserById(userId);

        UserVipDTO vipDTO = new UserVipDTO();
        vipDTO.setUserId(user.getId());
        vipDTO.setVipStatus(user.getVipStatus());
        vipDTO.setVipExpireTime(user.getVipExpireTime() != null ? user.getVipExpireTime().toString() : null);
        vipDTO.setNickname(user.getNickname());

        return Result.success(vipDTO);
    }

    /** 判断用户是否存在 */
    @GetMapping("/{userId}/exists")
    public Result<Boolean> userExists(@PathVariable Long userId) {
        try {
            userService.getUserById(userId);
            return Result.success(true);
        } catch (Exception e) {
            return Result.success(false);
        }
    }
}
