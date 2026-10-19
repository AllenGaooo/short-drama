package com.bunny.flix.search.dto;

import com.bunny.flix.feign.search.dto.SearchResultDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 搜索结果封装（含总数与耗时，用于监控搜索性能是否达标）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchPageResult implements Serializable {

    /** 当前页数据 */
    private List<SearchResultDTO> list;

    /** 符合条件的总条数 */
    private Long total;

    /** 当前页 */
    private Integer page;

    /** 每页条数 */
    private Integer size;

    /** ES 查询耗时（毫秒） */
    private Long tookMs;

    /** 是否命中缓存 */
    private Boolean fromCache;

    /** 空结果 */
    public static SearchPageResult empty(Integer page, Integer size) {
        return SearchPageResult.builder()
                .list(Collections.emptyList())
                .total(0L)
                .page(page)
                .size(size)
                .tookMs(0L)
                .fromCache(false)
                .build();
    }
}
