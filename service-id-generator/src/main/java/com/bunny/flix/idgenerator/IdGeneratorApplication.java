package com.bunny.flix.idgenerator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 分布式ID生成服务启动类
 * 使用美团 Leaf 号段模式，为各业务模块提供全局唯一ID
 */
@SpringBootApplication
@EnableDiscoveryClient
public class IdGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdGeneratorApplication.class, args);
    }
}
