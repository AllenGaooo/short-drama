package com.bunny.flix.idgenerator.config;

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
 * Sentinel 限流规则
 * 为发号接口设置默认 QPS 阈值，防止突发流量把号段数据库打垮。
 * 规则也可通过 Sentinel Dashboard 动态调整，这里提供不依赖 Dashboard 的兜底默认值。
 */
@Slf4j
@Configuration
public class SentinelConfig {

    /** 单实例 /next 接口 QPS 上限 */
    @Value("${idgen.sentinel.next-qps:20000}")
    private int nextQps;

    /** 单实例 /batch 接口 QPS 上限 */
    @Value("${idgen.sentinel.batch-qps:2000}")
    private int batchQps;

    @PostConstruct
    public void initRules() {
        List<FlowRule> rules = new ArrayList<>();

        FlowRule nextRule = new FlowRule("idgen:next");
        nextRule.setCount(nextQps);
        nextRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        nextRule.setLimitApp(RuleConstant.LIMIT_APP_DEFAULT);
        rules.add(nextRule);

        FlowRule batchRule = new FlowRule("idgen:batch");
        batchRule.setCount(batchQps);
        batchRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        batchRule.setLimitApp(RuleConstant.LIMIT_APP_DEFAULT);
        rules.add(batchRule);

        FlowRuleManager.loadRules(rules);
        log.info("[idgen] Sentinel 限流规则已加载: idgen:next={} QPS, idgen:batch={} QPS", nextQps, batchQps);
    }
}
