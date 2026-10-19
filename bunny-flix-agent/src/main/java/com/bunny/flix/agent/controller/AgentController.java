package com.bunny.flix.agent.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.agent.service.AgentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 对话 Agent 对外接口
 * 前端通过 POST /api/agent/chat 发送用户消息，返回 agent 思考后的回复。
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    /** 发起一次对话 */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> payload) {
        String message = (String) payload.get("message");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> history =
                (List<Map<String, String>>) payload.getOrDefault("history", List.of());
        Map<String, Object> reply = agentService.chat(message, history);
        return Result.success(reply);
    }

    /** 获取 Agent 当前可用的工具列表（来自 MCP 服务） */
    @GetMapping("/tools")
    public Result<List<Map<String, Object>>> tools() {
        return Result.success(agentService.listTools());
    }

    /** 健康检查 / 欢迎语 */
    @GetMapping("/hello")
    public Result<Map<String, Object>> hello(@RequestParam(defaultValue = "管理员") String name) {
        return Result.success(Map.of("reply", "你好，" + name + "！我是兔兔智能助手，可以帮你查询短剧、用户，或回答平台相关问题。"));
    }
}
