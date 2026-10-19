package com.bunny.flix.user.service.impl;

import com.bunny.flix.core.exception.BusinessException;
import com.bunny.flix.user.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 短信验证码服务实现
 * 使用 Redis 存储验证码，支持发送频率限制和过期时间
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final StringRedisTemplate stringRedisTemplate;

    /** Redis 中存储验证码的 Key 前缀 */
    private static final String SMS_CODE_PREFIX = "sms:code:";
    /** 验证码有效期：5分钟 */
    private static final long CODE_EXPIRE_MINUTES = 5;
    /** 发送间隔：60秒内不能重复发送 */
    private static final long SEND_INTERVAL_SECONDS = 60;

    @Override
    public void sendCode(String phone) {
        // 检查发送间隔，防止频繁发送
        String intervalKey = "sms:interval:" + phone;
        String intervalValue = stringRedisTemplate.opsForValue().get(intervalKey);
        if (intervalValue != null) {
            throw new BusinessException(400, "发送过于频繁，请60秒后再试");
        }

        // 生成6位随机验证码
        String code = String.format("%06d", (int) (Math.random() * 1000000));

        // 验证码存入 Redis，5分钟后自动过期
        String codeKey = SMS_CODE_PREFIX + phone;
        stringRedisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 设置发送间隔标记，60秒后才能再次发送
        stringRedisTemplate.opsForValue().set(intervalKey, "1", SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // 生产环境应接入真实短信服务商（如阿里云短信）
        log.info("验证码已发送至手机号 {}: {}", phone, code);
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        String codeKey = SMS_CODE_PREFIX + phone;
        String storedCode = stringRedisTemplate.opsForValue().get(codeKey);

        if (storedCode == null) {
            throw new BusinessException(400, "验证码已过期，请重新获取");
        }

        if (!storedCode.equals(code)) {
            throw new BusinessException(400, "验证码错误");
        }

        // 验证成功后删除验证码，防止重复使用
        stringRedisTemplate.delete(codeKey);
        return true;
    }
}
