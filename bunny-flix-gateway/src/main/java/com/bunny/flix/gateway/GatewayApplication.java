package com.bunny.flix.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;

/**
 * 网关服务启动类
 * 网关是所有请求的入口，负责：
 * 1. 路由转发：把请求转发到对应的微服务
 * 2. 登录鉴权：验证Token，白名单路径跳过
 * 3. 限流熔断：通过 Sentinel 保护后端服务
 * 4. 跨域处理：允许前端跨域访问
 */
// 网关不需要数据库，排除数据源及 MyBatis 自动配置（common-core 传递引入了这些依赖）
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class
})
@EnableDiscoveryClient  // 注册到 Nacos，让网关能发现其他服务
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
