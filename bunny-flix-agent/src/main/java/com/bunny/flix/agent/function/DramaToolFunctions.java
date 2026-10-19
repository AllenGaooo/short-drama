package com.bunny.flix.agent.function;

import cn.hutool.json.JSONUtil;
import com.bunny.flix.agent.service.McpToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;

/**
 * 把 MCP 工具封装成 Spring AI 的 Function
 *
 * 作用：大模型通过 function calling 决定"调哪个工具、传什么参数"，
 *      这些 Function 负责真正执行（底层调用 bunny-flix-mcp 服务）。
 *
 * 设计要点：
 * 1. 每个 Function 实现 java.util.function.Function<Request, Response>
 * 2. 执行异常要捕获并返回"错误描述"给模型，而不是抛出——
 *    让模型知道工具失败了，它可以换个方式回答或重试
 * 3. 返回结果转成简洁文本，避免把大量原始数据塞回模型上下文（省 token）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DramaToolFunctions {

    private final McpToolService mcpToolService;

    /** 查询短剧 */
    public static class QueryDrama implements Function<DramaFunctions.QueryDramaRequest, DramaFunctions.FunctionResponse> {

        private final McpToolService mcpToolService;

        public QueryDrama(McpToolService mcpToolService) {
            this.mcpToolService = mcpToolService;
        }

        @Override
        public DramaFunctions.FunctionResponse apply(DramaFunctions.QueryDramaRequest request) {
            try {
                String keyword = request == null ? "" : request.getKeyword();
                Object result = mcpToolService.callTool("query_drama", Map.of("keyword", keyword));
                return new DramaFunctions.FunctionResponse(JSONUtil.toJsonStr(result));
            } catch (Exception e) {
                log.warn("[ai-function] 查询短剧失败: {}", e.getMessage());
                return new DramaFunctions.FunctionResponse("查询失败：" + e.getMessage());
            }
        }
    }

    /** 推荐短剧（相似推荐 / 热门推荐） */
    public static class RecommendDrama implements Function<DramaFunctions.RecommendRequest, DramaFunctions.FunctionResponse> {

        private final McpToolService mcpToolService;

        public RecommendDrama(McpToolService mcpToolService) {
            this.mcpToolService = mcpToolService;
        }

        @Override
        public DramaFunctions.FunctionResponse apply(DramaFunctions.RecommendRequest request) {
            try {
                String keyword = request == null ? "" : request.getKeyword();
                Object result = mcpToolService.callTool("recommend_drama", Map.of("keyword", keyword));
                return new DramaFunctions.FunctionResponse(JSONUtil.toJsonStr(result));
            } catch (Exception e) {
                log.warn("[ai-function] 推荐短剧失败: {}", e.getMessage());
                return new DramaFunctions.FunctionResponse("推荐失败：" + e.getMessage());
            }
        }
    }

    /** 热门榜单 */
    public static class HotRanking implements Function<DramaFunctions.RankingRequest, DramaFunctions.FunctionResponse> {

        private final McpToolService mcpToolService;

        public HotRanking(McpToolService mcpToolService) {
            this.mcpToolService = mcpToolService;
        }

        @Override
        public DramaFunctions.FunctionResponse apply(DramaFunctions.RankingRequest request) {
            try {
                int size = (request == null || request.getSize() == null) ? 10 : request.getSize();
                Object result = mcpToolService.callTool("get_hot_ranking", Map.of("size", String.valueOf(size)));
                return new DramaFunctions.FunctionResponse(JSONUtil.toJsonStr(result));
            } catch (Exception e) {
                log.warn("[ai-function] 获取榜单失败: {}", e.getMessage());
                return new DramaFunctions.FunctionResponse("获取榜单失败：" + e.getMessage());
            }
        }
    }

    /** 短剧详情 */
    public static class DramaDetail implements Function<DramaFunctions.DetailRequest, DramaFunctions.FunctionResponse> {

        private final McpToolService mcpToolService;

        public DramaDetail(McpToolService mcpToolService) {
            this.mcpToolService = mcpToolService;
        }

        @Override
        public DramaFunctions.FunctionResponse apply(DramaFunctions.DetailRequest request) {
            try {
                String dramaId = request == null ? "" : request.getDramaId();
                Object result = mcpToolService.callTool("get_drama_detail", Map.of("dramaId", dramaId));
                return new DramaFunctions.FunctionResponse(JSONUtil.toJsonStr(result));
            } catch (Exception e) {
                log.warn("[ai-function] 查询详情失败: {}", e.getMessage());
                return new DramaFunctions.FunctionResponse("查询详情失败：" + e.getMessage());
            }
        }
    }
}
