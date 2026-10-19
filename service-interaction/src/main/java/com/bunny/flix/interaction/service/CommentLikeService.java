package com.bunny.flix.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bunny.flix.interaction.entity.CommentLike;
import com.bunny.flix.interaction.mapper.CommentLikeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CommentLikeService {

    private final CommentLikeMapper commentLikeMapper;

    public void likeComment(Long userId, Long commentId) {
        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getUserId, userId).eq(CommentLike::getCommentId, commentId);
        CommentLike like = commentLikeMapper.selectOne(wrapper);
        if (like != null) {
            like.setStatus(1);
            like.setUpdateTime(LocalDateTime.now());
            commentLikeMapper.updateById(like);
        } else {
            like = new CommentLike();
            like.setUserId(userId);
            like.setCommentId(commentId);
            like.setStatus(1);
            commentLikeMapper.insert(like);
        }
    }

    public void unlikeComment(Long userId, Long commentId) {
        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getUserId, userId).eq(CommentLike::getCommentId, commentId);
        CommentLike like = commentLikeMapper.selectOne(wrapper);
        if (like != null) {
            like.setStatus(0);
            like.setUpdateTime(LocalDateTime.now());
            commentLikeMapper.updateById(like);
        }
    }

    public boolean isLiked(Long userId, Long commentId) {
        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getUserId, userId)
               .eq(CommentLike::getCommentId, commentId)
               .eq(CommentLike::getStatus, 1);
        return commentLikeMapper.selectCount(wrapper) > 0;
    }
}
