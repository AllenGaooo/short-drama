package com.bunny.flix.admin.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.security.JwtUtils;
import com.bunny.flix.security.UserContext;
import com.bunny.flix.admin.entity.AdminMenu;
import com.bunny.flix.admin.entity.AdminUser;
import com.bunny.flix.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理控制器
 * 独立于 UserController，所有接口走 /api/admin/**
 * 提供管理员注册、登录、菜单查询
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final JwtUtils jwtUtils;

    /** 管理员密码登录 */
    @PostMapping("/login/password")
    public Result<Map<String, Object>> loginByPassword(@RequestParam String phone,
                                                        @RequestParam String password) {
        String token = adminService.loginByPassword(phone, password);
        Long adminId = jwtUtils.getUserId(token);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", adminId);
        return Result.success("登录成功", data);
    }

    /** 管理员手机号+密码注册 */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestParam String phone,
                                                 @RequestParam String password) {
        AdminUser admin = adminService.register(phone, password);
        String token = jwtUtils.generateToken(admin.getId(), admin.getPhone(), "admin");

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", admin.getId());
        return Result.success("注册成功", data);
    }

    /** 获取当前登录管理员信息 */
    @GetMapping("/me")
    public Result<AdminUser> getCurrentAdmin() {
        Long adminId = UserContext.getUserId();
        return Result.success(adminService.getById(adminId));
    }

    /** 获取当前登录管理员的分级菜单 */
    @GetMapping("/menus")
    public Result<List<AdminMenu>> getMenus() {
        Long adminId = UserContext.getUserId();
        return Result.success(adminService.listMenus(adminId));
    }
}
