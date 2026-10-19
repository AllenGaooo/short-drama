package com.bunny.flix.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.interaction.entity.Danmaku;
import org.apache.ibatis.annotations.Mapper;

/**
 * 弹幕数据访问层
 */
@Mapper
public interface DanmakuMapper extends BaseMapper<Danmaku> {
}
