package com.bunny.flix.interaction.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.dto.PageResponse;
import com.bunny.flix.interaction.entity.Comment;
import com.bunny.flix.interaction.entity.Danmaku;
import com.bunny.flix.interaction.service.*;
import com.bunny.flix.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interaction")
@RequiredArgsConstructor
public class InteractionController {

    private final LikeService likeService;
    private final CollectService collectService;
    private final CommentService commentService;
    private final CommentLikeService commentLikeService;
    private final DanmakuService danmakuService;
    private final ShareService shareService;

    // === 点赞 ===
    @PostMapping("/like")
    public Result<Void> like(@RequestBody Map<String, Long> body) {
        Long userId = UserContext.getUserId();
        likeService.like(userId, body.get("dramaId"));
        return Result.success();
    }

    @DeleteMapping("/like/{dramaId}")
    public Result<Void> unlike(@PathVariable Long dramaId) {
        likeService.unlike(UserContext.getUserId(), dramaId);
        return Result.success();
    }

    @GetMapping("/like/check/{dramaId}")
    public Result<Boolean> isLiked(@PathVariable Long dramaId) {
        return Result.success(likeService.isLiked(UserContext.getUserId(), dramaId));
    }

    @GetMapping("/like/count/{dramaId}")
    public Result<Long> getLikeCount(@PathVariable Long dramaId) {
        return Result.success(likeService.getLikeCount(dramaId));
    }

    // === 收藏 ===
    @PostMapping("/collect")
    public Result<Void> collect(@RequestBody Map<String, Long> body) {
        collectService.collect(UserContext.getUserId(), body.get("dramaId"));
        return Result.success();
    }

    @DeleteMapping("/collect/{dramaId}")
    public Result<Void> uncollect(@PathVariable Long dramaId) {
        collectService.uncollect(UserContext.getUserId(), dramaId);
        return Result.success();
    }

    @GetMapping("/collect/check/{dramaId}")
    public Result<Boolean> isCollected(@PathVariable Long dramaId) {
        return Result.success(collectService.isCollected(UserContext.getUserId(), dramaId));
    }

    @GetMapping("/collect/count/{dramaId}")
    public Result<Long> getCollectCount(@PathVariable Long dramaId) {
        return Result.success(collectService.getCollectCount(dramaId));
    }

    // === 评论 ===
    @PostMapping("/comment")
    public Result<Comment> addComment(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        Long dramaId = toLong(body.get("dramaId"));
        Long episodeId = toLong(body.get("episodeId"));
        Long parentId = toLong(body.get("parentId"));
        Long replyToUserId = toLong(body.get("replyToUserId"));
        String content = (String) body.get("content");
        Comment comment = commentService.addComment(userId, dramaId, episodeId, parentId, replyToUserId, content);
        return Result.success(comment);
    }

    @PutMapping("/comment/{id}")
    public Result<Void> updateComment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        commentService.updateComment(id, UserContext.getUserId(), body.get("content"));
        return Result.success();
    }

    @DeleteMapping("/comment/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id, UserContext.getUserId());
        return Result.success();
    }

    @PutMapping("/comment/{id}/top")
    public Result<Void> topComment(@PathVariable Long id, @RequestParam boolean top) {
        commentService.topComment(id, top);
        return Result.success();
    }

    @GetMapping("/comment/list")
    public Result<PageResponse<Comment>> getComments(
            @RequestParam Long dramaId,
            @RequestParam(required = false) Long episodeId,
            @RequestParam(required = false) Long parentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "hot") String sortBy) {
        return Result.success(commentService.getComments(dramaId, episodeId, parentId, page, size, sortBy));
    }

    @GetMapping("/comment/replies/{commentId}")
    public Result<PageResponse<Comment>> getReplies(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(commentService.getCommentReplies(commentId, page, size));
    }

    // === 评论点赞 ===
    @PostMapping("/comment/like")
    public Result<Void> likeComment(@RequestBody Map<String, Long> body) {
        commentLikeService.likeComment(UserContext.getUserId(), body.get("commentId"));
        return Result.success();
    }

    @DeleteMapping("/comment/like/{commentId}")
    public Result<Void> unlikeComment(@PathVariable Long commentId) {
        commentLikeService.unlikeComment(UserContext.getUserId(), commentId);
        return Result.success();
    }

    // === 弹幕 ===
    @PostMapping("/danmaku")
    public Result<Danmaku> sendDanmaku(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        Danmaku danmaku = danmakuService.sendDanmaku(
                userId,
                toLong(body.get("dramaId")),
                toLong(body.get("episodeId")),
                (String) body.get("content"),
                toDouble(body.get("playTime")),
                (String) body.getOrDefault("color", "#FFFFFF"),
                body.get("font") != null ? Integer.parseInt(body.get("font").toString()) : 1
        );
        return Result.success(danmaku);
    }

    @GetMapping("/danmaku/list")
    public Result<List<Danmaku>> getDanmakuList(
            @RequestParam Long episodeId,
            @RequestParam Double playTime) {
        return Result.success(danmakuService.getDanmakuList(episodeId, playTime));
    }

    // === 分享 ===
    @PostMapping("/share")
    public Result<Void> share(@RequestBody Map<String, String> body) {
        shareService.recordShare(UserContext.getUserId(),
                Long.valueOf(body.get("dramaId")),
                body.getOrDefault("platform", "wechat"));
        return Result.success();
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        return Long.valueOf(obj.toString());
    }

    private Double toDouble(Object obj) {
        if (obj == null) return null;
        return Double.valueOf(obj.toString());
    }
}
