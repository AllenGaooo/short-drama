package com.bunny.flix.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;

/**
 * Sentinel 限流配置
 * Sentinel 是阿里的流量控制组件，当请求太多时自动拦截，保护后端服务不被压垮
 * 这里配置被限流后的返回信息
 */
@Configuration
public class SentinelConfig {

    /**
     * 初始化限流处理回调
     * 当请求被限流拦截时，返回 429（请求过多）和友好提示
     */
    @PostConstruct
    public void initBlockHandlers() {
        BlockRequestHandler blockRequestHandler = (serverWebExchange, throwable) ->
                ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(fromValue("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}"));

        GatewayCallbackManager.setBlockHandler(blockRequestHandler);
    }
}
