package com.bunny.flix.agent.config;

import com.bunny.flix.agent.function.DramaToolFunctions;
import com.bunny.flix.agent.service.McpToolService;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置：把 MCP 工具注册为大模型可调用的 Function
 *
 * 关键点（决定模型能否选对工具）：
 * 1. withName：工具名，模型据此调用
 * 2. withDescription：最重要！必须写清「什么时候用」「什么时候不要用」
 *    模型是靠描述来决策的，描述含糊会导致工具选错
 * 3. withInputType：入参类型，用于生成 JSON Schema
 *
 * 版本说明：Spring AI 0.8.1 使用 FunctionCallbackWrapper 注册方式。
 * 若升级到 Spring AI 1.0+，改为 @Bean + @Description 注解即可，业务代码不变。
 */
@Configuration
public class SpringAiConfig {

    @Bean
    public FunctionCallbackWrapper<?, ?> queryDramaFunction(McpToolService mcpToolService) {
        return FunctionCallbackWrapper
                .builder(new DramaToolFunctions.QueryDrama(mcpToolService))
                .withName("queryDrama")
                .withDescription(
                        "根据关键词查询短剧列表。"
                        + "当用户想找某部具体短剧、或按名称/题材搜索时使用，"
                        + "例如'有没有重生的剧'、'搜一下逆袭人生'、'都市题材的剧有哪些'。"
                        + "注意：如果用户是要'推荐''类似的'，不要用这个工具，改用 recommendDrama。")
                .withInputType(com.bunny.flix.agent.function.DramaFunctions.QueryDramaRequest.class)
                .build();
    }

    @Bean
    public FunctionCallbackWrapper<?, ?> recommendDramaFunction(McpToolService mcpToolService) {
        return FunctionCallbackWrapper
                .builder(new DramaToolFunctions.RecommendDrama(mcpToolService))
                .withName("recommendDrama")
                .withDescription(
                        "推荐短剧。用于两种场景："
                        + "1) 用户想找'类似的''差不多风格的'剧，需要传入参考剧名或题材；"
                        + "2) 用户没有明确目标，只是想看推荐，此时 keyword 传空字符串。"
                        + "例如'类似刚才那部'、'再推荐几部'、'有什么好看的'。")
                .withInputType(com.bunny.flix.agent.function.DramaFunctions.RecommendRequest.class)
                .build();
    }

    @Bean
    public FunctionCallbackWrapper<?, ?> hotRankingFunction(McpToolService mcpToolService) {
        return FunctionCallbackWrapper
                .builder(new DramaToolFunctions.HotRanking(mcpToolService))
                .withName("getHotRanking")
                .withDescription(
                        "获取热门短剧榜单/排行榜。"
                        + "当用户提到'榜单''排行''热门''最火的''Top10'时使用。"
                        + "size 表示返回条数，默认 10。")
                .withInputType(com.bunny.flix.agent.function.DramaFunctions.RankingRequest.class)
                .build();
    }

    @Bean
    public FunctionCallbackWrapper<?, ?> dramaDetailFunction(McpToolService mcpToolService) {
        return FunctionCallbackWrapper
                .builder(new DramaToolFunctions.DramaDetail(mcpToolService))
                .withName("getDramaDetail")
                .withDescription(
                        "查询某部短剧的详细信息（简介、集数、主演、题材）。"
                        + "当用户想了解某部剧的具体情况时使用，需要传入短剧ID。"
                        + "如果只知道剧名不知道ID，先用 queryDrama 查出ID。")
                .withInputType(com.bunny.flix.agent.function.DramaFunctions.DetailRequest.class)
                .build();
    }
}
