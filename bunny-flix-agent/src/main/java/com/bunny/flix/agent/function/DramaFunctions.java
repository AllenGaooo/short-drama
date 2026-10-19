package com.bunny.flix.agent.function;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

import java.io.Serializable;

/**
 * Spring AI function calling 的入参/出参模型
 *
 * 说明：
 * - 这些类会被 Spring AI 自动转成 JSON Schema 发给大模型，
 *   因此字段上的注释（配合 @JsonPropertyDescription）决定了模型能否正确填参。
 * - 字段命名要语义清晰，避免模型误解。
 */
public class DramaFunctions {

    /** 查询短剧：入参 */
    @Data
    public static class QueryDramaRequest implements Serializable {
        @JsonPropertyDescription("短剧名称关键词或题材词，例如'重生'、'都市'。传空字符串则返回热门短剧")
        private String keyword;
    }

    /** 推荐短剧：入参 */
    @Data
    public static class RecommendRequest implements Serializable {
        @JsonPropertyDescription("参考短剧名称或题材；用户没有明确目标时传空字符串，返回热门推荐")
        private String keyword;
    }

    /** 热门榜单：入参 */
    @Data
    public static class RankingRequest implements Serializable {
        @JsonPropertyDescription("返回条数，默认10，最大100")
        private Integer size;
    }

    /** 短剧详情：入参 */
    @Data
    public static class DetailRequest implements Serializable {
        @JsonPropertyDescription("短剧ID，必须是纯数字；若只知道剧名请先调用 queryDrama 获取ID")
        private String dramaId;
    }

    /** 统一出参（工具执行结果，转成文本回给模型） */
    @Data
    public static class FunctionResponse implements Serializable {
        private String result;

        public FunctionResponse() {
        }

        public FunctionResponse(String result) {
            this.result = result;
        }
    }
}
