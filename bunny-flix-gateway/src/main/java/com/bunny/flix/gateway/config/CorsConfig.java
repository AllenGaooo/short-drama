package com.bunny.flix.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * 跨域配置
 * 前端（比如 localhost:3000）和后端（比如 localhost:8080）不在同一个域名，
 * 浏览器默认会阻止跨域请求，这个配置就是告诉浏览器"允许跨域"
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许所有来源访问（生产环境建议限制具体域名）
        config.setAllowedOriginPatterns(Arrays.asList("*"));
        // 允许的请求方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许所有请求头
        config.setAllowedHeaders(Arrays.asList("*"));
        // 允许携带 Cookie 等认证信息
        config.setAllowCredentials(true);
        // 预检请求的缓存时间（秒），1小时内相同请求不需要再发预检
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
