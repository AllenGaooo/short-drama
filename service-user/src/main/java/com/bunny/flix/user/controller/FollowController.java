package com.bunny.flix.user.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.dto.PageResponse;
import com.bunny.flix.security.UserContext;
import com.bunny.flix.user.entity.Follow;
import com.bunny.flix.user.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 关注控制器
 * 提供关注/取消关注、查看粉丝列表、关注列表等接口
 */
@RestController
@RequestMapping("/api/user/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    /** 关注某个用户 */
    @PostMapping("/{followUserId}")
    public Result<Void> follow(@PathVariable Long followUserId) {
        Long currentUserId = UserContext.getUserId();
        followService.follow(currentUserId, followUserId);
        return Result.success("关注成功");
    }

    /** 取消关注 */
    @DeleteMapping("/{followUserId}")
    public Result<Void> unfollow(@PathVariable Long followUserId) {
        Long currentUserId = UserContext.getUserId();
        followService.unfollow(currentUserId, followUserId);
        return Result.success("取消关注成功");
    }

    /** 查看我的粉丝（关注我的人） */
    @GetMapping("/followers")
    public Result<PageResponse<Follow>> getFollowers(@RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "20") Integer size) {
        Long currentUserId = UserContext.getUserId();
        PageResponse<Follow> followers = followService.getFollowers(currentUserId, page, size);
        return Result.success(followers);
    }

    /** 查看我关注的人 */
    @GetMapping("/following")
    public Result<PageResponse<Follow>> getFollowing(@RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "20") Integer size) {
        Long currentUserId = UserContext.getUserId();
        PageResponse<Follow> following = followService.getFollowing(currentUserId, page, size);
        return Result.success(following);
    }

    /** 检查我是否关注了某个用户 */
    @GetMapping("/check/{followUserId}")
    public Result<Map<String, Object>> checkFollow(@PathVariable Long followUserId) {
        Long currentUserId = UserContext.getUserId();
        boolean following = followService.isFollowing(currentUserId, followUserId);

        Map<String, Object> data = new HashMap<>();
        data.put("following", following);
        return Result.success(data);
    }
}
