package com.bunny.flix.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.interaction.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论数据访问层
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
