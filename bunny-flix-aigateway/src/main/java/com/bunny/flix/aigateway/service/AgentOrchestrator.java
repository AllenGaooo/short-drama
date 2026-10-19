package com.bunny.flix.aigateway.service;

import com.bunny.flix.security.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 编排器（AI Gateway 核心）
 *
 * 流程对应架构图：
 *   用户 -> API Gateway -> AI Gateway/Agent 服务
 *        -> 意图识别 -> Query Router -> { RAG检索 | 图谱查询 | LLM }
 *        -> 汇总生成最终回复
 */
@Service
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final IntentRecognizer intentRecognizer;
    private final QueryRouter queryRouter;
    private final LlmService llmService;

    public AgentOrchestrator(IntentRecognizer intentRecognizer, QueryRouter queryRouter, LlmService llmService) {
        this.intentRecognizer = intentRecognizer;
        this.queryRouter = queryRouter;
        this.llmService = llmService;
    }

    public Map<String, Object> chat(String message, List<Map<String, String>> history) {
        if (message == null || message.isBlank()) {
            return Map.of("reply", "请输入你的问题～", "intent", Map.of(), "route", List.of());
        }

        Long operator = UserContext.getUserId();
        log.info("[aigateway] operator={} message={}", operator, message);

        // 1) 意图识别
        Map<String, Object> intent = intentRecognizer.recognize(message);
        String intentType = String.valueOf(intent.get("intent"));

        // 2) Query Router 分发
        List<Map<String, Object>> references = List.of();
        Map<String, Object> graph = Map.of();
        List<String> routes = new java.util.ArrayList<>();

        switch (intentType) {
            case "RAG" -> {
                references = queryRouter.routeToRag(message);
                routes.add("rag");
            }
            case "GRAPH" -> {
                graph = queryRouter.routeToGraph(message);
                routes.add("graph");
            }
            case "HYBRID" -> {
                references = queryRouter.routeToRag(message);
                graph = queryRouter.routeToGraph(message);
                routes.add("rag");
                routes.add("graph");
            }
            default -> {
                // LLM：无检索，直接生成（本地兜底会给出引导语）
            }
        }

        // 3) LLM 汇总生成（传入多轮历史，保证上下文连贯）
        String reply = llmService.answer(message, history, references, graph);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("reply", reply);
        out.put("intent", intent);
        out.put("route", routes);
        out.put("references", references);
        out.put("graph", graph);
        return out;
    }
}
