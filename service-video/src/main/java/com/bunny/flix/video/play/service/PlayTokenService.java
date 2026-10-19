package com.bunny.flix.video.play.service;

import cn.hutool.crypto.SecureUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 临时播放凭证服务（CDN 防盗链）
 *
 * 设计：token = MD5(userId + episodeId + expireTime + secret)
 * 播放地址形如：https://cdn.xxx.com/video/a.m3u8?token=xxx&expire=xxx
 * CDN 边缘节点或本服务回源时校验 token 与过期时间，防止用户复制裸地址传播。
 */
@Slf4j
@Service
public class PlayTokenService {

    private final StringRedisTemplate redisTemplate;

    /** 签名密钥（生产环境应放在 Nacos / 环境变量，不要硬编码） */
    @Value("${play.token.secret:bunny-flix-play-secret}")
    private String secret;

    /** 凭证有效期（秒），默认 10 分钟 */
    @Value("${play.token.expire-seconds:600}")
    private long expireSeconds;

    public PlayTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 生成临时播放凭证
     *
     * @param userId    用户ID
     * @param episodeId 剧集ID
     * @return 凭证与有效期
     */
    public TokenResult generate(Long userId, Long episodeId) {
        long expireAt = System.currentTimeMillis() / 1000 + expireSeconds;
        String token = sign(userId, episodeId, expireAt);

        // Redis 记录凭证（用于主动失效与统计），过期时间与凭证一致
        try {
            redisTemplate.opsForValue().set(tokenKey(userId, episodeId, expireAt), "1",
                    expireSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Redis 不可用不阻断发凭证，仅记录日志（无状态校验仍可工作）
            log.warn("[play] 播放凭证写入Redis失败，降级为无状态校验: {}", e.getMessage());
        }
        return new TokenResult(token, expireAt, expireSeconds);
    }

    /**
     * 校验凭证（CDN 回源校验 / 内部校验复用）
     */
    public boolean verify(Long userId, Long episodeId, Long expireAt, String token) {
        if (token == null || expireAt == null) {
            return false;
        }
        // 1) 过期校验
        if (System.currentTimeMillis() / 1000 > expireAt) {
            log.debug("[play] 播放凭证已过期 userId={} episodeId={}", userId, episodeId);
            return false;
        }
        // 2) 签名校验（防篡改）
        String expected = sign(userId, episodeId, expireAt);
        return expected.equals(token);
    }

    /** 拼接带防盗链参数的播放地址 */
    public String buildPlayUrl(String baseUrl, Long userId, Long episodeId, String token, long expireAt) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        String sep = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + sep + "token=" + token + "&expire=" + expireAt + "&uid=" + userId;
    }

    /** 签名算法：MD5(userId + episodeId + expireTime + secret) */
    private String sign(Long userId, Long episodeId, long expireAt) {
        String raw = userId + "|" + episodeId + "|" + expireAt + "|" + secret;
        return SecureUtil.md5(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String tokenKey(Long userId, Long episodeId, long expireAt) {
        return "play:token:" + userId + ":" + episodeId + ":" + expireAt;
    }

    /** 生成结果 */
    public record TokenResult(String token, long expireAt, long expireSeconds) {
    }
}
