package com.bunny.flix.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.core.exception.BusinessException;
import com.bunny.flix.feign.idgenerator.BizTypeEnum;
import com.bunny.flix.feign.idgenerator.IdGeneratorFeignClient;
import com.bunny.flix.interaction.entity.Comment;
import com.bunny.flix.interaction.mapper.CommentMapper;
import com.bunny.flix.interaction.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final IdGeneratorFeignClient idGeneratorFeignClient;

    @Override
    @Transactional
    public Comment addComment(Long userId, Long dramaId, Long episodeId, Long parentId, Long replyToUserId, String content) {
        Comment comment = new Comment();
        comment.setId(idGeneratorFeignClient.nextId(BizTypeEnum.COMMENT).getData());
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
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());

        commentMapper.insert(comment);

        // If this is a reply, increment parent's reply count
        if (parentId != null && parentId > 0) {
            incrementReplyCount(parentId);
        }

        log.info("添加评论成功: userId={}, dramaId={}, commentId={}", userId, dramaId, comment.getId());
        return comment;
    }

    @Override
    @Transactional
    public void updateComment(Long userId, Long commentId, String content) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getIsDeleted() == 1) {
            throw new BusinessException(BusinessException.ResultCode.COMMENT_NOT_FOUND);
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权修改他人评论");
        }

        comment.setContent(content);
        comment.setUpdateTime(LocalDateTime.now());
        commentMapper.updateById(comment);
        log.info("更新评论成功: commentId={}", commentId);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getIsDeleted() == 1) {
            throw new BusinessException(BusinessException.ResultCode.COMMENT_NOT_FOUND);
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权删除他人评论");
        }

        comment.setIsDeleted(1);
        comment.setUpdateTime(LocalDateTime.now());
        commentMapper.updateById(comment);
        log.info("删除评论成功: commentId={}", commentId);
    }

    @Override
    @Transactional
    public void topComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getIsDeleted() == 1) {
            throw new BusinessException(BusinessException.ResultCode.COMMENT_NOT_FOUND);
        }

        comment.setIsTop(comment.getIsTop() == 1 ? 0 : 1);
        comment.setUpdateTime(LocalDateTime.now());
        commentMapper.updateById(comment);
        log.info("置顶/取消置顶评论: commentId={}, isTop={}", commentId, comment.getIsTop());
    }

    @Override
    public List<Comment> getComments(Long dramaId, Long episodeId, Long parentId, Integer page, Integer size, String sortBy) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getIsDeleted, 0);

        if (dramaId != null) {
            wrapper.eq(Comment::getDramaId, dramaId);
        }
        if (episodeId != null) {
            wrapper.eq(Comment::getEpisodeId, episodeId);
        }
        if (parentId != null) {
            wrapper.eq(Comment::getParentId, parentId);
        } else {
            wrapper.eq(Comment::getParentId, 0L);
        }

        // Sorting
        if ("hot".equals(sortBy)) {
            wrapper.orderByDesc(Comment::getIsTop)
                    .orderByDesc(Comment::getLikeCount)
                    .orderByDesc(Comment::getCreateTime);
        } else {
            wrapper.orderByDesc(Comment::getIsTop)
                    .orderByDesc(Comment::getCreateTime);
        }

        Page<Comment> pageParam = new Page<>(page, size);
        Page<Comment> result = commentMapper.selectPage(pageParam, wrapper);
        return result.getRecords();
    }

    @Override
    public List<Comment> getCommentReplies(Long commentId, Integer page, Integer size) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getParentId, commentId)
                .eq(Comment::getIsDeleted, 0)
                .orderByAsc(Comment::getCreateTime);

        Page<Comment> pageParam = new Page<>(page, size);
        Page<Comment> result = commentMapper.selectPage(pageParam, wrapper);
        return result.getRecords();
    }

    @Override
    @Transactional
    public void incrementReplyCount(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment != null) {
            comment.setReplyCount((comment.getReplyCount() != null ? comment.getReplyCount() : 0) + 1);
            commentMapper.updateById(comment);
        }
    }
}
