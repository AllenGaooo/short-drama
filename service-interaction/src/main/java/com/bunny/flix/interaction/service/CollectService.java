package com.bunny.flix.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.dto.PageResponse;
import com.bunny.flix.interaction.entity.Collect;
import com.bunny.flix.interaction.mapper.CollectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CollectService {

    private final CollectMapper collectMapper;

    public void collect(Long userId, Long dramaId) {
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, userId).eq(Collect::getDramaId, dramaId);
        Collect collect = collectMapper.selectOne(wrapper);
        if (collect != null) {
            collect.setStatus(1);
            collect.setUpdateTime(LocalDateTime.now());
            collectMapper.updateById(collect);
        } else {
            collect = new Collect();
            collect.setUserId(userId);
            collect.setDramaId(dramaId);
            collect.setStatus(1);
            collectMapper.insert(collect);
        }
    }

    public void uncollect(Long userId, Long dramaId) {
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, userId).eq(Collect::getDramaId, dramaId);
        Collect collect = collectMapper.selectOne(wrapper);
        if (collect != null) {
            collect.setStatus(0);
            collect.setUpdateTime(LocalDateTime.now());
            collectMapper.updateById(collect);
        }
    }

    public boolean isCollected(Long userId, Long dramaId) {
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, userId).eq(Collect::getDramaId, dramaId).eq(Collect::getStatus, 1);
        return collectMapper.selectCount(wrapper) > 0;
    }

    public long getCollectCount(Long dramaId) {
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getDramaId, dramaId).eq(Collect::getStatus, 1);
        return collectMapper.selectCount(wrapper);
    }

    public PageResponse<Collect> getUserCollects(Long userId, int page, int size) {
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, userId).eq(Collect::getStatus, 1).orderByDesc(Collect::getCreateTime);
        Page<Collect> result = collectMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResponse.of(result.getRecords(), result.getTotal(), page, size);
    }
}
