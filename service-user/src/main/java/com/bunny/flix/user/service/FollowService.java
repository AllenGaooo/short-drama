package com.bunny.flix.user.service;

import com.bunny.flix.dto.PageResponse;
import com.bunny.flix.user.entity.Follow;

public interface FollowService {

    /**
     * 关注用户
     */
    void follow(Long userId, Long followUserId);

    /**
     * 取消关注
     */
    void unfollow(Long userId, Long followUserId);

    /**
     * 获取粉丝列表
     */
    PageResponse<Follow> getFollowers(Long userId, Integer page, Integer size);

    /**
     * 获取关注列表
     */
    PageResponse<Follow> getFollowing(Long userId, Integer page, Integer size);

    /**
     * 是否已关注
     */
    boolean isFollowing(Long userId, Long followUserId);
}
