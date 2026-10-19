package com.bunny.flix.agent.service;

import cn.hutool.json.JSONUtil;
import com.bunny.flix.security.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 对话 Agent 编排服务
 *
 * 工作流（ReAct 风格）：
 *   1. 把用户问题送 RAG 检索相关平台知识；
 *   2. 判断是否需要调用业务工具（MCP）：命中关键词则调用对应工具；
 *   3. 汇总「系统提示 + 检索知识 + 工具结果 + 对话历史 + 用户问题」生成回复；
 *   4. 若配置了 LLM 地址则走大模型，否则用本地规则型回复兜底，保证离线可用。
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ConversationContextService conversationContextService;

    /** Spring AI 版对话服务（未配置 key 时其内部不可用，会自动降级） */
    @Autowired(required = false)
    private SpringAiAgentService springAiAgentService;

    /** Spring AI 开关：配了 spring.ai.openai.api-key 才启用 */
    @Value("${agent.spring-ai.enabled:true}")
    private boolean springAiEnabled;

    @Value("${agent.mcp-service:lb://bunny-flix-mcp}")
    private String mcpService;

    @Value("${agent.rag-service:lb://bunny-flix-rag}")
    private String ragService;

    /** 可选：OpenAI 兼容大模型地址与密钥，留空则使用本地兜底回复 */
    @Value("${agent.llm.enabled:false}")
    private boolean llmEnabled;

    @Value("${agent.llm.url:}")
    private String llmUrl;

    @Value("${agent.llm.api-key:}")
    private String llmApiKey;

    @Value("${agent.llm.model:gpt-3.5-turbo}")
    private String llmModel;

    /**
     * 工具调用决策关键词（按优先级排列，Map 用 LinkedHashMap 保证顺序）
     * 说明：这是"规则型"意图识别，零成本、可解释；
     *      若要更智能的意图理解，可开启 agent.llm.* 交由大模型做 function calling。
     */
    private static final Map<String, String> TOOL_KEYWORDS = new LinkedHashMap<>();

    static {
        TOOL_KEYWORDS.put("推荐", "recommend_drama");
        TOOL_KEYWORDS.put("类似", "recommend_drama");
        TOOL_KEYWORDS.put("榜单", "get_hot_ranking");
        TOOL_KEYWORDS.put("排行", "get_hot_ranking");
        TOOL_KEYWORDS.put("热门", "get_hot_ranking");
        TOOL_KEYWORDS.put("详情", "get_drama_detail");
        TOOL_KEYWORDS.put("短剧", "query_drama");
        TOOL_KEYWORDS.put("剧", "query_drama");
        TOOL_KEYWORDS.put("用户", "query_user");
        TOOL_KEYWORDS.put("通知", "send_notice");
    }

    public Map<String, Object> chat(String message, List<Map<String, String>> history) {
        if (message == null || message.isBlank()) {
            return Map.of("reply", "请输入你想咨询的内容～", "usedTools", List.of(), "references", List.of());
        }

        Long operatorId = UserContext.getUserId();
        log.info("[agent] operator={} message={}", operatorId, message);

        // 0) 多轮上下文：指代消解，把「上一部」「类似刚才那部」还原成具体剧名
        //    未登录时用匿名用户维度，保证功能仍可用
        Long contextKey = operatorId != null ? operatorId : ANONYMOUS_USER_ID;
        String resolvedMessage = conversationContextService.resolveReference(contextKey, message);

        // 1) RAG 检索知识（用消解后的语句，否则「上一部」无法命中）
        List<Map<String, Object>> references = retrieve(resolvedMessage);
        // 2) 决定是否调用 MCP 工具
        List<Map<String, Object>> toolCalls = decideAndCallTools(resolvedMessage);
        // 3) 记录本轮提到的短剧，供下一轮指代使用
        rememberDrama(contextKey, toolCalls);
        // 4) 生成回复（传入历史，支持多轮上下文）
        String reply = generate(message, history, references, toolCalls);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("reply", reply);
        out.put("usedTools", toolCalls);
        out.put("references", references);
        // 回传解析结果，便于前端提示"正在围绕《XXX》对话"
        out.put("resolvedMessage", resolvedMessage);
        out.put("contextDrama", conversationContextService.getLastDrama(contextKey));
        return out;
    }

    /** 未登录用户的上下文维度（同一进程内共享，仅用于功能可用） */
    private static final Long ANONYMOUS_USER_ID = 0L;

    /**
     * 从工具调用结果中提取短剧名并写入上下文，供下一轮「上一部」这类指代使用
     */
    private void rememberDrama(Long userId, List<Map<String, Object>> toolCalls) {
        for (Map<String, Object> call : toolCalls) {
            if (!"query_drama".equals(call.get("tool"))) {
                continue;
            }
            Object result = call.get("result");
            if (!(result instanceof Map<?, ?> resultMap)) {
                continue;
            }
            // 工具返回结构：{ tool: "query_drama", result: [ {title: ...}, ... ] }
            Object data = resultMap.get("result");
            if (data instanceof java.util.List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof Map<?, ?> drama && drama.get("title") != null) {
                    conversationContextService.recordDrama(userId, drama.get("title").toString());
                }
            }
        }
    }

    public List<Map<String, Object>> listTools() {
        try {
            String url = mcpService + "/api/mcp/tools";
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null && resp.get("data") instanceof List) {
                return (List<Map<String, Object>>) resp.get("data");
            }
        } catch (Exception e) {
            log.warn("[agent] 获取工具列表失败: {}", e.getMessage());
        }
        return List.of();
    }

    // ---------- 内部步骤 ----------

    private List<Map<String, Object>> retrieve(String query) {
        try {
            String url = ragService + "/api/rag/retrieve?query=" + java.net.URLEncoder.encode(query, "UTF-8") + "&topK=3";
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null && resp.get("data") instanceof List) {
                return (List<Map<String, Object>>) resp.get("data");
            }
        } catch (Exception e) {
            log.warn("[agent] RAG 检索失败: {}", e.getMessage());
        }
        return List.of();
    }

    private List<Map<String, Object>> decideAndCallTools(String message) {
        List<Map<String, Object>> calls = new ArrayList<>();
        for (Map.Entry<String, String> e : TOOL_KEYWORDS.entrySet()) {
            if (message.contains(e.getKey())) {
                calls.add(callTool(e.getValue(), message));
                if (calls.size() >= 2) break; // 单次最多调用 2 个工具
            }
        }
        return calls;
    }

    private Map<String, Object> callTool(String tool, String message) {
        Map<String, Object> args = buildToolArgs(tool, message);
        Map<String, Object> payload = Map.of("name", tool, "args", args);
        try {
            String url = mcpService + "/api/mcp/call";
            Map<?, ?> resp = restTemplate.postForObject(url, payload, Map.class);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tool", tool);
            item.put("args", args);
            item.put("result", resp == null ? null : resp.get("data"));
            return item;
        } catch (Exception ex) {
            log.warn("[agent] 调用工具 {} 失败: {}", tool, ex.getMessage());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tool", tool);
            item.put("args", args);
            item.put("error", ex.getMessage());
            return item;
        }
    }

    private Map<String, Object> buildToolArgs(String tool, String message) {
        Map<String, Object> args = new LinkedHashMap<>();
        if ("query_drama".equals(tool)) {
            args.put("keyword", extractKeyword(message, "短剧", "剧"));
        } else if ("recommend_drama".equals(tool)) {
            // 推荐：从"类似XXX""推荐XXX"中提取参考剧名
            args.put("keyword", extractKeyword(message, "类似", "推荐"));
        } else if ("get_hot_ranking".equals(tool)) {
            args.put("size", "10");
        } else if ("get_drama_detail".equals(tool)) {
            args.put("dramaId", extractDigits(message));
        } else if ("query_user".equals(tool)) {
            args.put("keyword", message);
        } else if ("send_notice".equals(tool)) {
            Long uid = UserContext.getUserId();
            args.put("userId", uid == null ? "anonymous" : String.valueOf(uid));
            args.put("content", message);
        }
        return args;
    }

    /** 从语句中提取数字（用于从"1001的详情"中提取短剧ID） */
    private String extractDigits(String message) {
        if (message == null) {
            return "";
        }
        StringBuilder digits = new StringBuilder();
        for (char c : message.toCharArray()) {
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (digits.length() > 0) {
                break;
            }
        }
        return digits.toString();
    }

    private String extractKeyword(String message, String... marks) {
        for (String m : marks) {
            int i = message.indexOf(m);
            if (i >= 0) {
                // 取标记之后 6 个字作为关键词
                int end = Math.min(i + m.length() + 6, message.length());
                return message.substring(i + m.length(), end).replaceAll("[?？。，,！!]", "").trim();
            }
        }
        return "";
    }

    /**
     * 生成回复（三级降级，保证任何情况下都有回复）
     *
     * 优先级：
     *   ① Spring AI（function calling）：能理解复杂/长尾表达，工具选择由模型决定
     *   ② 原生 LLM 接口（agent.llm.*）：Spring AI 不可用时的备选
     *   ③ 本地模板：都没有时兜底，保证离线可用
     */
    private String generate(String message, List<Map<String, String>> history,
                            List<Map<String, Object>> references, List<Map<String, Object>> toolCalls) {

        // ① 优先走 Spring AI（模型自主选工具）
        if (springAiEnabled) {
            try {
                String reply = springAiAgentService.chat(message, history);
                if (reply != null && !reply.isBlank()) {
                    return reply;
                }
            } catch (Exception e) {
                log.warn("[agent] Spring AI 调用失败，降级: {}", e.getMessage());
            }
        }

        // ② 备选：原生大模型接口
        if (llmEnabled && llmUrl != null && !llmUrl.isBlank()) {
            try {
                return callLlm(message, history, references, toolCalls);
            } catch (Exception e) {
                log.warn("[agent] LLM 调用失败，回退本地回复: {}", e.getMessage());
            }
        }

        // ③ 本地兜底
        return localReply(message, history, references, toolCalls);
    }

    /** 本地规则型回复（无需外部模型，保证离线可跑） */
    private String localReply(String message, List<Map<String, String>> history,
                              List<Map<String, Object>> references, List<Map<String, Object>> toolCalls) {
        StringBuilder sb = new StringBuilder();

        // 多轮上下文：若本轮是承接上文的追问，先点明"正在围绕哪部剧"，让回复有上下文感
        if (history != null && !history.isEmpty()) {
            sb.append("（承接上文，共 ").append(history.size()).append(" 轮对话）\n");
        }

        // 工具结果优先展示
        if (!toolCalls.isEmpty()) {
            for (Map<String, Object> call : toolCalls) {
                Object res = call.get("result");
                if (res != null) {
                    sb.append("🛠 已为你调用工具「").append(call.get("tool")).append("」：\n")
                      .append(JSONUtil.toJsonPrettyStr(res)).append("\n");
                } else if (call.get("error") != null) {
                    sb.append("⚠️ 工具「").append(call.get("tool")).append("」调用失败：")
                      .append(call.get("error")).append("\n");
                }
            }
        }
        // 知识补充
        if (!references.isEmpty()) {
            sb.append("\n📚 相关平台知识：\n");
            for (Map<String, Object> r : references) {
                sb.append("- ").append(r.get("content")).append("\n");
            }
        }
        // 兜底话术
        if (sb.length() == 0) {
            sb.append("收到你的消息：「").append(message).append("」。")
              .append("我是兔兔智能助手，可以帮你查询短剧/用户或回答平台运营问题，试试问我“有哪些热门短剧？”或“怎么查询用户？”");
        } else {
            sb.append("\n（如需更智能的回答，可在 agent 服务配置 agent.llm.* 接入大模型）");
        }
        return sb.toString().trim();
    }

    /** 调用 OpenAI 兼容接口（启用时） */
    private String callLlm(String message, List<Map<String, String>> history,
                           List<Map<String, Object>> references, List<Map<String, Object>> toolCalls) {
        StringBuilder sys = new StringBuilder("你是兔兔短剧平台的智能运营助手。");
        if (!references.isEmpty()) {
            sys.append("已知知识：\n");
            references.forEach(r -> sys.append("- ").append(r.get("content")).append("\n"));
        }
        if (!toolCalls.isEmpty()) {
            sys.append("已获取工具数据：\n");
            toolCalls.forEach(c -> sys.append("- ").append(c.get("tool")).append(": ")
                    .append(JSONUtil.toJsonStr(c.get("result"))).append("\n"));
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", sys.toString()));
        // 多轮历史：兼容两种入参格式
        //  1) {role:"用户说的话"}  —— 现有前端的传法
        //  2) {role:"user"|"assistant", content:"..."} —— 标准大模型消息格式
        if (history != null) {
            for (Map<String, String> h : history) {
                String content = h.get("content") != null ? h.get("content") : h.get("role");
                if (content == null || content.isBlank()) {
                    continue;
                }
                String role = "user".equals(h.get("role")) || "assistant".equals(h.get("role"))
                        ? h.get("role") : "user";
                // 注意：采用格式2时不能把 "user" 这种角色标识当正文
                if (!"user".equals(content) && !"assistant".equals(content)) {
                    messages.add(Map.of("role", role, "content", content));
                }
            }
        }
        messages.add(Map.of("role", "user", "content", message));

        Map<String, Object> body = Map.of(
                "model", llmModel,
                "messages", messages,
                "temperature", 0.7
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (llmApiKey != null && !llmApiKey.isBlank()) {
            headers.setBearerAuth(llmApiKey);
        }
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = restTemplate.postForObject(llmUrl, entity, Map.class);
        if (resp != null && resp.get("choices") instanceof List) {
            List<?> choices = (List<?>) resp.get("choices");
            if (!choices.isEmpty() && choices.get(0) instanceof Map) {
                Object content = ((Map<?, ?>) choices.get(0)).get("message");
                if (content instanceof Map) {
                    return String.valueOf(((Map<?, ?>) content).get("content"));
                }
            }
        }
        return localReply(message, references, toolCalls);
    }
}
