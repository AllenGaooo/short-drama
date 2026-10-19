package com.bunny.flix.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.core.exception.BusinessException;
import com.bunny.flix.dto.PageResponse;
import com.bunny.flix.interaction.entity.Comment;
import com.bunny.flix.interaction.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;

    public Comment addComment(Long userId, Long dramaId, Long episodeId,
                               Long parentId, Long replyToUserId, String content) {
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setDramaId(dramaId);
        comment.setEpisodeId(episodeId);
        comment.setParentId(parentId != null ? parentId : 0L);
        comment.setReplyToUserId(replyToUserId);
        comment.setContent(content);
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setIsTop(0);
        comment.setIsDeleted(0);
        commentMapper.insert(comment);

        // 更新父评论回复数
        if (parentId != null && parentId > 0) {
            incrementReplyCount(parentId);
        }
        return comment;
    }

    public void updateComment(Long commentId, Long userId, String content) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getIsDeleted() == 1) {
            throw new BusinessException("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能编辑自己的评论");
        }
        comment.setContent(content);
        comment.setUpdateTime(LocalDateTime.now());
        commentMapper.updateById(comment);
    }

    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        comment.setIsDeleted(1);
        comment.setUpdateTime(LocalDateTime.now());
        commentMapper.updateById(comment);
    }

    public void topComment(Long commentId, boolean isTop) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        comment.setIsTop(isTop ? 1 : 0);
        commentMapper.updateById(comment);
    }

    public PageResponse<Comment> getComments(Long dramaId, Long episodeId, Long parentId,
                                              int page, int size, String sortBy) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getDramaId, dramaId)
               .eq(Comment::getParentId, parentId != null ? parentId : 0L)
               .eq(Comment::getIsDeleted, 0);
        if (episodeId != null) {
            wrapper.eq(Comment::getEpisodeId, episodeId);
        }
        if ("time".equals(sortBy)) {
            wrapper.orderByDesc(Comment::getCreateTime);
        } else {
            wrapper.orderByDesc(Comment::getLikeCount).orderByDesc(Comment::getCreateTime);
        }
        Page<Comment> result = commentMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResponse.of(result.getRecords(), result.getTotal(), page, size);
    }

    public PageResponse<Comment> getCommentReplies(Long commentId, int page, int size) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getParentId, commentId)
               .eq(Comment::getIsDeleted, 0)
               .orderByAsc(Comment::getCreateTime);
        Page<Comment> result = commentMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResponse.of(result.getRecords(), result.getTotal(), page, size);
    }

    public void incrementReplyCount(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment != null) {
            comment.setReplyCount(comment.getReplyCount() + 1);
            commentMapper.updateById(comment);
        }
    }
}
