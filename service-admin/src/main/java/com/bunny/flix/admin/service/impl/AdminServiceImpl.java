package com.bunny.flix.admin.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bunny.flix.core.exception.BusinessException;
import com.bunny.flix.feign.idgenerator.BizTypeEnum;
import com.bunny.flix.feign.idgenerator.IdGeneratorFeignClient;
import com.bunny.flix.security.JwtUtils;
import com.bunny.flix.admin.entity.AdminMenu;
import com.bunny.flix.admin.entity.AdminUser;
import com.bunny.flix.admin.mapper.AdminMenuMapper;
import com.bunny.flix.admin.mapper.AdminUserMapper;
import com.bunny.flix.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminUserMapper adminUserMapper;
    private final AdminMenuMapper adminMenuMapper;
    private final JwtUtils jwtUtils;
    private final IdGeneratorFeignClient idGeneratorFeignClient;

    @Override
    @Transactional
    public AdminUser register(String phone, String password) {
        LambdaQueryWrapper<AdminUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminUser::getPhone, phone);
        if (adminUserMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(BusinessException.ResultCode.CONFLICT.getCode(), "管理员手机号已注册");
        }

        String salt = RandomUtil.randomString(16);
        String encryptedPassword = DigestUtil.md5Hex(password + salt);

        AdminUser admin = new AdminUser();
        // 调用 id-generator 号段服务分配管理员ID（ADMIN 业务）
        admin.setId(idGeneratorFeignClient.nextId(BizTypeEnum.ADMIN).getData());
        admin.setPhone(phone);
        admin.setPassword(encryptedPassword);
        admin.setSalt(salt);
        admin.setNickname("管理员" + phone.substring(phone.length() - 4));
        admin.setAvatar("https://cdn.bunnyflix.com/default-avatar.png");
        admin.setStatus(0);
        admin.setRole("NORMAL");
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());

        adminUserMapper.insert(admin);
        log.info("管理员注册成功: phone={}, adminId={}", phone, admin.getId());
        return admin;
    }

    @Override
    public String loginByPassword(String phone, String password) {
        LambdaQueryWrapper<AdminUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminUser::getPhone, phone);
        AdminUser admin = adminUserMapper.selectOne(wrapper);

        if (admin == null) {
            throw new BusinessException(BusinessException.ResultCode.USER_NOT_FOUND);
        }
        if (admin.getStatus() == 1) {
            throw new BusinessException(BusinessException.ResultCode.USER_DISABLED);
        }
        if (admin.getStatus() == 2) {
            throw new BusinessException(BusinessException.ResultCode.USER_DISABLED.getCode(), "账号已注销");
        }

        String encryptedPassword = DigestUtil.md5Hex(password + admin.getSalt());
        if (!encryptedPassword.equals(admin.getPassword())) {
            throw new BusinessException(401, "密码错误");
        }

        // userType 固定为 admin，与普通用户区分
        String token = jwtUtils.generateToken(admin.getId(), admin.getPhone(), "admin");
        log.info("管理员密码登录成功: adminId={}", admin.getId());
        return token;
    }

    @Override
    public AdminUser getById(Long adminId) {
        AdminUser admin = adminUserMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(BusinessException.ResultCode.USER_NOT_FOUND);
        }
        admin.setPassword(null);
        admin.setSalt(null);
        return admin;
    }

    @Override
    public List<AdminMenu> listMenus(Long adminId) {
        LambdaQueryWrapper<AdminMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminMenu::getUserId, adminId)
               .orderByAsc(AdminMenu::getSort);
        return adminMenuMapper.selectList(wrapper);
    }
}
