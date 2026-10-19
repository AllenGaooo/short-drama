package com.bunny.flix.search.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.search.entity.HotWord;
import com.bunny.flix.search.service.HotWordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 热搜词控制器，提供热搜词的增删改查接口
 */
@Tag(name = "热搜词管理", description = "热搜词的增删改查")
@RestController
@RequestMapping("/api/search/hot-word")
@RequiredArgsConstructor
public class HotWordController {

    private final HotWordService hotWordService;

    @Operation(summary = "热搜词列表")
    @GetMapping
    public Result<List<HotWord>> listHotWords() {
        return Result.success(hotWordService.listHotWords());
    }

    @Operation(summary = "新增热搜词")
    @PostMapping
    public Result<Void> addHotWord(@RequestBody HotWord hotWord) {
        hotWordService.addHotWord(hotWord);
        return Result.success();
    }

    @Operation(summary = "更新热搜词")
    @PutMapping("/{id}")
    public Result<Void> updateHotWord(
            @Parameter(description = "热搜词ID") @PathVariable Long id, @RequestBody HotWord hotWord) {
        hotWord.setId(id);
        hotWordService.updateHotWord(hotWord);
        return Result.success();
    }

    @Operation(summary = "删除热搜词")
    @DeleteMapping("/{id}")
    public Result<Void> deleteHotWord(@Parameter(description = "热搜词ID") @PathVariable Long id) {
        hotWordService.deleteHotWord(id);
        return Result.success();
    }
}
