package com.bunny.flix.drama.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bunny.flix.drama.entity.Actor;
import com.bunny.flix.drama.mapper.ActorMapper;
import com.bunny.flix.drama.service.ActorService;
import com.bunny.flix.feign.idgenerator.BizTypeEnum;
import com.bunny.flix.feign.idgenerator.IdGeneratorFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ActorServiceImpl extends ServiceImpl<ActorMapper, Actor> implements ActorService {

    @Autowired
    private IdGeneratorFeignClient idGeneratorFeignClient;

    @Override
    public Page<Actor> listActors(Integer page, Integer pageSize, String keyword) {
        LambdaQueryWrapper<Actor> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(keyword), Actor::getName, keyword);
        wrapper.orderByDesc(Actor::getCreateTime);

        Page<Actor> pageParam = new Page<>(page != null ? page : 1, pageSize != null ? pageSize : 10);
        return this.page(pageParam, wrapper);
    }

    @Override
    public Actor getActor(Long id) {
        return this.getById(id);
    }

    @Override
    public Actor createActor(Actor actor) {
        if (actor.getId() == null) {
            actor.setId(idGeneratorFeignClient.nextId(BizTypeEnum.ACTOR).getData());
        }
        actor.setCreateTime(LocalDateTime.now());
        this.save(actor);
        return actor;
    }

    @Override
    public Actor updateActor(Actor actor) {
        this.updateById(actor);
        return this.getById(actor.getId());
    }

    @Override
    public void deleteActor(Long id) {
        this.removeById(id);
    }
}
