package com.bunny.flix.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.interaction.entity.Collect;
import com.bunny.flix.interaction.mapper.CollectMapper;
import com.bunny.flix.interaction.service.CollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectServiceImpl implements CollectService {

    private final CollectMapper collectMapper;
    private final RedissonClient redissonClient;

    @Override
    @Transactional
    public void collect(Long userId, Long dramaId) {
        String lockKey = "collect_lock:" + userId + ":" + dramaId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Collect::getUserId, userId)
                        .eq(Collect::getDramaId, dramaId);
                Collect existing = collectMapper.selectOne(wrapper);

                if (existing != null) {
                    existing.setStatus(1);
                    existing.setUpdateTime(LocalDateTime.now());
                    collectMapper.updateById(existing);
                } else {
                    Collect collect = new Collect();
                    collect.setUserId(userId);
                    collect.setDramaId(dramaId);
                    collect.setStatus(1);
                    collect.setCreateTime(LocalDateTime.now());
                    collect.setUpdateTime(LocalDateTime.now());
                    collectMapper.insert(collect);
                }
                log.info("收藏成功: userId={}, dramaId={}", userId, dramaId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("收藏获取锁失败: userId={}, dramaId={}", userId, dramaId);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional
    public void uncollect(Long userId, Long dramaId) {
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, userId)
                .eq(Collect::getDramaId, dramaId);
        Collect existing = collectMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setStatus(0);
            existing.setUpdateTime(LocalDateTime.now());
            collectMapper.updateById(existing);
            log.info("取消收藏成功: userId={}, dramaId={}", userId, dramaId);
        }
    }

    @Override
    public boolean isCollected(Long userId, Long dramaId) {
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, userId)
                .eq(Collect::getDramaId, dramaId)
                .eq(Collect::getStatus, 1);
        return collectMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Long getCollectCount(Long dramaId) {
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getDramaId, dramaId)
                .eq(Collect::getStatus, 1);
        return collectMapper.selectCount(wrapper);
    }

    @Override
    public List<Collect> getUserCollects(Long userId, Integer page, Integer size) {
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, userId)
                .eq(Collect::getStatus, 1)
                .orderByDesc(Collect::getCreateTime);

        Page<Collect> pageParam = new Page<>(page, size);
        Page<Collect> result = collectMapper.selectPage(pageParam, wrapper);
        return result.getRecords();
    }
}
