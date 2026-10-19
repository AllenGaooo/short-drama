package com.bunny.flix.feign.search;

import com.bunny.flix.core.Result;
import com.bunny.flix.feign.search.dto.SearchResultDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 搜索服务 Feign 客户端
 * 基于 Elasticsearch 的全文搜索服务
 */
@FeignClient(name = "service-search", path = "/api/search")
public interface SearchFeignClient {

    /** 搜索短剧，支持分页和排序 */
    @GetMapping
    Result<List<SearchResultDTO>> search(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sortBy", defaultValue = "default") String sortBy);

    /** 搜索建议（输入时实时联想） */
    @GetMapping("/suggest")
    Result<List<String>> suggest(@RequestParam("keyword") String keyword);

    /** 获取热门搜索词 */
    @GetMapping("/hot")
    Result<List<String>> getHotWords();
}
