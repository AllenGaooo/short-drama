package com.bunny.flix.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.core.exception.BusinessException;
import com.bunny.flix.dto.PageResponse;
import com.bunny.flix.user.entity.Follow;
import com.bunny.flix.user.mapper.FollowMapper;
import com.bunny.flix.user.service.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 关注服务实现
 * 实现关注/取消关注、查询粉丝/关注列表、判断是否关注等业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowMapper followMapper;

    @Override
    @Transactional
    public void follow(Long userId, Long followUserId) {
        // 不能关注自己
        if (userId.equals(followUserId)) {
            throw new BusinessException(400, "不能关注自己");
        }

        // 检查是否已经关注了
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, followUserId);
        if (followMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "已关注该用户");
        }

        Follow follow = new Follow();
        follow.setUserId(userId);
        follow.setFollowUserId(followUserId);
        follow.setCreateTime(LocalDateTime.now());

        followMapper.insert(follow);
        log.info("关注成功: userId={}, followUserId={}", userId, followUserId);
    }

    @Override
    @Transactional
    public void unfollow(Long userId, Long followUserId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, followUserId);
        int deleted = followMapper.delete(wrapper);
        if (deleted == 0) {
            throw new BusinessException(400, "未关注该用户");
        }
        log.info("取消关注成功: userId={}, followUserId={}", userId, followUserId);
    }

    /** 获取粉丝列表（关注我的人） */
    @Override
    public PageResponse<Follow> getFollowers(Long userId, Integer page, Integer size) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowUserId, userId)
                .orderByDesc(Follow::getCreateTime);

        Page<Follow> pageResult = followMapper.selectPage(
                new Page<>(page, size), wrapper);

        PageResponse<Follow> response = new PageResponse<>();
        response.setRecords(pageResult.getRecords());
        response.setTotal(pageResult.getTotal());
        response.setPage((int) pageResult.getCurrent());
        response.setSize((int) pageResult.getSize());
        return response;
    }

    /** 获取关注列表（我关注的人） */
    @Override
    public PageResponse<Follow> getFollowing(Long userId, Integer page, Integer size) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getUserId, userId)
                .orderByDesc(Follow::getCreateTime);

        Page<Follow> pageResult = followMapper.selectPage(
                new Page<>(page, size), wrapper);

        PageResponse<Follow> response = new PageResponse<>();
        response.setRecords(pageResult.getRecords());
        response.setTotal(pageResult.getTotal());
        response.setPage((int) pageResult.getCurrent());
        response.setSize((int) pageResult.getSize());
        return response;
    }

    @Override
    public boolean isFollowing(Long userId, Long followUserId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, followUserId);
        return followMapper.selectCount(wrapper) > 0;
    }
}
