package com.bunny.flix.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.interaction.entity.Like;
import com.bunny.flix.interaction.mapper.LikeMapper;
import com.bunny.flix.interaction.service.LikeService;
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
public class LikeServiceImpl implements LikeService {

    private final LikeMapper likeMapper;
    private final RedissonClient redissonClient;

    @Override
    @Transactional
    public void like(Long userId, Long dramaId) {
        String lockKey = "like_lock:" + userId + ":" + dramaId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Like::getUserId, userId)
                        .eq(Like::getDramaId, dramaId);
                Like existing = likeMapper.selectOne(wrapper);

                if (existing != null) {
                    existing.setStatus(1);
                    existing.setUpdateTime(LocalDateTime.now());
                    likeMapper.updateById(existing);
                } else {
                    Like like = new Like();
                    like.setUserId(userId);
                    like.setDramaId(dramaId);
                    like.setStatus(1);
                    like.setCreateTime(LocalDateTime.now());
                    like.setUpdateTime(LocalDateTime.now());
                    likeMapper.insert(like);
                }
                log.info("点赞成功: userId={}, dramaId={}", userId, dramaId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("点赞获取锁失败: userId={}, dramaId={}", userId, dramaId);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional
    public void unlike(Long userId, Long dramaId) {
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId)
                .eq(Like::getDramaId, dramaId);
        Like existing = likeMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setStatus(0);
            existing.setUpdateTime(LocalDateTime.now());
            likeMapper.updateById(existing);
            log.info("取消点赞成功: userId={}, dramaId={}", userId, dramaId);
        }
    }

    @Override
    public boolean isLiked(Long userId, Long dramaId) {
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId)
                .eq(Like::getDramaId, dramaId)
                .eq(Like::getStatus, 1);
        return likeMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Long getLikeCount(Long dramaId) {
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getDramaId, dramaId)
                .eq(Like::getStatus, 1);
        return likeMapper.selectCount(wrapper);
    }

    @Override
    public List<Like> getUserLikes(Long userId, Integer page, Integer size) {
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId)
                .eq(Like::getStatus, 1)
                .orderByDesc(Like::getCreateTime);

        Page<Like> pageParam = new Page<>(page, size);
        Page<Like> result = likeMapper.selectPage(pageParam, wrapper);
        return result.getRecords();
    }
}
