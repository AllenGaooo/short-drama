package com.bunny.flix.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.interaction.entity.Like;
import org.apache.ibatis.annotations.Mapper;

/**
 * 点赞数据访问层
 */
@Mapper
public interface LikeMapper extends BaseMapper<Like> {
}
