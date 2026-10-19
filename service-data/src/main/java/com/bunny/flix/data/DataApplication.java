package com.bunny.flix.data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 数据服务启动类
 * 负责播放数据统计、热门排行计算，通过定时任务定期刷新排行榜
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class DataApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataApplication.class, args);
    }
}
