package com.bunny.flix.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
public class BunnyFlixAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(BunnyFlixAgentApplication.class, args);
    }

    /** 启用负载均衡，下游可用 lb://服务名 调用 */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
