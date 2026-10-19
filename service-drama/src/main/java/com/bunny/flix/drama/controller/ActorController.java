package com.bunny.flix.drama.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.core.Result;
import com.bunny.flix.drama.entity.Actor;
import com.bunny.flix.drama.service.ActorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 演员控制器，提供演员的增删改查接口
 */
@RestController
@RequestMapping("/api/drama/actor")
@RequiredArgsConstructor
public class ActorController {

    private final ActorService actorService;

    @GetMapping("/list")
    public Result<Page<Actor>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(actorService.listActors(page, pageSize, keyword));
    }

    @GetMapping("/{id}")
    public Result<Actor> getById(@PathVariable Long id) {
        Actor actor = actorService.getActor(id);
        if (actor == null) {
            return Result.notFound("演员不存在");
        }
        return Result.success(actor);
    }

    @PostMapping
    public Result<Actor> create(@RequestBody Actor actor) {
        return Result.success(actorService.createActor(actor));
    }

    @PutMapping("/{id}")
    public Result<Actor> update(@PathVariable Long id, @RequestBody Actor actor) {
        actor.setId(id);
        return Result.success(actorService.updateActor(actor));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        actorService.deleteActor(id);
        return Result.success();
    }
}
