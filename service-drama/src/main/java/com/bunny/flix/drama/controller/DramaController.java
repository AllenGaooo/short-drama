package com.bunny.flix.drama.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.core.Result;
import com.bunny.flix.drama.entity.Drama;
import com.bunny.flix.drama.service.DramaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 短剧控制器
 * 提供短剧的增删改查、搜索、分页列表等接口
 */
@RestController
@RequestMapping("/api/drama")
@RequiredArgsConstructor
public class DramaController {

    private final DramaService dramaService;

    @GetMapping("/list")
    public Result<Page<Drama>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer payType,
            @RequestParam(required = false) Long tagId) {
        return Result.success(dramaService.listDramas(page, pageSize, keyword, status, payType, tagId));
    }

    /**
     * 热门榜单
     * @param size   返回条数，默认 10，最大 100
     * @param sortBy hot=按播放量（默认），newest=按最新
     */
    @GetMapping("/ranking")
    public Result<List<Drama>> ranking(
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy) {
        return Result.success(dramaService.listRanking(size, sortBy));
    }

    @GetMapping("/{id}")
    public Result<Drama> getById(@PathVariable Long id) {
        Drama drama = dramaService.getDramaById(id);
        if (drama == null) {
            return Result.notFound("短剧不存在");
        }
        return Result.success(drama);
    }

    @GetMapping("/{id}/exists")
    public Result<Boolean> exists(@PathVariable Long id) {
        Drama drama = dramaService.getDramaById(id);
        return Result.success(drama != null);
    }

    @PostMapping
    public Result<Drama> create(@RequestBody Drama drama) {
        return Result.success(dramaService.createDrama(drama));
    }

    @PutMapping("/{id}")
    public Result<Drama> update(@PathVariable Long id, @RequestBody Drama drama) {
        drama.setId(id);
        return Result.success(dramaService.updateDrama(drama));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dramaService.deleteDrama(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        dramaService.updateStatus(id, status);
        return Result.success();
    }

    @PutMapping("/{id}/completed")
    public Result<Void> updateCompleted(@PathVariable Long id, @RequestParam Integer completed) {
        dramaService.updateCompleted(id, completed);
        return Result.success();
    }
}
