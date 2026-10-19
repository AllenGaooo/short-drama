package com.bunny.flix.drama.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.drama.entity.Actor;
import org.apache.ibatis.annotations.Mapper;

/**
 * 演员数据访问层
 */
@Mapper
public interface ActorMapper extends BaseMapper<Actor> {
}
