package com.bunny.flix.drama;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 短剧服务启动类
 * 负责短剧的增删改查、剧集管理、演员和标签管理
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.bunny.flix.feign")
@MapperScan("com.bunny.flix.drama.mapper")
public class DramaApplication {

    public static void main(String[] args) {
        SpringApplication.run(DramaApplication.class, args);
    }
}
