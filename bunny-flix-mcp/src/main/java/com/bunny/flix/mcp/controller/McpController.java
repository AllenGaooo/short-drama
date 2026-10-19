package com.bunny.flix.mcp.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.mcp.service.McpToolService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * MCP 工具服务对外接口
 * Agent 通过 HTTP 调用这里暴露的工具（tools），实现"模型调用工具"的能力。
 */
@RestController
@RequestMapping("/api/mcp")
public class McpController {

    private final McpToolService mcpToolService;

    public McpController(McpToolService mcpToolService) {
        this.mcpToolService = mcpToolService;
    }

    /** 列出当前 MCP 服务注册的所有工具（名称 + 描述 + 入参说明） */
    @GetMapping("/tools")
    public Result<List<Map<String, Object>>> listTools() {
        return Result.success(mcpToolService.listTools());
    }

    /** 调用指定工具，payload 形如 {"name":"...","args":{...}} */
    @PostMapping("/call")
    public Result<Map<String, Object>> callTool(@RequestBody Map<String, Object> payload) {
        String name = (String) payload.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> args = (Map<String, Object>) payload.getOrDefault("args", Map.of());
        Object result = mcpToolService.call(name, args);
        return Result.success(Map.of("tool", name, "result", result));
    }

    /** 便捷查询短剧 */
    @GetMapping("/drama")
    public Result<Object> queryDrama(@RequestParam(required = false) String keyword) {
        return Result.success(mcpToolService.call("query_drama", Map.of("keyword", keyword == null ? "" : keyword)));
    }
}
