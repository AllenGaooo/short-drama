package com.bunny.flix.drama.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.drama.entity.Tag;
import com.bunny.flix.drama.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签控制器，提供标签的增删改查接口
 */
@RestController
@RequestMapping("/api/drama/tag")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public Result<List<Tag>> list(@RequestParam(required = false) Long parentId) {
        if (parentId != null) {
            return Result.success(tagService.listTags(parentId));
        }
        return Result.success(tagService.listAllTags());
    }

    @GetMapping("/all")
    public Result<List<Tag>> listAll() {
        return Result.success(tagService.listAllTags());
    }

    @GetMapping("/{id}")
    public Result<Tag> getById(@PathVariable Long id) {
        Tag tag = tagService.getTag(id);
        if (tag == null) {
            return Result.notFound("标签不存在");
        }
        return Result.success(tag);
    }

    @PostMapping
    public Result<Tag> create(@RequestBody Tag tag) {
        return Result.success(tagService.createTag(tag));
    }

    @PutMapping("/{id}")
    public Result<Tag> update(@PathVariable Long id, @RequestBody Tag tag) {
        tag.setId(id);
        return Result.success(tagService.updateTag(tag));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.deleteTag(id);
        return Result.success();
    }
}
