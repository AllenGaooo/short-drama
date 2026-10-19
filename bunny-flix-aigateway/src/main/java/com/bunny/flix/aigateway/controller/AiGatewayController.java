package com.bunny.flix.aigateway.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.aigateway.service.AgentOrchestrator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * AI 网关对外接口
 * 前端通过 POST /api/aigateway/chat 发起对话，由 AI Gateway 完成意图识别->路由->检索->生成
 */
@RestController
@RequestMapping("/api/aigateway")
public class AiGatewayController {

    private final AgentOrchestrator orchestrator;

    public AiGatewayController(AgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> payload) {
        String message = (String) payload.get("message");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> history =
                (List<Map<String, String>>) payload.getOrDefault("history", List.of());
        return Result.success(orchestrator.chat(message, history));
    }
}
