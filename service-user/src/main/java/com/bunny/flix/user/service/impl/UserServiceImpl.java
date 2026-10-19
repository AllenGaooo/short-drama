package com.bunny.flix.user.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bunny.flix.core.exception.BusinessException;
import com.bunny.flix.feign.idgenerator.BizTypeEnum;
import com.bunny.flix.feign.idgenerator.IdGeneratorFeignClient;
import com.bunny.flix.feign.user.dto.VipStatusDTO;
import cn.hutool.json.JSONUtil;
import com.bunny.flix.security.JwtUtils;
import com.bunny.flix.user.entity.User;
import com.bunny.flix.user.mapper.UserMapper;
import com.bunny.flix.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户服务实现
 * 实现注册、登录（密码/验证码）、用户信息管理、VIP状态更新等核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final IdGeneratorFeignClient idGeneratorFeignClient;

    /** Redis：缓存验证码、用户热点信息 */
    @Autowired(required = false)
    @Qualifier("stringRedisTemplate")
    private StringRedisTemplate stringRedisTemplate;

    /** 用户缓存开关（可配置关闭，便于排查问题） */
    @Value("${user.cache.enabled:true}")
    private boolean cacheEnabled;

    /** 用户缓存前缀与 TTL */
    private static final String USER_CACHE_PREFIX = "user:info:";
    private static final long USER_CACHE_TTL_MINUTES = 30;

    /** 缓存统计：命中数 / 总查询数（用于验证命中率） */
    private final AtomicLong cacheHitCount = new AtomicLong();
    private final AtomicLong cacheQueryCount = new AtomicLong();

    @Override
    @Transactional
    public User register(String phone, String password) {
        // 检查手机号是否已注册
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(BusinessException.ResultCode.CONFLICT.getCode(), "手机号已注册");
        }

        // 生成随机盐值，用 MD5(密码+盐) 的方式加密密码
        String salt = RandomUtil.randomString(16);
        String encryptedPassword = DigestUtil.md5Hex(password + salt);

        User user = new User();
        // 调用 id-generator 号段服务分配用户ID
        user.setId(idGeneratorFeignClient.nextId(BizTypeEnum.USER).getData());
        user.setPhone(phone);
        user.setPassword(encryptedPassword);
        user.setSalt(salt);
        // 默认昵称：用户+手机号后4位
        user.setNickname("用户" + phone.substring(phone.length() - 4));
        user.setAvatar("https://cdn.bunnyflix.com/default-avatar.png");
        user.setGender(0);
        user.setStatus(0);
        user.setVipStatus(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);
        log.info("用户注册成功: phone={}, userId={}", phone, user.getId());
        return user;
    }

    @Override
    public String loginByPassword(String phone, String password) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(BusinessException.ResultCode.USER_NOT_FOUND);
        }

        if (user.getStatus() == 1) {
            throw new BusinessException(BusinessException.ResultCode.USER_DISABLED);
        }

        if (user.getStatus() == 2) {
            throw new BusinessException(BusinessException.ResultCode.USER_DISABLED.getCode(), "用户已注销");
        }

        // 用同样的盐值加密用户输入的密码，然后和数据库中存的加密密码比对
        String encryptedPassword = DigestUtil.md5Hex(password + user.getSalt());
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BusinessException(401, "密码错误");
        }

        // 生成 JWT Token 返回
        String token = jwtUtils.generateToken(user.getId(), user.getPhone(), "user");
        log.info("用户密码登录成功: userId={}", user.getId());
        return token;
    }

    @Override
    public String loginByCode(String phone, String code) {
        // 验证码已在 SmsService 中验证过了
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            // 验证码登录时如果用户不存在，自动帮用户注册
            user = register(phone, RandomUtil.randomString(12));
        }

        if (user.getStatus() == 1) {
            throw new BusinessException(BusinessException.ResultCode.USER_DISABLED);
        }

        if (user.getStatus() == 2) {
            throw new BusinessException(BusinessException.ResultCode.USER_DISABLED.getCode(), "用户已注销");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getPhone(), "user");
        log.info("用户验证码登录成功: userId={}", user.getId());
        return token;
    }

    @Override
    public User getUserById(Long userId) {
        cacheQueryCount.incrementAndGet();
        // 1) 查缓存（热点用户信息），命中直接返回
        User cached = getFromCache(userId);
        if (cached != null) {
            return cached;
        }

        // 2) 未命中查数据库
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(BusinessException.ResultCode.USER_NOT_FOUND);
        }
        // 返回用户信息前隐藏敏感数据（密码、盐值）
        user.setPassword(null);
        user.setSalt(null);

        // 3) 回写缓存
        putToCache(userId, user);
        return user;
    }

    /** 从 Redis 读取用户缓存 */
    private User getFromCache(Long userId) {
        if (!cacheEnabled || stringRedisTemplate == null) {
            return null;
        }
        try {
            String json = stringRedisTemplate.opsForValue().get(userCacheKey(userId));
            if (json != null) {
                cacheHitCount.incrementAndGet();
                return JSONUtil.toBean(json, User.class);
            }
        } catch (Exception e) {
            log.warn("[user] 用户缓存读取失败，降级查库: {}", e.getMessage());
        }
        return null;
    }

    /** 写入用户缓存（TTL 加随机抖动，避免同时失效） */
    private void putToCache(Long userId, User user) {
        if (!cacheEnabled || stringRedisTemplate == null) {
            return;
        }
        try {
            long ttl = USER_CACHE_TTL_MINUTES + (long) (Math.random() * 5);
            stringRedisTemplate.opsForValue()
                    .set(userCacheKey(userId), JSONUtil.toJsonStr(user), ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("[user] 用户缓存写入失败: {}", e.getMessage());
        }
    }

    /** 失效用户缓存（资料更新/改密后调用，保证一致性） */
    private void evictCache(Long userId) {
        if (!cacheEnabled || stringRedisTemplate == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(userCacheKey(userId));
        } catch (Exception e) {
            log.warn("[user] 用户缓存失效失败: {}", e.getMessage());
        }
    }

    private static String userCacheKey(Long userId) {
        return USER_CACHE_PREFIX + userId;
    }

    @Override
    @Transactional
    public void updateUser(User user) {
        User existingUser = userMapper.selectById(user.getId());
        if (existingUser == null) {
            throw new BusinessException(BusinessException.ResultCode.USER_NOT_FOUND);
        }

        existingUser.setNickname(user.getNickname());
        existingUser.setAvatar(user.getAvatar());
        existingUser.setGender(user.getGender());
        existingUser.setBirthday(user.getBirthday());
        existingUser.setSignature(user.getSignature());
        existingUser.setUpdateTime(LocalDateTime.now());

        userMapper.updateById(existingUser);
        log.info("用户信息更新成功: userId={}", user.getId());
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(BusinessException.ResultCode.USER_NOT_FOUND);
        }

        // 先验证旧密码是否正确
        String encryptedOldPassword = DigestUtil.md5Hex(oldPassword + user.getSalt());
        if (!encryptedOldPassword.equals(user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }

        // 生成新的盐值并加密新密码
        String salt = RandomUtil.randomString(16);
        String encryptedNewPassword = DigestUtil.md5Hex(newPassword + salt);
        user.setPassword(encryptedNewPassword);
        user.setSalt(salt);
        user.setUpdateTime(LocalDateTime.now());

        userMapper.updateById(user);
        // 密码变更，必须失效缓存，否则会返回旧密码的缓存数据
        evictCache(userId);
        log.info("用户密码修改成功: userId={}", userId);
    }

    @Override
    @Transactional
    public void updateVipStatus(VipStatusDTO vipStatusDTO) {
        User user = userMapper.selectById(vipStatusDTO.getUserId());
        if (user == null) {
            throw new BusinessException(BusinessException.ResultCode.USER_NOT_FOUND);
        }

        user.setVipStatus(vipStatusDTO.getVipStatus());
        if (vipStatusDTO.getVipExpireTime() != null && !vipStatusDTO.getVipExpireTime().isEmpty()) {
            user.setVipExpireTime(LocalDateTime.parse(vipStatusDTO.getVipExpireTime()));
        }
        user.setUpdateTime(LocalDateTime.now());

        userMapper.updateById(user);
        // VIP 变更失效缓存，避免读到过期会员状态
        evictCache(vipStatusDTO.getUserId());
        log.info("VIP状态更新成功: userId={}, vipStatus={}", vipStatusDTO.getUserId(), vipStatusDTO.getVipStatus());
    }

    /**
     * 缓存命中率统计（用于验证缓存效果）
     */
    public Map<String, Object> cacheMetrics() {
        long query = cacheQueryCount.get();
        long hit = cacheHitCount.get();
        double rate = query == 0 ? 0 : hit * 100.0 / query;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("queryCount", query);
        m.put("hitCount", hit);
        m.put("hitRate", Math.round(rate * 100.0) / 100.0 + "%");
        m.put("cacheEnabled", cacheEnabled);
        return m;
    }
}
