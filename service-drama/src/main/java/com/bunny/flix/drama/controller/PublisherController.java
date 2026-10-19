package com.bunny.flix.drama.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.core.Result;
import com.bunny.flix.drama.entity.Publisher;
import com.bunny.flix.drama.service.PublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 发行方控制器，提供发行方的增删改查接口
 */
@RestController
@RequestMapping("/api/drama/publisher")
@RequiredArgsConstructor
public class PublisherController {

    private final PublisherService publisherService;

    @GetMapping("/list")
    public Result<Page<Publisher>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(publisherService.listPublishers(page, pageSize, keyword));
    }

    @GetMapping("/{id}")
    public Result<Publisher> getById(@PathVariable Long id) {
        Publisher publisher = publisherService.getPublisher(id);
        if (publisher == null) {
            return Result.notFound("发行方不存在");
        }
        return Result.success(publisher);
    }

    @PostMapping
    public Result<Publisher> create(@RequestBody Publisher publisher) {
        return Result.success(publisherService.createPublisher(publisher));
    }

    @PutMapping("/{id}")
    public Result<Publisher> update(@PathVariable Long id, @RequestBody Publisher publisher) {
        publisher.setId(id);
        return Result.success(publisherService.updatePublisher(publisher));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        publisherService.deletePublisher(id);
        return Result.success();
    }
}
