package com.bunny.flix.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.dto.PageResponse;
import com.bunny.flix.interaction.entity.Like;
import com.bunny.flix.interaction.mapper.LikeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeMapper likeMapper;

    public void like(Long userId, Long dramaId) {
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId).eq(Like::getDramaId, dramaId);
        Like like = likeMapper.selectOne(wrapper);
        if (like != null) {
            like.setStatus(1);
            like.setUpdateTime(LocalDateTime.now());
            likeMapper.updateById(like);
        } else {
            like = new Like();
            like.setUserId(userId);
            like.setDramaId(dramaId);
            like.setStatus(1);
            likeMapper.insert(like);
        }
    }

    public void unlike(Long userId, Long dramaId) {
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId).eq(Like::getDramaId, dramaId);
        Like like = likeMapper.selectOne(wrapper);
        if (like != null) {
            like.setStatus(0);
            like.setUpdateTime(LocalDateTime.now());
            likeMapper.updateById(like);
        }
    }

    public boolean isLiked(Long userId, Long dramaId) {
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId).eq(Like::getDramaId, dramaId).eq(Like::getStatus, 1);
        return likeMapper.selectCount(wrapper) > 0;
    }

    public long getLikeCount(Long dramaId) {
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getDramaId, dramaId).eq(Like::getStatus, 1);
        return likeMapper.selectCount(wrapper);
    }

    public PageResponse<Like> getUserLikes(Long userId, int page, int size) {
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId).eq(Like::getStatus, 1).orderByDesc(Like::getCreateTime);
        Page<Like> result = likeMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResponse.of(result.getRecords(), result.getTotal(), page, size);
    }
}
