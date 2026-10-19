package com.bunny.flix.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.interaction.entity.Collect;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收藏数据访问层
 */
@Mapper
public interface CollectMapper extends BaseMapper<Collect> {
}
