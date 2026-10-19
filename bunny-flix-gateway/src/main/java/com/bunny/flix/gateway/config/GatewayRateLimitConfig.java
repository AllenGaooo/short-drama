package com.bunny.flix.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

/**
 * 网关限流规则配置
 * 针对领券等热点接口配置 Sentinel 限流，防止高并发抢券时压垮后端
 */
@Slf4j
@Configuration
public class GatewayRateLimitConfig {

    /**
     * 初始化网关 API 分组和限流规则
     */
    @PostConstruct
    public void init() {
        initCustomizedApis();
        initGatewayRules();
    }

    /**
     * 定义 API 分组：将"领券"相关路径归为一组，方便统一限流
     */
    private void initCustomizedApis() {
        Set<ApiDefinition> definitions = new HashSet<>();
        // 领券接口分组
        definitions.add(new ApiDefinition("coupon_receive_api")
                .setPredicateItems(Set.of(
                        new ApiPathPredicateItem().setPattern("/api/marketing/coupon/receive/**")
                                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX)
                )));
        GatewayApiDefinitionManager.loadApiDefinitions(definitions);
    }

    /**
     * 加载网关限流规则
     */
    private void initGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();
        // 领券接口限流：QPS 上限 20，突发缓冲 10
        rules.add(new GatewayFlowRule("coupon_receive_api")
                .setCount(20)
                .setIntervalSec(1)
                .setBurst(10)
                .setControlBehavior(com.alibaba.csp.sentinel.slots.block.RuleConstant.CONTROL_BEHAVIOR_DEFAULT));
        // 兜底：对 service-marketing 整体限流 QPS 100
        rules.add(new GatewayFlowRule("service-marketing")
                .setCount(100)
                .setIntervalSec(1));
        GatewayRuleManager.loadRules(rules);
        log.info("网关限流规则加载完成，领券接口 QPS=20");
    }
}
