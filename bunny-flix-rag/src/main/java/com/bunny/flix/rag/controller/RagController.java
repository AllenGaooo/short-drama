package com.bunny.flix.rag.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.rag.service.RagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * RAG 知识检索服务
 * 支持：入库（ingest）、检索（retrieve）。内部用简单的词频向量 + 余弦相似度做语义召回。
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /** 检索最相关的若干条知识 */
    @GetMapping("/retrieve")
    public Result<List<Map<String, Object>>> retrieve(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int topK) {
        return Result.success(ragService.retrieve(query, topK));
    }

    /** 批量入库知识片段 */
    @PostMapping("/ingest")
    public Result<Map<String, Object>> ingest(@RequestBody List<Map<String, Object>> docs) {
        ragService.ingest(docs);
        return Result.success(Map.of("ingested", docs.size()));
    }
}
