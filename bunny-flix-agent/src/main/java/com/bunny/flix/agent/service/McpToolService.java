package com.bunny.flix.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * MCP 工具客户端（Agent 调用 bunny-flix-mcp 服务）
 *
 * 职责：
 * 1. 拉取工具清单（供大模型做 function calling）
 * 2. 执行指定工具并拿到结果
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolService {

    private final RestTemplate restTemplate;

    @Value("${agent.mcp-service:lb://bunny-flix-mcp}")
    private String mcpService;

    /**
     * 获取工具清单（MCP 返回 name/description/parameters）
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listTools() {
        try {
            Map<String, Object> resp = restTemplate.getForObject(mcpService + "/api/mcp/tools", Map.class);
            if (resp != null && resp.get("data") instanceof List) {
                return (List<Map<String, Object>>) resp.get("data");
            }
        } catch (Exception e) {
            log.warn("[agent] 获取MCP工具列表失败: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * 调用指定工具
     *
     * @param toolName 工具名，如 query_drama
     * @param args     参数
     * @return 工具执行结果（MCP 返回的 data 部分）
     */
    @SuppressWarnings("unchecked")
    public Object callTool(String toolName, Map<String, Object> args) {
        Map<String, Object> payload = Map.of("name", toolName, "args", args == null ? Map.of() : args);
        Map<String, Object> resp = restTemplate.postForObject(mcpService + "/api/mcp/call", payload, Map.class);
        if (resp != null) {
            return resp.get("data");
        }
        return null;
    }
}
