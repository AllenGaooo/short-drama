package com.bunny.flix.graph.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.graph.service.GraphService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 知识图谱查询接口
 * 支持按实体/关系精确查询，也支持自然语言意图查询（简单关键词解析）
 */
@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    /** 按主体 + 关系查询邻接三元组 */
    @GetMapping("/query")
    public Result<Map<String, Object>> query(
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) String relation) {
        return Result.success(graphService.query(entity, relation));
    }

    /** 自然语言查询：从问句中抽取实体/关系后检索 */
    @PostMapping("/ask")
    public Result<Map<String, Object>> ask(@RequestBody Map<String, Object> payload) {
        String question = (String) payload.get("question");
        return Result.success(graphService.ask(question));
    }
}
