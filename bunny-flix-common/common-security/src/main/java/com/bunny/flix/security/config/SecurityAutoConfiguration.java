package com.bunny.flix.security.config;

import com.bunny.flix.security.JwtUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * common-security 模块的自动配置。
 * 任何引入 common-security 的 Spring Boot 应用都会自动获得 JwtUtils Bean，
 * 无需在各微服务主类上额外加 @ComponentScan("com.bunny.flix.security")。
 */
@Configuration
public class SecurityAutoConfiguration {

    @Bean
    public JwtUtils jwtUtils() {
        return new JwtUtils();
    }
}
