package com.bunny.flix.drama.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.drama.entity.Actor;

public interface ActorService {

    Page<Actor> listActors(Integer page, Integer pageSize, String keyword);

    Actor getActor(Long id);

    Actor createActor(Actor actor);

    Actor updateActor(Actor actor);

    void deleteActor(Long id);
}
