package com.bunny.flix.coin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 金币服务启动类
 * 消费播放完成事件发放金币奖励，并提供金币账户查询、流水记录能力
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.bunny.flix.feign")
@MapperScan("com.bunny.flix.coin.mapper")
public class CoinApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoinApplication.class, args);
    }
}
