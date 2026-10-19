package com.bunny.flix.search;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 搜索服务启动类
 * 基于 Elasticsearch 提供短剧全文搜索、搜索联想、热搜词管理
 * 通过 RocketMQ 同步短剧数据到 ES
 */
// 排除 Boot 3.0.2 自带的 ES 自动配置（其按 8.5.x 客户端编译，与 8.14.3 的 RestClientTransport 构造器不兼容），
// ES 客户端由 ElasticsearchConfig 手动装配
@SpringBootApplication(exclude = {
        ElasticsearchClientAutoConfiguration.class,
        ElasticsearchRestClientAutoConfiguration.class
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.bunny.flix.feign")
@MapperScan("com.bunny.flix.search.mapper")
public class SearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchApplication.class, args);
    }
}
