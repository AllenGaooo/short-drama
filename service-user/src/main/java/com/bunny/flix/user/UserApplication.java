package com.bunny.flix.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 用户服务启动类
 * 负责用户注册登录、个人信息管理、关注功能
 */
@SpringBootApplication
@EnableDiscoveryClient  // 注册到 Nacos 服务发现
@EnableFeignClients(basePackages = "com.bunny.flix.feign")  // 启用 Feign 远程调用
@MapperScan("com.bunny.flix.user.mapper")  // 扫描 MyBatis Mapper
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
