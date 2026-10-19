package com.bunny.flix.recommend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 推荐服务启动类
 * 基于用户标签偏好和余弦相似度实现协同过滤推荐
 * 通过定时任务定期刷新推荐结果
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class RecommendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendApplication.class, args);
    }
}
