package com.bunny.flix.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 * 用来生成、解析、验证用户的登录令牌（Token）
 * Token 里保存了用户ID、手机号、用户类型等信息
 */
@Slf4j
public class JwtUtils {

    /** JWT 签名密钥，生产环境一定要改成复杂密钥 */
    @Value("${jwt.secret:bunny-flix-secret-key-default-change-in-production}")
    private String secret;

    /** 普通Token过期时间，默认2小时（7200000毫秒） */
    @Value("${jwt.expiration:7200000}")
    private Long expiration;

    /** 刷新Token过期时间，默认7天（604800000毫秒） */
    @Value("${jwt.refresh-expiration:604800000}")
    private Long refreshExpiration;

    /** 获取签名密钥，要求至少32字节 */
    private SecretKey getSecretKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成登录Token
     * @param userId 用户ID
     * @param phone 手机号
     * @param userType 用户类型（普通用户/VIP等）
     */
    public String generateToken(Long userId, String phone, String userType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("phone", phone);
        claims.put("userType", userType);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 生成刷新Token
     * 当普通Token过期后，用刷新Token换一个新的普通Token，不用重新登录
     */
    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** 解析Token，获取里面存储的所有信息 */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("Token已过期");
        } catch (JwtException e) {
            throw new RuntimeException("Token解析失败");
        }
    }

    /** 从Token中提取用户ID */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return Long.valueOf(claims.getSubject());
    }

    /** 从Token中提取手机号 */
    public String getPhone(String token) {
        return parseToken(token).get("phone", String.class);
    }

    /** 从Token中提取用户类型 */
    public String getUserType(String token) {
        return parseToken(token).get("userType", String.class);
    }

    /** 验证Token是否有效（有没有过期、签名对不对） */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
