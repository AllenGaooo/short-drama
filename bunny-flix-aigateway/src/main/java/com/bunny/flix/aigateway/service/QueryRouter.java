package com.bunny.flix.aigateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Query Router（查询路由器）
 * 根据意图识别结果，将查询分发到对应的下游能力：
 *   RAG    -> bunny-flix-rag 检索
 *   GRAPH  -> bunny-flix-graph 图谱查询
 *   HYBRID -> 两者都调用
 *   LLM    -> 不路由（由大模型直接生成）
 * 对应架构图中的「Query Router」节点。
 */
@Component
public class QueryRouter {

    private final RestTemplate restTemplate;

    @Value("${aigateway.rag-service:lb://bunny-flix-rag}")
    private String ragService;

    @Value("${aigateway.graph-service:lb://bunny-flix-graph}")
    private String graphService;

    public QueryRouter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** 调用 RAG 检索 */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> routeToRag(String query) {
        try {
            String url = ragService + "/api/rag/retrieve?query="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&topK=3";
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null && resp.get("data") instanceof List) {
                return (List<Map<String, Object>>) resp.get("data");
            }
        } catch (Exception e) {
            // 调用失败返回空，由上层兜底
        }
        return List.of();
    }

    /** 调用图谱查询 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> routeToGraph(String query) {
        try {
            String url = graphService + "/api/graph/ask";
            Map<String, Object> resp = restTemplate.postForObject(url, Map.of("question", query), Map.class);
            if (resp != null && resp.get("data") instanceof Map) {
                return (Map<String, Object>) resp.get("data");
            }
        } catch (Exception e) {
            // 调用失败返回空
        }
        return Map.of();
    }
}
