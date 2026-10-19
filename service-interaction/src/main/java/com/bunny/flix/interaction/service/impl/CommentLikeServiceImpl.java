package com.bunny.flix.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bunny.flix.interaction.entity.CommentLike;
import com.bunny.flix.interaction.mapper.CommentLikeMapper;
import com.bunny.flix.interaction.service.CommentLikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentLikeServiceImpl implements CommentLikeService {

    private final CommentLikeMapper commentLikeMapper;

    @Override
    @Transactional
    public void likeComment(Long userId, Long commentId) {
        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getUserId, userId)
                .eq(CommentLike::getCommentId, commentId);
        CommentLike existing = commentLikeMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setStatus(1);
            existing.setUpdateTime(LocalDateTime.now());
            commentLikeMapper.updateById(existing);
        } else {
            CommentLike commentLike = new CommentLike();
            commentLike.setUserId(userId);
            commentLike.setCommentId(commentId);
            commentLike.setStatus(1);
            commentLike.setCreateTime(LocalDateTime.now());
            commentLike.setUpdateTime(LocalDateTime.now());
            commentLikeMapper.insert(commentLike);
        }
        log.info("评论点赞成功: userId={}, commentId={}", userId, commentId);
    }

    @Override
    @Transactional
    public void unlikeComment(Long userId, Long commentId) {
        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getUserId, userId)
                .eq(CommentLike::getCommentId, commentId);
        CommentLike existing = commentLikeMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setStatus(0);
            existing.setUpdateTime(LocalDateTime.now());
            commentLikeMapper.updateById(existing);
            log.info("取消评论点赞: userId={}, commentId={}", userId, commentId);
        }
    }

    @Override
    public boolean isLiked(Long userId, Long commentId) {
        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getUserId, userId)
                .eq(CommentLike::getCommentId, commentId)
                .eq(CommentLike::getStatus, 1);
        return commentLikeMapper.selectCount(wrapper) > 0;
    }
}
