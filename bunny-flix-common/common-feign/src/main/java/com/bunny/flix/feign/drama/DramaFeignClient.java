package com.bunny.flix.feign.drama;

import com.bunny.flix.core.Result;
import com.bunny.flix.feign.drama.dto.DramaDTO;
import com.bunny.flix.feign.drama.dto.EpisodeDTO;
import com.bunny.flix.feign.drama.dto.TagDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 短剧服务 Feign 客户端
 * 其他微服务通过这个接口调用 service-drama 短剧服务的 API
 * Feign 会自动把接口方法调用转成 HTTP 请求
 */
@FeignClient(name = "service-drama", path = "/api/drama")
public interface DramaFeignClient {

    /** 根据ID查询短剧详情 */
    @GetMapping("/{id}")
    Result<DramaDTO> getDramaById(@PathVariable("id") Long id);

    /** 判断短剧是否存在 */
    @GetMapping("/{id}/exists")
    Result<Boolean> dramaExists(@PathVariable("id") Long id);

    /** 根据剧集ID查询剧集详情 */
    @GetMapping("/episode/{episodeId}")
    Result<EpisodeDTO> getEpisodeById(@PathVariable("episodeId") Long episodeId);

    /** 查询某部短剧的所有剧集列表 */
    @GetMapping("/episode/list/{dramaId}")
    Result<List<EpisodeDTO>> listEpisodes(@PathVariable("dramaId") Long dramaId);

    /** 查询所有标签 */
    @GetMapping("/tag/all")
    Result<List<TagDTO>> listAllTags();

    /** 根据父标签ID查询子标签列表 */
    @GetMapping("/tag")
    Result<List<TagDTO>> listTagsByParentId(@RequestParam(value = "parentId", required = false) Long parentId);
}

