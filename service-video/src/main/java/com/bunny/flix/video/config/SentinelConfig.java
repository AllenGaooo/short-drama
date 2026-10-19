package com.bunny.flix.video.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel 限流规则（设计文档第 13 节 服务保护）
 *
 * 播放入口是流量最大的接口，配置 QPS 上限；
 * 超过阈值走 blockHandler 限流，异常走 fallback 降级，保护服务不被突发流量打垮。
 */
@Slf4j
@Configuration
public class SentinelConfig {

    @Value("${play.sentinel.authorize-qps:50000}")
    private int authorizeQps;

    @Value("${play.sentinel.progress-qps:100000}")
    private int progressQps;

    @Value("${play.sentinel.event-qps:50000}")
    private int eventQps;

    @PostConstruct
    public void initRules() {
        List<FlowRule> rules = new ArrayList<>();
        rules.add(rule("play:authorize", authorizeQps));
        rules.add(rule("play:progress", progressQps));
        rules.add(rule("play:event", eventQps));
        FlowRuleManager.loadRules(rules);
        log.info("[play] Sentinel 限流规则已加载: authorize={} progress={} event={}",
                authorizeQps, progressQps, eventQps);
    }

    private FlowRule rule(String resource, int qps) {
        FlowRule r = new FlowRule(resource);
        r.setCount(qps);
        r.setGrade(RuleConstant.FLOW_GRADE_QPS);
        r.setLimitApp(RuleConstant.LIMIT_APP_DEFAULT);
        return r;
    }
}
