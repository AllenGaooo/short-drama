package com.bunny.flix.aigateway.service;

import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM 服务（对应架构图中的「LLM」节点）
 * 若配置了 OpenAI 兼容接口则调用大模型汇总答案；否则用本地模板兜底，保证离线可跑。
 */
@Service
public class LlmService {

    private final RestTemplate restTemplate;

    @Value("${aigateway.llm.enabled:false}")
    private boolean enabled;

    @Value("${aigateway.llm.url:}")
    private String url;

    @Value("${aigateway.llm.api-key:}")
    private String apiKey;

    @Value("${aigateway.llm.model:gpt-3.5-turbo}")
    private String model;

    public LlmService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 汇总生成最终答案
     * @param question 用户问题
     * @param history 多轮对话历史
     * @param references RAG 检索片段
     * @param graph 图谱查询结果
     */
    public String answer(String question, List<Map<String, String>> history,
                         List<Map<String, Object>> references, Map<String, Object> graph) {
        if (enabled && url != null && !url.isBlank()) {
            try {
                return callModel(question, history, references, graph);
            } catch (Exception ignored) {
                // 失败回退本地
            }
        }
        return localAnswer(question, history, references, graph);
    }

    private String localAnswer(String question, List<Map<String, String>> history,
                               List<Map<String, Object>> references, Map<String, Object> graph) {
        StringBuilder sb = new StringBuilder();
        sb.append("针对「").append(question).append("」：\n\n");

        if (graph != null && !graph.isEmpty() && graph.get("triples") instanceof List) {
            List<?> triples = (List<?>) graph.get("triples");
            if (!triples.isEmpty()) {
                sb.append("🔗 图谱关系：\n");
                for (Object o : triples) {
                    if (o instanceof Map) {
                        Map<?, ?> t = (Map<?, ?>) o;
                        sb.append("- ").append(t.get("subject")).append(" ")
                          .append(t.get("relation")).append(" ")
                          .append(t.get("object")).append("\n");
                    }
                }
                sb.append("\n");
            }
        }

        if (references != null && !references.isEmpty()) {
            sb.append("📚 相关知识：\n");
            for (Map<String, Object> r : references) {
                sb.append("- ").append(r.get("content")).append("\n");
            }
            sb.append("\n");
        }

        if (sb.toString().trim().endsWith("：")) {
            sb.append("我暂时没有检索到相关信息，你可以换个问法，或描述更具体的业务对象（如某部短剧、会员体系等）。");
        } else {
            sb.append("（已综合图谱与知识库作答；如需更自然的表达可配置 aigateway.llm.* 接入大模型）");
        }
        return sb.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private String callModel(String question, List<Map<String, String>> history,
                             List<Map<String, Object>> references, Map<String, Object> graph) {
        StringBuilder sys = new StringBuilder("你是兔兔短剧平台的智能助手，请基于给定资料作答。\n");
        if (graph != null && graph.get("triples") instanceof List) {
            sys.append("图谱：").append(JSONUtil.toJsonStr(graph.get("triples"))).append("\n");
        }
        if (references != null && !references.isEmpty()) {
            sys.append("知识：").append(JSONUtil.toJsonStr(references)).append("\n");
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", sys.toString()));
        // 多轮历史（兼容 {role:文本} 与标准消息格式）
        if (history != null) {
            for (Map<String, String> h : history) {
                String content = h.get("content") != null ? h.get("content") : h.get("role");
                if (content == null || content.isBlank()
                        || "user".equals(content) || "assistant".equals(content)) {
                    continue;
                }
                messages.add(Map.of("role", "user", "content", content));
            }
        }
        messages.add(Map.of("role", "user", "content", question));

        Map<String, Object> body = Map.of("model", model, "messages", messages, "temperature", 0.7);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) headers.setBearerAuth(apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        Map<String, Object> resp = restTemplate.postForObject(url, entity, Map.class);
        if (resp != null && resp.get("choices") instanceof List) {
            List<?> choices = (List<?>) resp.get("choices");
            if (!choices.isEmpty() && choices.get(0) instanceof Map) {
                Object msg = ((Map<?, ?>) choices.get(0)).get("message");
                if (msg instanceof Map) return String.valueOf(((Map<?, ?>) msg).get("content"));
            }
        }
        return localAnswer(question, references, graph);
    }
}
