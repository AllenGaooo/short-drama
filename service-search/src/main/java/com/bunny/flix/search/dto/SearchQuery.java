package com.bunny.flix.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 多条件搜索入参
 *
 * 说明：
 * - keyword 走全文检索（参与相关性算分）
 * - category / tags / actors / minPlayCount 走 filter 精确筛选：
 *   不参与算分，且 ES 会对 filter 结果做缓存（query cache），性能更好
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchQuery implements Serializable {

    /** 搜索关键词（全文检索） */
    private String keyword;

    /** 题材/分类精确筛选，如 "都市"、"甜宠" */
    private String category;

    /** 标签筛选（任意命中即可） */
    private List<String> tags;

    /** 演员筛选 */
    private String actors;

    /** 最小播放量筛选 */
    private Long minPlayCount;

    /** 排序：default(相关度) / heat / newest / play */
    @Builder.Default
    private String sortBy = "default";

    /** 页码，从 1 开始 */
    @Builder.Default
    private Integer page = 1;

    /** 每页条数 */
    @Builder.Default
    private Integer size = 10;
}
