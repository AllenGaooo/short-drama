package com.bunny.flix.push;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 推送服务启动类
 * 负责站内通知和App推送，通过 RocketMQ 监听互动事件自动生成通知
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.bunny.flix.push.mapper")
public class PushApplication {

    public static void main(String[] args) {
        SpringApplication.run(PushApplication.class, args);
    }
}
