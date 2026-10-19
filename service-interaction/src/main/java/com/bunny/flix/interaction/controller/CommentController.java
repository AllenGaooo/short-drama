package com.bunny.flix.interaction.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.interaction.entity.Comment;
import com.bunny.flix.interaction.mq.InteractionEventProducer;
import com.bunny.flix.interaction.service.CommentService;
import com.bunny.flix.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 评论控制器，提供评论的发布、删除、查询、置顶等接口
 */
@RestController
@RequestMapping("/api/interaction/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final InteractionEventProducer interactionEventProducer;

    @PostMapping
    public Result<Comment> addComment(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        Long dramaId = body.get("dramaId") != null ? Long.valueOf(body.get("dramaId").toString()) : null;
        Long episodeId = body.get("episodeId") != null ? Long.valueOf(body.get("episodeId").toString()) : null;
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
        Long replyToUserId = body.get("replyToUserId") != null ? Long.valueOf(body.get("replyToUserId").toString()) : null;
        String content = (String) body.get("content");

        Comment comment = commentService.addComment(userId, dramaId, episodeId, parentId, replyToUserId, content);

        if (parentId != null && parentId > 0 && replyToUserId != null) {
            interactionEventProducer.sendInteractionEvent("COMMENT_REPLY", userId, dramaId, replyToUserId);
        }

        return Result.success(comment);
    }

    @PutMapping("/{id}")
    public Result<Void> updateComment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        commentService.updateComment(userId, id, body.get("content"));
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        commentService.deleteComment(userId, id);
        return Result.success();
    }

    @PutMapping("/{id}/top")
    public Result<Void> topComment(@PathVariable Long id) {
        commentService.topComment(id);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<Comment>> getComments(
            @RequestParam(value = "dramaId", required = false) Long dramaId,
            @RequestParam(value = "episodeId", required = false) Long episodeId,
            @RequestParam(value = "parentId", required = false) Long parentId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sortBy", defaultValue = "default") String sortBy) {
        return Result.success(commentService.getComments(dramaId, episodeId, parentId, page, size, sortBy));
    }
}
