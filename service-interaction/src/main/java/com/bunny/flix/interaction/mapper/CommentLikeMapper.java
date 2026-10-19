package com.bunny.flix.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.interaction.entity.CommentLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论点赞数据访问层
 */
@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {
}
